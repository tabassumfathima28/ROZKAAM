import * as admin from "firebase-admin";
import * as functions from "firebase-functions";
import { FieldValue } from "firebase-admin/firestore";
import { randomBytes } from "crypto";

const db = admin.firestore();
const proofBucket = () => admin.storage().bucket();

function requireTaskId(taskId: unknown): string {
  if (typeof taskId !== "string" || !taskId.trim() || taskId.includes("/") || taskId.length > 128) {
    throw new functions.https.HttpsError("invalid-argument", "A valid task ID is required.");
  }
  return taskId.trim();
}

function requireAuth(context: functions.https.CallableContext): string {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Sign in to continue.");
  }
  return context.auth.uid;
}

function distanceMeters(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const radians = (value: number) => value * Math.PI / 180;
  const dLat = radians(lat2 - lat1);
  const dLng = radians(lng2 - lng1);
  const a = Math.sin(dLat / 2) ** 2 +
    Math.cos(radians(lat1)) * Math.cos(radians(lat2)) * Math.sin(dLng / 2) ** 2;
  return 6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export async function executeStartTask(
  workerUid: string,
  taskId: string,
  otp: string,
  lat: number,
  lng: number,
  qrToken?: string
): Promise<{ success: boolean; status: string }> {
  const taskRef = db.collection("tasks").doc(taskId);
  const secretRef = db.collection("taskSecrets").doc(taskId);
  const configRef = db.collection("config").doc("app");
  const outcome = await db.runTransaction(async (transaction) => {
    const [taskSnap, secretSnap, configSnap] = await Promise.all([
      transaction.get(taskRef),
      transaction.get(secretRef),
      transaction.get(configRef),
    ]);
    if (!taskSnap.exists || !secretSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Task arrival details were not found.");
    }

    const task = taskSnap.data()!;
    if (task.workerId !== workerUid) {
      throw new functions.https.HttpsError("permission-denied", "Only the assigned worker can start this task.");
    }
    if (task.status !== "accepted") {
      throw new functions.https.HttpsError("failed-precondition", "This task is not awaiting arrival verification.");
    }

    const arrivalRadiusMeters = Number(configSnap.data()?.arrivalRadiusMeters ?? 500);
    if (!Number.isFinite(lat) || lat < -90 || lat > 90 ||
        !Number.isFinite(lng) || lng < -180 || lng > 180) {
      throw new functions.https.HttpsError("invalid-argument", "Valid current coordinates are required.");
    }
    if (distanceMeters(lat, lng, Number(task.lat), Number(task.lng)) > arrivalRadiusMeters) {
      throw new functions.https.HttpsError("failed-precondition", "You must be near the task location to verify arrival.");
    }

    const secret = secretSnap.data()!;
    const attempts = Number(secret.failedAttempts ?? 0);
    if (secret.locked === true || attempts >= 5) {
      throw new functions.https.HttpsError("resource-exhausted", "Verification is locked. Contact the poster.");
    }

    const isQrMatch = typeof qrToken === "string" && qrToken.trim().length > 0 &&
      typeof secret.qrToken === "string" && qrToken.trim() === secret.qrToken;
    const isOtpMatch = typeof otp === "string" && /^\d{4}$/.test(otp) && otp === secret.otp;

    if (!isQrMatch && !isOtpMatch) {
      const nextAttempts = attempts + 1;
      transaction.update(secretRef, {
        failedAttempts: nextAttempts,
        locked: nextAttempts >= 5,
        updatedAt: FieldValue.serverTimestamp(),
      });
      return { success: false, locked: nextAttempts >= 5 };
    }

    transaction.update(taskRef, {
      status: "started",
      startedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.update(secretRef, {
      qrToken: null,
      verifiedAt: FieldValue.serverTimestamp(),
    });
    return { success: true, locked: false };
  });

  if (!outcome.success) {
    throw new functions.https.HttpsError(
      outcome.locked ? "resource-exhausted" : "invalid-argument",
      outcome.locked ? "Too many incorrect verification attempts. Contact the poster." : "Incorrect arrival OTP or QR code."
    );
  }
  return { success: true, status: "started" };
}

export const startTask = functions.https.onCall(async (data, context) => {
  const uid = requireAuth(context);
  const taskId = requireTaskId(data?.taskId);
  const otp = typeof data?.otp === "string" ? data.otp : "";
  const qrToken = typeof data?.qrToken === "string" ? data.qrToken : undefined;
  const lat = Number(data?.lat);
  const lng = Number(data?.lng);
  return executeStartTask(uid, taskId, otp, lat, lng, qrToken);
});

export function createProofSubmissionUpdate(
  workerUid: string,
  taskId: string,
  task: FirebaseFirestore.DocumentData,
  proofPath: string,
  description: string
): { status: "submitted"; workerId: string; proofPath: string; proofDescription: string } {
  const prefix = `task-proofs/${taskId}/${workerUid}/`;
  const fileName = proofPath.startsWith(prefix) ? proofPath.slice(prefix.length) : "";
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.(jpg|png)$/i.test(fileName)) {
    throw new functions.https.HttpsError("invalid-argument", "Proof must be uploaded to the assigned task's proof folder.");
  }
  const proofDescription = description.trim();
  if (proofDescription.length < 5 || proofDescription.length > 500) {
    throw new functions.https.HttpsError("invalid-argument", "Description must be between 5 and 500 characters.");
  }
  if (task.workerId !== workerUid) {
    throw new functions.https.HttpsError("permission-denied", "Only the assigned worker can submit proof.");
  }
  if (task.status !== "started") {
    throw new functions.https.HttpsError("failed-precondition", "The task must be started before proof can be submitted.");
  }
  return { status: "submitted", workerId: workerUid, proofPath, proofDescription };
}

export function validateRating(stars: number, comment: string): string {
  if (!Number.isInteger(stars) || stars < 1 || stars > 5) {
    throw new functions.https.HttpsError("invalid-argument", "Rating must be between 1 and 5 stars.");
  }
  const normalizedComment = comment.trim();
  if (normalizedComment.length > 1000) {
    throw new functions.https.HttpsError("invalid-argument", "Review comment must be 1000 characters or fewer.");
  }
  return normalizedComment;
}

export function validateUpiId(upiId: string): string {
  const normalizedUpiId = upiId.trim();
  if (!/^[A-Za-z0-9._-]{2,256}@[A-Za-z0-9]{2,64}$/.test(normalizedUpiId)) {
    throw new functions.https.HttpsError("invalid-argument", "Enter a valid UPI ID.");
  }
  return normalizedUpiId;
}

export async function executeSubmitTaskProof(
  workerUid: string,
  taskId: string,
  proofPath: string,
  description: string
): Promise<{ success: boolean; taskId: string; status: string }> {
  const prefix = `task-proofs/${taskId}/${workerUid}/`;
  if (!proofPath.startsWith(prefix)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid proof storage path.");
  }
  const proofFile = proofBucket().file(proofPath);
  const [exists] = await proofFile.exists();
  if (!exists) throw new functions.https.HttpsError("failed-precondition", "Proof photo has not finished uploading.");
  const [metadata] = await proofFile.getMetadata();
  if (!["image/jpeg", "image/png"].includes(metadata.contentType ?? "") ||
      Number(metadata.size) > 5 * 1024 * 1024) {
    throw new functions.https.HttpsError("invalid-argument", "Proof must be a JPEG or PNG no larger than 5 MB.");
  }

  const taskRef = db.collection("tasks").doc(taskId);
  const secretRef = db.collection("taskSecrets").doc(taskId);
  return db.runTransaction(async (transaction) => {
    const [taskSnap, secretSnap] = await Promise.all([
      transaction.get(taskRef),
      transaction.get(secretRef),
    ]);
    if (!taskSnap.exists) throw new functions.https.HttpsError("not-found", "Task not found.");
    if (!secretSnap.exists) throw new functions.https.HttpsError("not-found", "Task proof verification details were not found.");
    const update = createProofSubmissionUpdate(workerUid, taskId, taskSnap.data()!, proofPath, description);
    const secret = secretSnap.data()!;
    transaction.update(taskRef, {
      ...update,
      proofSubmittedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    if (typeof secret.proofQrToken !== "string" || !/^[0-9a-f]{64}$/i.test(secret.proofQrToken)) {
      transaction.update(secretRef, {
        proofQrToken: randomBytes(32).toString("hex"),
        proofQrFailedAttempts: 0,
        proofQrLocked: false,
        updatedAt: FieldValue.serverTimestamp(),
      });
    }
    return { success: true, taskId, status: update.status };
  });
}

export const submitTaskProof = functions.https.onCall(async (data, context) => {
  const uid = requireAuth(context);
  const taskId = requireTaskId(data?.taskId);
  const proofPath = typeof data?.proofPath === "string" ? data.proofPath.trim() : "";
  const description = typeof data?.description === "string" ? data.description : "";
  return executeSubmitTaskProof(uid, taskId, proofPath, description);
});

export function validateTaskProofQr(
  workerUid: string,
  taskId: string,
  scannedTaskId: string,
  qrToken: string,
  task: FirebaseFirestore.DocumentData,
  secret: FirebaseFirestore.DocumentData
): void {
  if (scannedTaskId !== taskId) {
    throw new functions.https.HttpsError("invalid-argument", "This QR code belongs to a different task.");
  }
  if (task.workerId !== workerUid) {
    throw new functions.https.HttpsError("permission-denied", "Only the assigned worker can verify this proof.");
  }
  if (task.status !== "submitted") {
    throw new functions.https.HttpsError("failed-precondition", "This task has no submitted proof to verify.");
  }
  if (task.proofQrVerifiedAt) {
    throw new functions.https.HttpsError("already-exists", "This task proof QR code has already been used.");
  }
  if (secret.proofQrLocked === true || Number(secret.proofQrFailedAttempts ?? 0) >= 5) {
    throw new functions.https.HttpsError("resource-exhausted", "Proof QR verification is locked.");
  }
  if (!/^[0-9a-f]{64}$/i.test(qrToken) ||
      typeof secret.proofQrToken !== "string" ||
      qrToken !== secret.proofQrToken) {
    throw new functions.https.HttpsError("invalid-argument", "The task proof QR code is invalid.");
  }
}

export async function executeVerifyTaskProofQr(
  workerUid: string,
  taskId: string,
  scannedTaskId: string,
  qrToken: string
): Promise<{ success: boolean; taskId: string; status: string }> {
  const taskRef = db.collection("tasks").doc(taskId);
  const secretRef = db.collection("taskSecrets").doc(taskId);
  const outcome = await db.runTransaction(async (transaction) => {
    const [taskSnap, secretSnap] = await Promise.all([
      transaction.get(taskRef),
      transaction.get(secretRef),
    ]);
    if (!taskSnap.exists || !secretSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Task proof QR details were not found.");
    }

    const task = taskSnap.data()!;
    const secret = secretSnap.data()!;
    try {
      validateTaskProofQr(workerUid, taskId, scannedTaskId, qrToken, task, secret);
    } catch (error) {
      if (error instanceof functions.https.HttpsError &&
          error.code === "invalid-argument" &&
          error.message === "The task proof QR code is invalid.") {
        const attempts = Number(secret.proofQrFailedAttempts ?? 0) + 1;
        transaction.update(secretRef, {
          proofQrFailedAttempts: attempts,
          proofQrLocked: attempts >= 5,
          updatedAt: FieldValue.serverTimestamp(),
        });
        return { success: false, locked: attempts >= 5 };
      }
      throw error;
    }

    transaction.update(taskRef, {
      proofQrVerifiedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.update(secretRef, {
      proofQrToken: null,
      proofQrVerifiedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    return { success: true, locked: false };
  });

  if (!outcome.success) {
    throw new functions.https.HttpsError(
      outcome.locked ? "resource-exhausted" : "invalid-argument",
      outcome.locked ? "Too many incorrect proof QR attempts. Contact the poster." : "The task proof QR code is invalid."
    );
  }
  return { success: true, taskId, status: "submitted" };
}

export const verifyTaskProofQr = functions.https.onCall(async (data, context) => {
  const uid = requireAuth(context);
  const taskId = requireTaskId(data?.taskId);
  const scannedTaskId = requireTaskId(data?.scannedTaskId);
  const qrToken = typeof data?.qrToken === "string" ? data.qrToken.trim() : "";
  return executeVerifyTaskProofQr(uid, taskId, scannedTaskId, qrToken);
});

export async function executeApproveTask(posterUid: string, taskId: string) {
  const taskRef = db.collection("tasks").doc(taskId);
  const ledgerPosterRef = db.collection("transactions").doc(`release_${taskId}_poster`);
  const ledgerWorkerRef = db.collection("transactions").doc(`release_${taskId}_worker`);
  return db.runTransaction(async (transaction) => {
    const taskSnap = await transaction.get(taskRef);
    if (!taskSnap.exists) throw new functions.https.HttpsError("not-found", "Task not found.");
    const task = taskSnap.data()!;
    if (task.posterId !== posterUid) throw new functions.https.HttpsError("permission-denied", "Only the task poster can approve proof.");
    if (task.status !== "submitted") throw new functions.https.HttpsError("failed-precondition", "Only submitted tasks can be approved.");
    if (!task.proofQrVerifiedAt) {
      throw new functions.https.HttpsError("failed-precondition", "The assigned worker must verify the task proof QR code before payment approval.");
    }
    const workerUid = typeof task.workerId === "string" ? task.workerId : "";
    const price = Number(task.price);
    if (!workerUid || !Number.isSafeInteger(price) || price <= 0) {
      throw new functions.https.HttpsError("failed-precondition", "Task payment data is invalid.");
    }
    const posterWalletRef = db.collection("wallets").doc(posterUid);
    const workerWalletRef = db.collection("wallets").doc(workerUid);
    const workerUserRef = db.collection("users").doc(workerUid);
    const [posterWalletSnap, workerWalletSnap, posterLedgerSnap, workerLedgerSnap] =
      await Promise.all([
        transaction.get(posterWalletRef),
        transaction.get(workerWalletRef),
        transaction.get(ledgerPosterRef),
        transaction.get(ledgerWorkerRef),
      ]);
    if (posterLedgerSnap.exists || workerLedgerSnap.exists) {
      throw new functions.https.HttpsError("already-exists", "Payment for this task has already been released.");
    }
    const posterWallet = posterWalletSnap.data() ?? {};
    const workerWallet = workerWalletSnap.data() ?? {};
    const held = Number(posterWallet.held ?? 0);
    if (held < price) throw new functions.https.HttpsError("failed-precondition", "Escrow balance is insufficient.");

    transaction.set(posterWalletRef, {
      available: Number(posterWallet.available ?? 0),
      held: held - price,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    transaction.set(workerWalletRef, {
      available: Number(workerWallet.available ?? 0) + price,
      held: Number(workerWallet.held ?? 0),
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    transaction.set(workerUserRef, {
      completedCount: FieldValue.increment(1),
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    transaction.update(taskRef, {
      status: "completed",
      completedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    for (const [ref, uid, direction] of [
      [ledgerPosterRef, posterUid, "debit"],
      [ledgerWorkerRef, workerUid, "credit"],
    ] as const) {
      transaction.create(ref, {
        uid,
        type: "release",
        direction,
        amount: price,
        taskId,
        createdAt: FieldValue.serverTimestamp(),
      });
    }
    return { success: true, taskId, status: "completed", amount: price };
  });
}

export const approveTask = functions.https.onCall(async (data, context) => {
  return executeApproveTask(requireAuth(context), requireTaskId(data?.taskId));
});

export async function executeCancelTask(posterUid: string, taskId: string) {
  const taskRef = db.collection("tasks").doc(taskId);
  const ledgerRef = db.collection("transactions").doc(`refund_${taskId}`);
  return db.runTransaction(async (transaction) => {
    const taskSnap = await transaction.get(taskRef);
    if (!taskSnap.exists) throw new functions.https.HttpsError("not-found", "Task not found.");
    const task = taskSnap.data()!;
    if (task.posterId !== posterUid) throw new functions.https.HttpsError("permission-denied", "Only the poster can cancel this task.");
    if (task.status !== "open" && task.status !== "accepted") {
      throw new functions.https.HttpsError("failed-precondition", "This task can no longer be cancelled.");
    }
    const price = Number(task.price);
    const walletRef = db.collection("wallets").doc(posterUid);
    const [walletSnap, ledgerSnap] = await Promise.all([
      transaction.get(walletRef),
      transaction.get(ledgerRef),
    ]);
    if (ledgerSnap.exists) throw new functions.https.HttpsError("already-exists", "This task has already been refunded.");
    const wallet = walletSnap.data() ?? {};
    const held = Number(wallet.held ?? 0);
    if (!Number.isSafeInteger(price) || price <= 0 || held < price) {
      throw new functions.https.HttpsError("failed-precondition", "Escrow balance is insufficient for the refund.");
    }
    transaction.set(walletRef, {
      available: Number(wallet.available ?? 0) + price,
      held: held - price,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    transaction.update(taskRef, {
      status: "cancelled",
      cancelledAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.create(ledgerRef, {
      uid: posterUid,
      type: "refund",
      amount: price,
      taskId,
      createdAt: FieldValue.serverTimestamp(),
    });
    return { success: true, taskId, status: "cancelled", refunded: price };
  });
}

export const cancelTask = functions.https.onCall(async (data, context) => {
  return executeCancelTask(requireAuth(context), requireTaskId(data?.taskId));
});

export async function executeRateWorker(
  posterUid: string,
  taskId: string,
  stars: number,
  comment: string
) {
  const normalizedComment = validateRating(stars, comment);
  const taskRef = db.collection("tasks").doc(taskId);
  const reviewRef = db.collection("reviews").doc(taskId);
  return db.runTransaction(async (transaction) => {
    const taskSnap = await transaction.get(taskRef);
    if (!taskSnap.exists) throw new functions.https.HttpsError("not-found", "Task not found.");
    const task = taskSnap.data()!;
    if (task.posterId !== posterUid) throw new functions.https.HttpsError("permission-denied", "Only the poster can rate this task's worker.");
    if (task.status !== "completed") throw new functions.https.HttpsError("failed-precondition", "Only completed tasks can be rated.");
    if (task.rated === true) throw new functions.https.HttpsError("already-exists", "This task has already been rated.");
    const workerUid = typeof task.workerId === "string" ? task.workerId : "";
    if (!workerUid) throw new functions.https.HttpsError("failed-precondition", "This task has no assigned worker.");
    const workerRef = db.collection("users").doc(workerUid);
    const [workerSnap, reviewSnap] = await Promise.all([
      transaction.get(workerRef),
      transaction.get(reviewRef),
    ]);
    if (reviewSnap.exists) throw new functions.https.HttpsError("already-exists", "This task has already been rated.");
    const worker = workerSnap.data() ?? {};
    const oldCount = Number(worker.ratingCount ?? 0);
    const oldRating = Number(worker.rating ?? 0);
    const count = oldCount + 1;
    const rating = (oldRating * oldCount + stars) / count;
    transaction.create(reviewRef, {
      taskId,
      posterId: posterUid,
      workerId: workerUid,
      stars,
      comment: normalizedComment,
      createdAt: FieldValue.serverTimestamp(),
    });
    transaction.update(taskRef, { rated: true, ratedAt: FieldValue.serverTimestamp() });
    transaction.set(workerRef, {
      rating,
      ratingCount: count,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    return { success: true, taskId, rating, ratingCount: count };
  });
}

export const rateWorker = functions.https.onCall(async (data, context) => {
  const uid = requireAuth(context);
  const stars = Number(data?.stars);
  const comment = typeof data?.comment === "string" ? data.comment : "";
  return executeRateWorker(uid, requireTaskId(data?.taskId), stars, comment);
});

export async function executeRequestWithdrawal(uid: string, amount: number, upiId: string) {
  if (!Number.isSafeInteger(amount) || amount <= 0) {
    throw new functions.https.HttpsError("invalid-argument", "Withdrawal amount must be a positive whole number of paise.");
  }
  const normalizedUpiId = validateUpiId(upiId);
  const walletRef = db.collection("wallets").doc(uid);
  const withdrawalRef = db.collection("withdrawals").doc();
  const ledgerRef = db.collection("transactions").doc(`withdrawal_${withdrawalRef.id}`);
  return db.runTransaction(async (transaction) => {
    const walletSnap = await transaction.get(walletRef);
    const wallet = walletSnap.data() ?? {};
    const available = Number(wallet.available ?? 0);
    if (available < amount) throw new functions.https.HttpsError("failed-precondition", "Insufficient available balance.");
    transaction.set(walletRef, {
      available: available - amount,
      held: Number(wallet.held ?? 0),
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    transaction.create(withdrawalRef, {
      uid,
      amount,
      upiId: normalizedUpiId,
      status: "pending",
      createdAt: FieldValue.serverTimestamp(),
    });
    transaction.create(ledgerRef, {
      uid,
      type: "withdrawal",
      amount,
      withdrawalId: withdrawalRef.id,
      createdAt: FieldValue.serverTimestamp(),
    });
    return { success: true, withdrawalId: withdrawalRef.id, status: "pending" };
  });
}

export const requestWithdrawal = functions.https.onCall(async (data, context) => {
  const uid = requireAuth(context);
  const amount = Number(data?.amount);
  const upiId = typeof data?.upiId === "string" ? data.upiId : "";
  return executeRequestWithdrawal(uid, amount, upiId);
});

export const processWithdrawal = functions.https.onCall(async (data, context) => {
  const uid = requireAuth(context);
  const isAdmin = context.auth?.token.admin === true;
  const isEmulator = process.env.FUNCTIONS_EMULATOR === "true";
  if (!isAdmin && !isEmulator) {
    throw new functions.https.HttpsError("permission-denied", "Withdrawal processing is admin-only outside the emulator.");
  }
  const withdrawalId = requireTaskId(data?.withdrawalId);
  const ref = db.collection("withdrawals").doc(withdrawalId);
  return db.runTransaction(async (transaction) => {
    const snap = await transaction.get(ref);
    if (!snap.exists) throw new functions.https.HttpsError("not-found", "Withdrawal not found.");
    if (!isAdmin && snap.get("uid") !== uid) {
      throw new functions.https.HttpsError("permission-denied", "You can only process your own test withdrawal.");
    }
    if (snap.get("status") !== "pending") {
      throw new functions.https.HttpsError("failed-precondition", "Withdrawal is not pending.");
    }
    transaction.update(ref, {
      status: "processed",
      processedAt: FieldValue.serverTimestamp(),
    });
    return { success: true, withdrawalId, status: "processed" };
  });
});

export const getCounterpartContact = functions.https.onCall(async (data, context) => {
  const uid = requireAuth(context);
  const taskId = requireTaskId(data?.taskId);
  const taskSnap = await db.collection("tasks").doc(taskId).get();
  if (!taskSnap.exists) throw new functions.https.HttpsError("not-found", "Task not found.");
  const task = taskSnap.data()!;
  if (!["accepted", "started", "submitted", "completed"].includes(task.status) ||
      (task.posterId !== uid && task.workerId !== uid)) {
    throw new functions.https.HttpsError("permission-denied", "Task contact is available only to its participants.");
  }
  const counterpartUid = task.posterId === uid ? task.workerId : task.posterId;
  if (typeof counterpartUid !== "string" || !counterpartUid) {
    throw new functions.https.HttpsError("failed-precondition", "Task has no other participant yet.");
  }
  const userSnap = await db.collection("users").doc(counterpartUid).get();
  if (!userSnap.exists || typeof userSnap.get("phone") !== "string") {
    throw new functions.https.HttpsError("not-found", "Counterpart contact is unavailable.");
  }
  return { phone: userSnap.get("phone") };
});

export const onTaskStatusChanged = functions.firestore
  .document("tasks/{taskId}")
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();
    if (before.status === after.status) return;
    const participants = [...new Set([after.posterId, after.workerId].filter(
      (uid): uid is string => typeof uid === "string" && uid.length > 0
    ))];
    const message = `Task "${after.title ?? "Task"}" is now ${after.status}.`;
    await Promise.all(participants.map((uid) => db
      .collection("notifications").doc(uid).collection("items")
      .doc(`${context.params.taskId}_${after.status}`)
      .set({
        taskId: context.params.taskId,
        type: "task_status",
        title: "Task update",
        message,
        status: after.status,
        read: false,
        createdAt: FieldValue.serverTimestamp(),
      })));

    await sendPushToUsers(participants, "Task update", message, context.params.taskId);
  });

async function sendPushToUsers(
  userIds: string[],
  title: string,
  body: string,
  taskId: string
): Promise<void> {
  if (process.env.FUNCTIONS_EMULATOR === "true" || userIds.length === 0) return;

  const userSnapshots = await Promise.all(
    userIds.map((uid) => db.collection("users").doc(uid).get())
  );
  const tokens = [...new Set(userSnapshots
    .map((user) => user.get("fcmToken"))
    .filter((token): token is string => typeof token === "string" && token.length > 0))];
  for (let offset = 0; offset < tokens.length; offset += 500) {
    await admin.messaging().sendEachForMulticast({
      tokens: tokens.slice(offset, offset + 500),
      notification: { title, body },
      data: { taskId },
    });
  }
}

export const autoReleaseTasks = functions.pubsub
  .schedule("every 60 minutes")
  .onRun(async () => {
    const configSnap = await db.collection("config").doc("app").get();
    const hours = Number(configSnap.data()?.autoReleaseHours ?? 72);
    const cutoff = new Date(Date.now() - hours * 60 * 60 * 1000);
    const tasks = await db.collection("tasks")
      .where("status", "==", "submitted")
      .where("proofSubmittedAt", "<=", admin.firestore.Timestamp.fromDate(cutoff))
      .get();
    for (const task of tasks.docs) {
      await executeApproveTask(String(task.get("posterId")), task.id);
    }
    return null;
  });
