"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.acceptTask = exports.postTask = void 0;
exports.generateTaskOtp = generateTaskOtp;
exports.getAppPriceBounds = getAppPriceBounds;
exports.executePostTask = executePostTask;
exports.executeAcceptTask = executeAcceptTask;
const functions = __importStar(require("firebase-functions"));
const admin = __importStar(require("firebase-admin"));
const firestore_1 = require("firebase-admin/firestore");
const crypto_1 = require("crypto");
const geohash_1 = require("./geohash");
const db = admin.firestore();
function generateTaskOtp() {
    return String((0, crypto_1.randomInt)(1000, 10000));
}
/**
 * Loads price bounds from config/app or returns defaults.
 * SPEC.md Section A6 & A7.
 */
async function getAppPriceBounds() {
    try {
        const appConfigDoc = await db.collection("config").doc("app").get();
        if (appConfigDoc.exists) {
            const data = appConfigDoc.data();
            const minPrice = typeof data?.minPrice === "number" ? data.minPrice : 10000;
            const maxPrice = typeof data?.maxPrice === "number" ? data.maxPrice : 1000000;
            return { minPrice, maxPrice };
        }
    }
    catch (err) {
        functions.logger.warn("Could not read config/app, using defaults", err);
    }
    return { minPrice: 10000, maxPrice: 1000000 };
}
/**
 * Executes task posting inside an atomic Firestore transaction.
 *
 * SPEC.md Section A7:
 * postTask(data) : validates price bounds from config/app , checks
 * wallet.available >= price , and in one transaction moves the price to held ,
 * creates the task (status open, geohash), creates taskSecrets with a random 4-digit
 * OTP, and writes a hold ledger entry.
 */
async function executePostTask(uid, params, bounds) {
    const { title, description, category, price, lat, lng, address, scheduledFor } = params;
    if (price < bounds.minPrice || price > bounds.maxPrice) {
        throw new functions.https.HttpsError("invalid-argument", `Task price must be between ₹${bounds.minPrice / 100} and ₹${bounds.maxPrice / 100}.`);
    }
    const walletRef = db.collection("wallets").doc(uid);
    const taskRef = db.collection("tasks").doc();
    const secretRef = db.collection("taskSecrets").doc(taskRef.id);
    const transactionRef = db.collection("transactions").doc();
    return await db.runTransaction(async (transaction) => {
        const walletSnap = await transaction.get(walletRef);
        let currentAvailable = 0;
        let currentHeld = 0;
        if (walletSnap.exists) {
            const wData = walletSnap.data();
            currentAvailable = typeof wData.available === "number" ? wData.available : 0;
            currentHeld = typeof wData.held === "number" ? wData.held : 0;
        }
        if (currentAvailable < price) {
            throw new functions.https.HttpsError("failed-precondition", `Insufficient wallet balance. You have ₹${currentAvailable / 100}, but this task requires ₹${price / 100}. Please add money to your wallet.`);
        }
        const newAvailable = currentAvailable - price;
        const newHeld = currentHeld + price;
        const geohash = (0, geohash_1.encodeGeohash)(lat, lng, 9);
        const otp = generateTaskOtp();
        // 1. Move task price from available to held in user wallet
        transaction.set(walletRef, {
            available: newAvailable,
            held: newHeld,
            updatedAt: firestore_1.FieldValue.serverTimestamp(),
        }, { merge: true });
        // 2. Create task with status "open"
        transaction.set(taskRef, {
            id: taskRef.id,
            title: title,
            description: description,
            category: category,
            price: price,
            lat: lat,
            lng: lng,
            geohash: geohash,
            address: address,
            posterId: uid,
            workerId: null,
            status: "open",
            scheduledFor: scheduledFor || "Now",
            proofPath: null,
            rated: false,
            createdAt: firestore_1.FieldValue.serverTimestamp(),
            updatedAt: firestore_1.FieldValue.serverTimestamp(),
        });
        // 3. Create taskSecrets with arrival OTP readable only by poster
        const qrToken = (0, crypto_1.randomBytes)(16).toString("hex");
        transaction.set(secretRef, {
            taskId: taskRef.id,
            posterId: uid,
            otp: otp,
            qrToken: qrToken,
            proofQrToken: (0, crypto_1.randomBytes)(32).toString("hex"),
            proofQrFailedAttempts: 0,
            failedAttempts: 0,
            createdAt: firestore_1.FieldValue.serverTimestamp(),
        });
        // 4. Record hold in immutable transactions ledger
        transaction.set(transactionRef, {
            uid: uid,
            type: "hold",
            amount: price,
            taskId: taskRef.id,
            razorpayPaymentId: null,
            createdAt: firestore_1.FieldValue.serverTimestamp(),
        });
        functions.logger.info(`Task ${taskRef.id} created by user ${uid}. Held ₹${price / 100} in escrow. Geohash: ${geohash}`);
        return {
            success: true,
            taskId: taskRef.id,
            price: price,
            available: newAvailable,
            held: newHeld,
        };
    });
}
/**
 * Cloud Function: postTask(data)
 */
exports.postTask = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "User must be authenticated to post a task.");
    }
    const uid = context.auth.uid;
    const title = String(data?.title || "").trim();
    const description = String(data?.description || "").trim();
    const category = String(data?.category || "").trim();
    const price = Number(data?.price);
    const lat = Number(data?.lat);
    const lng = Number(data?.lng);
    const address = String(data?.address || "").trim();
    const scheduledFor = String(data?.scheduledFor || "Now").trim();
    if (!title || title.length < 3) {
        throw new functions.https.HttpsError("invalid-argument", "Task title must be at least 3 characters.");
    }
    if (!description || description.length < 5) {
        throw new functions.https.HttpsError("invalid-argument", "Task description must be at least 5 characters.");
    }
    if (!category) {
        throw new functions.https.HttpsError("invalid-argument", "Task category is required.");
    }
    if (!price || !Number.isInteger(price) || price <= 0) {
        throw new functions.https.HttpsError("invalid-argument", "Task price must be a positive integer in paise.");
    }
    if (isNaN(lat) || lat < -90 || lat > 90 || isNaN(lng) || lng < -180 || lng > 180) {
        throw new functions.https.HttpsError("invalid-argument", "Valid latitude and longitude coordinates are required.");
    }
    if (!address) {
        throw new functions.https.HttpsError("invalid-argument", "Task location address is required.");
    }
    const bounds = await getAppPriceBounds();
    const result = await executePostTask(uid, { title, description, category, price, lat, lng, address, scheduledFor }, bounds);
    return result;
});
/**
 * Executes task acceptance inside an atomic Firestore transaction.
 *
 * SPEC.md Section A7:
 * acceptTask(taskId) : transaction, only if status is open and the caller is not the poster.
 */
async function executeAcceptTask(workerUid, taskId) {
    const userRef = db.collection("users").doc(workerUid);
    const taskRef = db.collection("tasks").doc(taskId);
    return await db.runTransaction(async (transaction) => {
        // 1. Verify worker profile and availability
        const userSnap = await transaction.get(userRef);
        if (!userSnap.exists) {
            throw new functions.https.HttpsError("not-found", "Worker user profile not found.");
        }
        const userData = userSnap.data();
        if (userData.available !== true) {
            throw new functions.https.HttpsError("failed-precondition", "Worker is currently set to unavailable. Please turn on 'I am free now' to accept tasks.");
        }
        // 2. Verify task status and eligibility
        const taskSnap = await transaction.get(taskRef);
        if (!taskSnap.exists) {
            throw new functions.https.HttpsError("not-found", "Task not found.");
        }
        const taskData = taskSnap.data();
        if (taskData.posterId === workerUid) {
            throw new functions.https.HttpsError("permission-denied", "You cannot accept your own task.");
        }
        if (taskData.status !== "open") {
            throw new functions.https.HttpsError("failed-precondition", "This task is no longer open for acceptance.");
        }
        // 3. Atomically assign task to worker (preserves existing escrow held balance)
        transaction.update(taskRef, {
            status: "accepted",
            workerId: workerUid,
            acceptedAt: firestore_1.FieldValue.serverTimestamp(),
            updatedAt: firestore_1.FieldValue.serverTimestamp(),
        });
        functions.logger.info(`Task ${taskId} accepted by worker ${workerUid}.`);
        return {
            success: true,
            taskId: taskId,
            status: "accepted",
        };
    });
}
/**
 * Cloud Function: acceptTask(taskId)
 */
exports.acceptTask = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "User must be authenticated to accept a task.");
    }
    const workerUid = context.auth.uid;
    const taskId = String(data?.taskId || "").trim();
    if (!taskId) {
        throw new functions.https.HttpsError("invalid-argument", "Task ID is required.");
    }
    return await executeAcceptTask(workerUid, taskId);
});
//# sourceMappingURL=tasks.js.map