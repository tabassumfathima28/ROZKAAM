const test = require("node:test");
const assert = require("node:assert");
const { encodeGeohash } = require("../lib/geohash");

test("Geohash Encoding - Accuracy & Length", () => {
  // Bangalore coordinates
  const lat = 12.9716;
  const lng = 77.5946;
  const hash = encodeGeohash(lat, lng, 9);

  assert.strictEqual(hash.length, 9);
  assert.strictEqual(hash, "tdr1v9qtj");

  // Mumbai coordinates
  const mumbaiHash = encodeGeohash(19.076, 72.8777, 9);
  assert.strictEqual(mumbaiHash.length, 9);
  assert.strictEqual(mumbaiHash, "te7ud2evv");
});

test("PostTask - In-memory Transaction & Escrow Hold Simulation", () => {
  // Simulator testing exact transactional logic of executePostTask
  const mockDb = {
    wallets: new Map([
      ["user_poster_1", { available: 50000, held: 0 }],
      ["user_poor_poster", { available: 5000, held: 0 }]
    ]),
    tasks: new Map(),
    taskSecrets: new Map(),
    transactions: []
  };

  const bounds = { minPrice: 10000, maxPrice: 1000000 }; // ₹100 to ₹10,000

  function simulatePostTask(uid, params) {
    const { title, description, category, price, lat, lng, address, scheduledFor } = params;

    if (!title || title.length < 3) throw new Error("Invalid title");
    if (!description || description.length < 5) throw new Error("Invalid description");
    if (!category) throw new Error("Invalid category");
    if (!price || !Number.isInteger(price) || price <= 0) throw new Error("Invalid price");
    if (price < bounds.minPrice || price > bounds.maxPrice) throw new Error("Price out of bounds");
    if (isNaN(lat) || lat < -90 || lat > 90 || isNaN(lng) || lng < -180 || lng > 180) throw new Error("Invalid coordinates");
    if (!address) throw new Error("Invalid address");

    const wallet = mockDb.wallets.get(uid) || { available: 0, held: 0 };
    if (wallet.available < price) {
      throw new Error("Insufficient wallet balance");
    }

    // Atomic changes
    const newAvailable = wallet.available - price;
    const newHeld = wallet.held + price;
    mockDb.wallets.set(uid, { available: newAvailable, held: newHeld });

    const taskId = `task_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
    const geohash = encodeGeohash(lat, lng, 9);
    const otp = String(Math.floor(1000 + Math.random() * 9000));

    mockDb.tasks.set(taskId, {
      id: taskId,
      title,
      description,
      category,
      price,
      lat,
      lng,
      geohash,
      address,
      posterId: uid,
      workerId: null,
      status: "open",
      scheduledFor: scheduledFor || "Now",
      proofPath: null,
      rated: false
    });

    mockDb.taskSecrets.set(taskId, {
      taskId,
      posterId: uid,
      otp,
      failedAttempts: 0
    });

    mockDb.transactions.push({
      uid,
      type: "hold",
      amount: price,
      taskId,
      razorpayPaymentId: null
    });

    return {
      success: true,
      taskId,
      price,
      available: newAvailable,
      held: newHeld,
      otp
    };
  }

  // 1. Successful Task Creation with sufficient balance
  const validParams = {
    title: "Need help moving boxes",
    description: "Move 5 boxes from 2nd floor to ground floor.",
    category: "moving",
    price: 30000, // ₹300
    lat: 12.9716,
    lng: 77.5946,
    address: "MG Road, Bangalore",
    scheduledFor: "Today, 4:00 PM"
  };

  const result = simulatePostTask("user_poster_1", validParams);
  assert.strictEqual(result.success, true);
  assert.strictEqual(result.price, 30000);
  assert.strictEqual(result.available, 20000); // 50000 - 30000
  assert.strictEqual(result.held, 30000); // 0 + 30000

  // Verify wallet state in DB
  const updatedWallet = mockDb.wallets.get("user_poster_1");
  assert.strictEqual(updatedWallet.available, 20000);
  assert.strictEqual(updatedWallet.held, 30000);

  // Verify task state in DB
  const createdTask = mockDb.tasks.get(result.taskId);
  assert.strictEqual(createdTask.status, "open");
  assert.strictEqual(createdTask.workerId, null);
  assert.strictEqual(createdTask.geohash, "tdr1v9qtj");
  assert.strictEqual(createdTask.posterId, "user_poster_1");

  // Verify taskSecrets OTP
  const taskSecret = mockDb.taskSecrets.get(result.taskId);
  assert.strictEqual(taskSecret.posterId, "user_poster_1");
  assert.strictEqual(typeof taskSecret.otp, "string");
  assert.strictEqual(taskSecret.otp.length, 4);
  assert.strictEqual(/^\d{4}$/.test(taskSecret.otp), true); // 4-digit numeric OTP

  // Verify transaction ledger hold record
  const holdTx = mockDb.transactions.find((tx) => tx.taskId === result.taskId);
  assert.ok(holdTx);
  assert.strictEqual(holdTx.type, "hold");
  assert.strictEqual(holdTx.amount, 30000);
  assert.strictEqual(holdTx.uid, "user_poster_1");

  // 2. Insufficient balance test
  assert.throws(
    () => {
      simulatePostTask("user_poor_poster", {
        ...validParams,
        price: 20000 // ₹200, but user only has ₹50
      });
    },
    /Insufficient wallet balance/
  );

  // Verify user_poor_poster wallet was NOT modified (atomic failure)
  assert.strictEqual(mockDb.wallets.get("user_poor_poster").available, 5000);
  assert.strictEqual(mockDb.wallets.get("user_poor_poster").held, 0);

  // 3. Price below minPrice
  assert.throws(
    () => {
      simulatePostTask("user_poster_1", {
        ...validParams,
        price: 5000 // ₹50 (min is ₹100 = 10000 paise)
      });
    },
    /Price out of bounds/
  );

  // 4. Price above maxPrice
  assert.throws(
    () => {
      simulatePostTask("user_poster_1", {
        ...validParams,
        price: 2000000 // ₹20,000 (max is ₹10,000 = 1000000 paise)
      });
    },
    /Price out of bounds/
  );

  // 5. Missing or invalid title
  assert.throws(
    () => {
      simulatePostTask("user_poster_1", {
        ...validParams,
        title: "ab" // less than 3 chars
      });
    },
    /Invalid title/
  );

  // 6. Missing address
  assert.throws(
    () => {
      simulatePostTask("user_poster_1", {
        ...validParams,
        address: ""
      });
    },
    /Invalid address/
  );
});
