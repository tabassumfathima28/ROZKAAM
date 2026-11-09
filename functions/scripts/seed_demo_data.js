/**
 * Idempotent local Firebase Emulator Seed Script for ROZKAAM.
 * Seeds task categories, app configuration, dedicated demo Poster & Worker accounts,
 * user wallets, escrow records, and realistic open tasks in Bengaluru.
 *
 * SAFETY: Runs ONLY against local Firebase Emulator (project: rozkaam-70984).
 */
const PROJECT_ID = "rozkaam-70984";
const FIRESTORE_HOST = process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8080";
const AUTH_HOST = process.env.FIREBASE_AUTH_EMULATOR_HOST || "127.0.0.1:9099";

if (FIRESTORE_HOST !== "127.0.0.1:8080" && FIRESTORE_HOST !== "localhost:8080") {
  console.error("FATAL: Seed script only supports local Firestore Emulator at 127.0.0.1:8080:", FIRESTORE_HOST);
  process.exit(1);
}

process.env.FIRESTORE_EMULATOR_HOST = FIRESTORE_HOST;
process.env.FIREBASE_AUTH_EMULATOR_HOST = AUTH_HOST;
process.env.GCLOUD_PROJECT = PROJECT_ID;

const admin = require("firebase-admin");
const { encodeGeohash } = require("../lib/geohash");

if (!admin.apps.length) {
  admin.initializeApp({ projectId: PROJECT_ID });
}

const db = admin.firestore();
const auth = admin.auth();

async function ensureAuthUser(uid, phoneNumber, displayName) {
  try {
    const user = await auth.getUser(uid);
    console.log(`✔ Auth user already exists: ${uid} (${user.phoneNumber})`);
    return user;
  } catch (err) {
    if (err.code === "auth/user-not-found") {
      // Check if phone number is taken by another UID in emulator
      try {
        const byPhone = await auth.getUserByPhoneNumber(phoneNumber);
        console.log(`✔ Auth user exists with phone ${phoneNumber} (uid: ${byPhone.uid})`);
        return byPhone;
      } catch (phoneErr) {
        // Create new user
        const newUser = await auth.createUser({
          uid,
          phoneNumber,
          displayName
        });
        console.log(`✔ Created Auth user: ${uid} (${phoneNumber}, ${displayName})`);
        return newUser;
      }
    }
    throw err;
  }
}

async function seed() {
  console.log("=== ROZKAAM EMULATOR SEED START ===");
  console.log(`Target: Firestore @ ${FIRESTORE_HOST}, Auth @ ${AUTH_HOST}, Project: ${PROJECT_ID}`);

  // 1. Seed config/categories (SPEC.md A2, A6)
  const categoriesRef = db.collection("config").doc("categories");
  await categoriesRef.set({
    items: [
      { id: "cleaning", label: "Cleaning", icon: "cleaning" },
      { id: "delivery", label: "Delivery", icon: "delivery" },
      { id: "moving", label: "Moving", icon: "moving" },
      { id: "repair", label: "Repairs", icon: "repair" },
      { id: "gardening", label: "Gardening", icon: "gardening" },
      { id: "car_wash", label: "Car Wash", icon: "car_wash" },
      { id: "pet_care", label: "Pet Care", icon: "pet" }
    ],
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  }, { merge: true });
  console.log("✔ config/categories seeded with 7 standard categories.");

  // 2. Seed config/app (SPEC.md A6, A7)
  const appConfigRef = db.collection("config").doc("app");
  await appConfigRef.set({
    minPrice: 10000,          // ₹100
    maxPrice: 1000000,        // ₹10,000
    quickAmounts: [50000, 100000, 200000, 500000], // ₹500, ₹1000, ₹2000, ₹5000
    defaultRadiusKm: 10,
    arrivalRadiusMeters: 500,
    autoReleaseHours: 24,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  }, { merge: true });
  console.log("✔ config/app pricing & radius configuration verified.");

  // 3. Preserve and initialize wallet for existing user Tabassum Fathima if needed
  const existingUserId = "ELb9N5wa6raKTdtoK7lLBkO3RtQa";
  const existingWalletRef = db.collection("wallets").doc(existingUserId);
  const existingWalletSnap = await existingWalletRef.get();
  if (!existingWalletSnap.exists) {
    await existingWalletRef.set({
      available: 1000000, // ₹10,000 initial balance for test task posting
      held: 0,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    await db.collection("transactions").doc(`topup_${existingUserId}_init`).set({
      uid: existingUserId,
      type: "topup",
      amount: 1000000,
      taskId: null,
      razorpayPaymentId: "pay_test_tabassum_init",
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });
    console.log(`✔ Initialized wallet for existing user ${existingUserId} (₹10,000 available).`);
  } else {
    console.log(`✔ Preserved existing user ${existingUserId} wallet (${JSON.stringify(existingWalletSnap.data())}).`);
  }

  // 4. Demo Poster Account (Rahul Sharma)
  const POSTER_UID = "poster_demo_uid";
  const POSTER_PHONE = "+919876543210";
  const posterAuth = await ensureAuthUser(POSTER_UID, POSTER_PHONE, "Rahul Sharma");
  const actualPosterUid = posterAuth.uid;

  await db.collection("users").doc(actualPosterUid).set({
    name: "Rahul Sharma",
    phone: POSTER_PHONE,
    photoUrl: "",
    activeMode: "poster",
    emergencyContact: "9876543200",
    available: false,
    radiusKm: 10,
    dailyGoal: 50000,
    rating: 4.8,
    ratingCount: 12,
    completedCount: 15,
    fcmToken: "",
    createdAt: Date.now() - 30 * 86400000
  }, { merge: true });

  await db.collection("publicProfiles").doc(actualPosterUid).set({
    name: "Rahul Sharma",
    photoUrl: "",
    rating: 4.8,
    completedCount: 15,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  }, { merge: true });

  // Poster wallet: ₹5,000 available + ₹850 held in escrow for the 2 open tasks
  await db.collection("wallets").doc(actualPosterUid).set({
    available: 500000,
    held: 85000,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  }, { merge: true });

  await db.collection("transactions").doc(`topup_${actualPosterUid}_init`).set({
    uid: actualPosterUid,
    type: "topup",
    amount: 585000,
    taskId: null,
    razorpayPaymentId: "pay_test_poster_init",
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  }, { merge: true });
  console.log(`✔ Demo Poster account ready: ${actualPosterUid} (${POSTER_PHONE})`);

  // 5. Demo Worker Account (Suresh Kumar)
  const WORKER_UID = "worker_demo_uid";
  const WORKER_PHONE = "+919876543211";
  const workerAuth = await ensureAuthUser(WORKER_UID, WORKER_PHONE, "Suresh Kumar");
  const actualWorkerUid = workerAuth.uid;

  await db.collection("users").doc(actualWorkerUid).set({
    name: "Suresh Kumar",
    phone: WORKER_PHONE,
    photoUrl: "",
    activeMode: "worker",
    emergencyContact: "9876543201",
    available: true,
    radiusKm: 15,
    dailyGoal: 100000, // ₹1,000
    rating: 4.9,
    ratingCount: 24,
    completedCount: 28,
    fcmToken: "",
    createdAt: Date.now() - 60 * 86400000
  }, { merge: true });

  await db.collection("publicProfiles").doc(actualWorkerUid).set({
    name: "Suresh Kumar",
    photoUrl: "",
    rating: 4.9,
    completedCount: 28,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  }, { merge: true });

  await db.collection("wallets").doc(actualWorkerUid).set({
    available: 250000, // ₹2,500
    held: 0,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  }, { merge: true });

  await db.collection("transactions").doc(`topup_${actualWorkerUid}_init`).set({
    uid: actualWorkerUid,
    type: "topup",
    amount: 250000,
    taskId: null,
    razorpayPaymentId: "pay_test_worker_init",
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  }, { merge: true });
  console.log(`✔ Demo Worker account ready: ${actualWorkerUid} (${WORKER_PHONE})`);

  // 6. Realistic Open Demo Tasks in Bengaluru
  const demoTasks = [
    {
      id: "demo_task_cleaning_001",
      title: "Apartment Deep Cleaning",
      description: "Deep clean a 2BHK apartment including kitchen tiles, balcony, and bathrooms. Cleaning supplies provided.",
      category: "cleaning",
      price: 35000, // ₹350
      lat: 12.9750,
      lng: 77.6000,
      address: "Residency Road, Ashok Nagar, Bengaluru, Karnataka 560025",
      scheduledFor: "Today",
      otp: "4821",
      qrToken: "clean4821tok"
    },
    {
      id: "demo_task_delivery_002",
      title: "Document Pickup & Express Delivery",
      description: "Pick up signed legal papers from 12th Main Indiranagar and deliver to 80 Feet Road Koramangala.",
      category: "delivery",
      price: 50000, // ₹500
      lat: 12.9780,
      lng: 77.6200,
      address: "100 Feet Road, HAL 2nd Stage, Indiranagar, Bengaluru, Karnataka 560038",
      scheduledFor: "Now",
      otp: "7539",
      qrToken: "deliv7539tok"
    }
  ];

  for (const t of demoTasks) {
    const geohash = encodeGeohash(t.lat, t.lng, 9);
    const taskRef = db.collection("tasks").doc(t.id);
    const existingTask = await taskRef.get();

    // Preserve task if already progressed beyond 'open'
    if (existingTask.exists && existingTask.data().status !== "open") {
      console.log(`✔ Preserving in-progress task ${t.id} (status: ${existingTask.data().status})`);
      continue;
    }

    await taskRef.set({
      id: t.id,
      title: t.title,
      description: t.description,
      category: t.category,
      price: t.price,
      lat: t.lat,
      lng: t.lng,
      geohash: geohash,
      address: t.address,
      posterId: actualPosterUid,
      workerId: null,
      status: "open",
      scheduledFor: t.scheduledFor,
      proofPath: null,
      proofDescription: null,
      rated: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    // Task secret with arrival OTP and QR verification token
    await db.collection("taskSecrets").doc(t.id).set({
      taskId: t.id,
      posterId: actualPosterUid,
      otp: t.otp,
      qrToken: t.qrToken,
      failedAttempts: 0,
      locked: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    // Escrow hold transaction ledger entry
    await db.collection("transactions").doc(`hold_${t.id}`).set({
      uid: actualPosterUid,
      type: "hold",
      amount: t.price,
      taskId: t.id,
      razorpayPaymentId: null,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    console.log(`✔ Seeded task [${t.category}]: "${t.title}" (₹${t.price / 100}) - OTP: ${t.otp}, QR Token: ${t.qrToken}`);
  }

  console.log("=== ROZKAAM EMULATOR SEED COMPLETE ===");
}

seed()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error("FATAL SEED ERROR:", err);
    process.exit(1);
  });
