/**
 * Seed app config and two Worker Mode demo tasks in the local Firestore Emulator.
 * Deterministic task IDs make reruns safe; no user or wallet documents are written.
 */
const PROJECT_ID = "rozkaam-70984";
const DEFAULT_EMULATOR_HOST = "127.0.0.1:8080";
const host = process.env.FIRESTORE_EMULATOR_HOST || DEFAULT_EMULATOR_HOST;

if (host !== "127.0.0.1:8080" && host !== "localhost:8080") {
  console.error("FATAL: Seed script only supports the local Firestore Emulator at port 8080:", host);
  process.exit(1);
}

if (process.env.GCLOUD_PROJECT && process.env.GCLOUD_PROJECT !== PROJECT_ID) {
  console.error(`FATAL: Seed script only supports project ${PROJECT_ID}:`, process.env.GCLOUD_PROJECT);
  process.exit(1);
}

process.env.FIRESTORE_EMULATOR_HOST = host;
process.env.GCLOUD_PROJECT = PROJECT_ID;

const admin = require("firebase-admin");
const { encodeGeohash } = require("../lib/geohash");

if (!admin.apps.length) {
  admin.initializeApp({
    projectId: process.env.GCLOUD_PROJECT
  });
}

const db = admin.firestore();

async function seedDemoTasks() {
  console.log(`Connecting to Firestore Emulator at ${process.env.FIRESTORE_EMULATOR_HOST} (project: ${process.env.GCLOUD_PROJECT})...`);

  const DEMO_POSTER_UID = "demo_poster_001";

  // 1. Ensure config/categories exists
  const categoriesRef = db.collection("config").doc("categories");
  await categoriesRef.set({
    items: [
      { id: "cleaning", label: "Cleaning", icon: "cleaning" },
      { id: "delivery", label: "Delivery", icon: "delivery" },
      { id: "moving", label: "Moving", icon: "moving" },
      { id: "repair", label: "Repairs", icon: "repair" }
    ]
  }, { merge: true });
  console.log("✔ config/categories verified.");

  // 2. Ensure config/app exists
  const appConfigRef = db.collection("config").doc("app");
  await appConfigRef.set({
    minPrice: 10000,
    maxPrice: 1000000,
    quickAmounts: [50000, 100000, 200000, 500000],
    defaultRadiusKm: 10
  }, { merge: true });
  console.log("✔ config/app verified.");

  // 3. Define two deterministic Bengaluru demo tasks
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
      scheduledFor: "Today"
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
      scheduledFor: "Now"
    }
  ];

  for (const t of demoTasks) {
    const geohash = encodeGeohash(t.lat, t.lng, 9);

    // Write task
    const taskDocRef = db.collection("tasks").doc(t.id);
    await taskDocRef.set({
      id: t.id,
      title: t.title,
      description: t.description,
      category: t.category,
      price: t.price,
      lat: t.lat,
      lng: t.lng,
      geohash: geohash,
      address: t.address,
      posterId: DEMO_POSTER_UID,
      workerId: null,
      status: "open",
      scheduledFor: t.scheduledFor,
      proofPath: null,
      rated: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    console.log(`✔ Task seeded: [${t.category.toUpperCase()}] "${t.title}" (₹${t.price / 100}) at ${t.lat}, ${t.lng} (geohash: ${geohash})`);
  }

  console.log("\nApp config and demo tasks successfully seeded in local Firestore Emulator.");
}

seedDemoTasks()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error("Error seeding demo tasks:", err);
    process.exit(1);
  });
