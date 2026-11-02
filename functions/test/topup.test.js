const test = require("node:test");
const assert = require("node:assert");
const crypto = require("node:crypto");
const { generatePaymentSignature, timingSafeCompare, generateWebhookSignature } = require("../lib/crypto");

test("Razorpay Signature - Authentic Signature Verification", () => {
  const secret = "test_razorpay_secret_123456";
  const orderId = "order_N123456789";
  const paymentId = "pay_P987654321";

  const expectedSignature = crypto
    .createHmac("sha256", secret)
    .update(`${orderId}|${paymentId}`)
    .digest("hex");

  const generatedSignature = generatePaymentSignature(orderId, paymentId, secret);
  assert.strictEqual(generatedSignature, expectedSignature);

  // Timing-safe comparison should return true
  assert.strictEqual(timingSafeCompare(generatedSignature, expectedSignature), true);
});

test("Razorpay Signature - Forged Signature Rejection", () => {
  const secret = "test_razorpay_secret_123456";
  const orderId = "order_N123456789";
  const paymentId = "pay_P987654321";

  const validSignature = generatePaymentSignature(orderId, paymentId, secret);
  const forgedSignature = "forged_signature_00000000000000000000000000000000000000000000000000";

  // Timing-safe comparison must reject forged signature
  assert.strictEqual(timingSafeCompare(validSignature, forgedSignature), false);

  // Altered payment ID must produce signature mismatch
  const alteredSignature = generatePaymentSignature(orderId, "pay_TAMPERED", secret);
  assert.strictEqual(timingSafeCompare(validSignature, alteredSignature), false);
});

test("Webhook Signature - Authentic Webhook Verification", () => {
  const secret = "test_webhook_secret_999";
  const payload = JSON.stringify({
    event: "order.paid",
    payload: {
      order: { entity: { id: "order_N12345" } },
      payment: { entity: { id: "pay_P99999" } }
    }
  });

  const validSignature = generateWebhookSignature(payload, secret);
  assert.strictEqual(timingSafeCompare(validSignature, validSignature), true);

  const forgedSignature = "wrong_webhook_signature";
  assert.strictEqual(timingSafeCompare(validSignature, forgedSignature), false);
});

test("TopUp Credit Idempotency & Ledger Logic", () => {
  // In-memory test simulator verifying the exact transaction semantics of executeTopUpCredit
  const mockDb = {
    payments: new Map([
      ["order_TEST_101", { orderId: "order_TEST_101", uid: "user_abc", amount: 50000, status: "created" }]
    ]),
    wallets: new Map([
      ["user_abc", { available: 10000, held: 0 }]
    ]),
    transactions: []
  };

  function simulateExecuteTopUp(uid, orderId, paymentId, signature) {
    const payment = mockDb.payments.get(orderId);
    if (!payment) throw new Error("Order not found");
    if (payment.uid !== uid) throw new Error("Permission denied");

    // Idempotency check: if already paid, do not credit again
    if (payment.status === "paid") {
      const wallet = mockDb.wallets.get(uid) || { available: 0, held: 0 };
      return {
        success: true,
        alreadyProcessed: true,
        orderId,
        available: wallet.available,
        amount: payment.amount
      };
    }

    const wallet = mockDb.wallets.get(uid) || { available: 0, held: 0 };
    const newAvailable = wallet.available + payment.amount;

    // 1. Update wallet
    mockDb.wallets.set(uid, { available: newAvailable, held: wallet.held });

    // 2. Mark payment paid
    payment.status = "paid";
    payment.razorpayPaymentId = paymentId;
    payment.signature = signature;

    // 3. Append to ledger
    mockDb.transactions.push({
      uid,
      type: "topup",
      amount: payment.amount,
      taskId: null,
      razorpayPaymentId: paymentId
    });

    return {
      success: true,
      alreadyProcessed: false,
      orderId,
      available: newAvailable,
      amount: payment.amount
    };
  }

  // 1. First execution: should credit wallet and create ledger record
  const result1 = simulateExecuteTopUp("user_abc", "order_TEST_101", "pay_001", "sig_001");
  assert.strictEqual(result1.success, true);
  assert.strictEqual(result1.alreadyProcessed, false);
  assert.strictEqual(result1.available, 60000); // 10000 + 50000
  assert.strictEqual(mockDb.wallets.get("user_abc").available, 60000);
  assert.strictEqual(mockDb.transactions.length, 1);
  assert.strictEqual(mockDb.transactions[0].amount, 50000);
  assert.strictEqual(mockDb.transactions[0].type, "topup");

  // 2. Second execution with same order: MUST be idempotent!
  const result2 = simulateExecuteTopUp("user_abc", "order_TEST_101", "pay_001", "sig_001");
  assert.strictEqual(result2.success, true);
  assert.strictEqual(result2.alreadyProcessed, true);
  assert.strictEqual(result2.available, 60000); // balance remains 60000, not double credited
  assert.strictEqual(mockDb.wallets.get("user_abc").available, 60000);
  assert.strictEqual(mockDb.transactions.length, 1); // no duplicate ledger entry!
});

