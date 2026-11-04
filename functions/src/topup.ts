import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import Razorpay from "razorpay";
import {
  getRazorpayKeyId,
  getRazorpayKeySecret,
  getRazorpayWebhookSecret,
} from "./config";
import {
  generatePaymentSignature,
  generateWebhookSignature,
  timingSafeCompare,
} from "./crypto";

const db = admin.firestore();

/**
 * Result returned from executeTopUpCredit transaction
 */
export interface TopUpExecutionResult {
  success: boolean;
  alreadyProcessed: boolean;
  orderId: string;
  available: number;
  amount: number;
}

/**
 * Idempotent transaction: Credits the user's wallet, updates payments/{orderId},
 * and appends a record to the transactions ledger.
 *
 * SPEC.md Section A6 & A7:
 * - wallets/{uid} : available, held. Owner can read. Only Functions can write.
 * - transactions/{id} : uid, type (topup, hold, release, refund, withdrawal),
 *   amount, taskId, razorpayPaymentId, createdAt. Owner can read. Functions only.
 * - If order is already paid, does NOT credit wallet again (Strict Idempotency).
 */
export async function executeTopUpCredit(
  uid: string,
  orderId: string,
  paymentId: string,
  signature?: string
): Promise<TopUpExecutionResult> {
  const paymentRef = db.collection("payments").doc(orderId);
  const walletRef = db.collection("wallets").doc(uid);
  const transactionRef = db.collection("transactions").doc();

  return await db.runTransaction(async (transaction) => {
    const paymentSnap = await transaction.get(paymentRef);
    if (!paymentSnap.exists) {
      throw new functions.https.HttpsError(
        "not-found",
        `Payment order ${orderId} not found.`
      );
    }

    const paymentData = paymentSnap.data()!;
    if (paymentData.uid !== uid) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Order does not belong to the authenticated user."
      );
    }

    // IDEMPOTENCY CHECK:
    // If order has already been credited, do not credit again!
    if (paymentData.status === "paid") {
      functions.logger.info(
        `Order ${orderId} has already been credited. Skipping duplicate credit (idempotent).`
      );
      const walletSnap = await transaction.get(walletRef);
      const available = walletSnap.exists
        ? (walletSnap.data()?.available || 0)
        : 0;

      return {
        success: true,
        alreadyProcessed: true,
        orderId: orderId,
        available: available,
        amount: paymentData.amount || 0,
      };
    }

    // Read current wallet balance
    const walletSnap = await transaction.get(walletRef);
    let currentAvailable = 0;
    let currentHeld = 0;
    if (walletSnap.exists) {
      const wData = walletSnap.data()!;
      currentAvailable =
        typeof wData.available === "number" ? wData.available : 0;
      currentHeld = typeof wData.held === "number" ? wData.held : 0;
    }

    const amount =
      typeof paymentData.amount === "number" ? paymentData.amount : 0;
    const newAvailable = currentAvailable + amount;

    // 1. Update wallet balance
    transaction.set(
      walletRef,
      {
        available: newAvailable,
        held: currentHeld,
        updatedAt: FieldValue.serverTimestamp(),
      },
      { merge: true }
    );

    // 2. Mark payment document as paid
    transaction.update(paymentRef, {
      status: "paid",
      razorpayPaymentId: paymentId,
      signature: signature || paymentData.signature || "",
      paidAt: FieldValue.serverTimestamp(),
    });

    // 3. Append to immutable transactions ledger per Section A6
    transaction.set(transactionRef, {
      uid: uid,
      type: "topup",
      amount: amount,
      taskId: null,
      razorpayPaymentId: paymentId,
      createdAt: FieldValue.serverTimestamp(),
    });

    functions.logger.info(
      `Successfully credited ₹${amount / 100} to wallet ${uid}. New available: ₹${newAvailable / 100}`
    );

    return {
      success: true,
      alreadyProcessed: false,
      orderId: orderId,
      available: newAvailable,
      amount: amount,
    };
  });
}

/**
 * Cloud Function: createTopUpOrder(amount)
 *
 * SPEC.md Section A7:
 * Callable unless stated. Checks request.auth and validates inputs.
 * creates a Razorpay order through the Orders API and saves a payments doc.
 */
export const createTopUpOrder = functions
  .runWith({ secrets: ["RAZORPAY_KEY_SECRET", "RAZORPAY_KEY_ID"] })
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated to create a top-up order."
      );
    }

    const uid = context.auth.uid;
    const amount = Number(data?.amount);

    // Validate amount: must be positive integer in paise (e.g. ₹100 = 10000 paise)
    if (!amount || !Number.isInteger(amount) || amount <= 0) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Invalid top-up amount. Amount must be a positive integer in paise."
      );
    }

    const keyId = getRazorpayKeyId();
    const keySecret = getRazorpayKeySecret();

    if (!keyId || !keySecret) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Razorpay credentials are not configured on the backend. Please configure RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET."
      );
    }

    const razorpay = new Razorpay({
      key_id: keyId,
      key_secret: keySecret,
    });

    try {
      const receipt = `topup_${uid.substring(0, 8)}_${Date.now()}`.substring(0, 40);
      const options = {
        amount: amount,
        currency: "INR",
        receipt: receipt,
        notes: {
          uid: uid,
          type: "topup",
        },
      };

      const order = await razorpay.orders.create(options);

      // Save payments doc per Section A6: payments/{razorpayOrderId}
      await db.collection("payments").doc(order.id).set({
        orderId: order.id,
        uid: uid,
        amount: amount,
        currency: order.currency || "INR",
        status: "created",
        receipt: receipt,
        createdAt: FieldValue.serverTimestamp(),
      });

      functions.logger.info(`Created Razorpay order ${order.id} for user ${uid}, amount ₹${amount / 100}`);

      return {
        orderId: order.id,
        amount: order.amount,
        currency: order.currency || "INR",
        keyId: keyId,
      };
    } catch (err: any) {
      functions.logger.error("Razorpay order creation failed:", err);
      throw new functions.https.HttpsError(
        "internal",
        err.message || "Failed to create Razorpay payment order."
      );
    }
  });

/**
 * Cloud Function: verifyTopUp(orderId, paymentId, signature)
 *
 * SPEC.md Section A7:
 * recomputes HMAC-SHA256 of orderId|paymentId with the Key Secret,
 * compares in constant time, then credits the wallet exactly once (idempotent)
 * and writes a ledger entry.
 */
export const verifyTopUp = functions
  .runWith({ secrets: ["RAZORPAY_KEY_SECRET", "RAZORPAY_KEY_ID"] })
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated to verify payment."
      );
    }

    const uid = context.auth.uid;
    const orderId = String(data?.orderId || "").trim();
    const paymentId = String(data?.paymentId || "").trim();
    const signature = String(data?.signature || "").trim();

    if (!orderId || !paymentId || !signature) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "orderId, paymentId, and signature are all required."
      );
    }

    const keySecret = getRazorpayKeySecret();
    if (!keySecret) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Razorpay Key Secret is not configured on the backend."
      );
    }

    // 1. Recompute HMAC-SHA256 of orderId|paymentId
    const expectedSignature = generatePaymentSignature(orderId, paymentId, keySecret);

    // 2. Constant-time comparison protects against timing attacks
    const isValid = timingSafeCompare(expectedSignature, signature);
    if (!isValid) {
      functions.logger.warn(
        `Signature verification failed for user ${uid}, order ${orderId}, payment ${paymentId}. Forged signature rejected.`
      );
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Invalid payment signature. Forged signature rejected."
      );
    }

    // 3. Idempotently execute wallet credit & write transaction ledger
    const result = await executeTopUpCredit(uid, orderId, paymentId, signature);
    return result;
  });

/**
 * Cloud Function: order.paid Webhook (razorpayWebhook)
 *
 * SPEC.md Section A7:
 * Also add an HTTP webhook for order.paid that verifies the webhook signature,
 * as a backup.
 */
export const razorpayWebhook = functions
  .runWith({ secrets: ["RAZORPAY_WEBHOOK_SECRET", "RAZORPAY_KEY_SECRET"] })
  .https.onRequest(async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).send("Method Not Allowed");
      return;
    }

    const webhookSignature = req.headers["x-razorpay-signature"];
    if (!webhookSignature || typeof webhookSignature !== "string") {
      functions.logger.warn("Webhook rejected: Missing x-razorpay-signature header");
      res.status(400).send("Missing x-razorpay-signature header");
      return;
    }

    const webhookSecret = getRazorpayWebhookSecret();
    if (!webhookSecret) {
      functions.logger.error("Webhook secret not configured on server");
      res.status(500).send("Webhook secret not configured");
      return;
    }

    // Verify webhook signature
    // Firebase Functions provides req.rawBody as Buffer
    const rawBody = (req as any).rawBody || JSON.stringify(req.body);
    const expectedSignature = generateWebhookSignature(rawBody, webhookSecret);

    if (!timingSafeCompare(expectedSignature, webhookSignature)) {
      functions.logger.warn("Webhook signature mismatch. Forged webhook rejected.");
      res.status(400).send("Invalid webhook signature");
      return;
    }

    const event = req.body?.event;
    functions.logger.info(`Received verified Razorpay webhook event: ${event}`);

    if (event === "order.paid") {
      const orderEntity = req.body?.payload?.order?.entity;
      const paymentEntity = req.body?.payload?.payment?.entity;

      const orderId = orderEntity?.id;
      const paymentId = paymentEntity?.id || "";

      if (!orderId) {
        res.status(400).send("Missing order ID in payload");
        return;
      }

      try {
        const paymentDoc = await db.collection("payments").doc(orderId).get();
        if (!paymentDoc.exists) {
          functions.logger.warn(`Webhook: Order ${orderId} not found in payments collection`);
          res.status(404).send("Order not found");
          return;
        }

        const uid = paymentDoc.data()?.uid;
        if (!uid) {
          res.status(400).send("Invalid payment record");
          return;
        }

        const result = await executeTopUpCredit(uid, orderId, paymentId);
        res.status(200).json({ status: "ok", result });
        return;
      } catch (err: any) {
        functions.logger.error("Error processing webhook order.paid:", err);
        res.status(500).send("Internal server error");
        return;
      }
    }

    // Acknowledge other events gracefully
    res.status(200).json({ status: "ignored", event });
  });

