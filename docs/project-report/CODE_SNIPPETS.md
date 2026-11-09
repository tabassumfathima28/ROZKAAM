# ROZKAAM Academic Project Report: Code Snippets

The excerpts below are taken from the current ROZKAAM source tree. They show the
implemented flow, not pseudocode. Paths are relative to the repository root.
Ellipses are used only where unrelated lines are omitted from an excerpt.
Monetary values in the backend are integer paise; the UI formats amounts as
rupees.

## 1. Firebase Phone OTP Authentication

**Source files:** [AuthViewModel.kt](../../app/src/main/java/com/rozkaam/app/ui/viewmodels/AuthViewModel.kt), [AuthRepository.kt](../../app/src/main/java/com/rozkaam/app/data/repository/AuthRepository.kt)

```kotlin
fun verifyOtp() {
    val verId = verificationId
    val code = _uiState.value.otpCode

    if (verId == null) {
        _uiState.update { it.copy(errorMessage = "Please request an OTP first.") }
        return
    }

    if (code.length != 6) {
        _uiState.update { it.copy(errorMessage = "Please enter the complete 6-digit OTP.") }
        return
    }

    _uiState.update { it.copy(isLoading = true, errorMessage = null) }

    viewModelScope.launch {
        val result = authRepository.verifyOtp(verId, code)
        result.fold(
            onSuccess = { user ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isCheckingProfile = true,
                        signedInUser = user
                    )
                }
            },
            onFailure = { error ->
                val message = when (error) {
                    is FirebaseAuthInvalidCredentialsException ->
                        "Wrong 6-digit OTP entered. Please check the code and try again."
                    is FirebaseTooManyRequestsException ->
                        "Too many incorrect attempts. Please wait before trying again."
                    else ->
                        error.localizedMessage ?: "Verification failed. Please check your code and try again."
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = message) }
            }
        )
    }
}
```

```kotlin
override suspend fun verifyOtp(verificationId: String, otpCode: String): Result<FirebaseUser> {
    return try {
        val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
        val authResult = auth.signInWithCredential(credential).await()
        val user = authResult.user ?: throw IllegalStateException("Firebase user is null after sign in")
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Explanation:** The ViewModel requires a six-digit code and an active Firebase
verification ID before attempting sign-in. It delegates credential creation
and authentication to the repository, exposes loading state while the request
runs, and reports authentication errors to the UI. Firebase Phone Auth performs
the credential verification; the application does not store or validate OTPs
itself.

**Significance:** Phone-based sign-in gives each ROZKAAM account an authenticated
Firebase UID used to scope its profile, tasks, wallet, and privileged backend
operations.

## 2. User Profile Management

**Source file:** [UserRepository.kt](../../app/src/main/java/com/rozkaam/app/data/repository/UserRepository.kt)

```kotlin
override suspend fun saveUserProfile(user: User): Result<Unit> {
    return try {
        val docRef = usersCollection.document(user.uid)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            // Document already exists, only update client-mutable fields to comply with security rules
            val updates = mutableMapOf<String, Any?>(
                "name" to user.name,
                "activeMode" to user.activeMode,
                "emergencyContact" to user.emergencyContact
            )
            if (user.photoUrl.isNotBlank()) {
                updates["photoUrl"] = user.photoUrl
            }
            docRef.update(updates).await()
        } else {
            // Initial creation with all base fields
            docRef.set(user.toMap()).await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Explanation:** The repository stores the profile under `users/{uid}`. It
distinguishes an initial profile document from an existing one. For an existing
profile, it sends only the client-editable profile fields; it does not overwrite
server-managed counters such as ratings or completed-task totals. Errors are
returned as a failed `Result` for the calling layer to handle.

**Significance:** UID-keyed profiles connect authentication to the app’s
Worker/Poster modes while helping preserve backend-owned account data.

## 3. Task Data Model

**Source file:** [Task.kt](../../app/src/main/java/com/rozkaam/app/data/model/Task.kt)

```kotlin
data class Task(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val price: Long = 0L, // in paise
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val geohash: String = "",
    val address: String = "",
    val posterId: String = "",
    val workerId: String? = null,
    val status: String = STATUS_OPEN,
    val scheduledFor: String = "Now",
    val proofPath: String? = null,
    val proofDescription: String? = null,
    val proofQrVerifiedAt: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val acceptedAt: Timestamp? = null,
    val rated: Boolean = false
) {
    ...
    companion object {
        const val STATUS_OPEN = "open"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_STARTED = "started"
        const val STATUS_SUBMITTED = "submitted"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_CANCELLED = "cancelled"
    }
}
```

**Explanation:** `Task` represents the Firestore task document in the Android
client. It combines task description and location with poster/worker assignment,
paise-denominated price, lifecycle status, proof references, and timestamps.
Nullable assignment and proof fields represent information that is not yet
available at every stage of a task.

**Significance:** A shared typed model lets screens and ViewModels render the
task lifecycle consistently, from an open listing through proof submission and
completion.

## 4. Task Creation and Geohash Generation

**Source file:** [tasks.ts](../../functions/src/tasks.ts)

The following excerpt is from `executePostTask`, inside its Firestore
transaction. The available-balance check occurs earlier in the same function.

```typescript
const newAvailable = currentAvailable - price;
const newHeld = currentHeld + price;
const geohash = encodeGeohash(lat, lng, 9);

const otp = generateTaskOtp();

// 1. Move task price from available to held in user wallet
transaction.set(
  walletRef,
  {
    available: newAvailable,
    held: newHeld,
    updatedAt: FieldValue.serverTimestamp(),
  },
  { merge: true }
);

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
  createdAt: FieldValue.serverTimestamp(),
  updatedAt: FieldValue.serverTimestamp(),
});

// 3. Create taskSecrets with arrival OTP readable only by poster
const qrToken = randomBytes(16).toString("hex");
transaction.set(secretRef, {
  taskId: taskRef.id,
  posterId: uid,
  otp: otp,
  qrToken: qrToken,
  proofQrToken: randomBytes(32).toString("hex"),
  proofQrFailedAttempts: 0,
  failedAttempts: 0,
  createdAt: FieldValue.serverTimestamp(),
});

// 4. Record hold in immutable transactions ledger
transaction.set(transactionRef, {
  uid: uid,
  type: "hold",
  amount: price,
  taskId: taskRef.id,
  razorpayPaymentId: null,
  createdAt: FieldValue.serverTimestamp(),
});
```

**Explanation:** The backend computes a precision-9 geohash from the task
coordinates and groups the escrow hold, task document, task verification
secrets, and hold-ledger entry in one Firestore transaction. If the poster
does not have enough available wallet funds, the function rejects the request
before these writes.

**Significance:** Server-side task creation prevents clients from independently
changing task state or wallet balances and keeps the initial escrow accounting
consistent with the created task.

## 5. Nearby Task Discovery

**Source file:** [TaskRepository.kt](../../app/src/main/java/com/rozkaam/app/data/repository/TaskRepository.kt)

```kotlin
override fun getNearbyOpenTasks(
workerLat: Double,
workerLng: Double,
radiusKm: Double,
workerUid: String
): Flow<List<TaskWithDistance>> = callbackFlow {
val maxDistanceMeters = radiusKm * 1000.0

val listenerRegistration = firestore.collection("tasks")
    .whereEqualTo("status", Task.STATUS_OPEN)
    .addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) {
            var totalOpen = 0
            var selfExcluded = 0
            var distanceExcluded = 0
            val tasks = mutableListOf<TaskWithDistance>()

            for (doc in snapshot.documents) {
                totalOpen++
                val task = Task.fromMap(doc.id, doc.data)
                // Exclude tasks created by the worker themselves (SPEC.md A7)
                if (task.posterId == workerUid) {
                    selfExcluded++
                    continue
                }

                val dist = calculateDistanceMeters(workerLat, workerLng, task.lat, task.lng)
                if (dist <= maxDistanceMeters) {
                    tasks.add(TaskWithDistance(task = task, distanceMeters = dist))
                } else {
                    distanceExcluded++
                }
            }

            tasks.sortBy { it.distanceMeters }
            Log.d("TaskDiscovery", "Found $totalOpen open tasks ($selfExcluded self-excluded, $distanceExcluded out of radius ${radiusKm}km). Eligible: ${tasks.size}")
            trySend(tasks)
        } else {
            trySend(emptyList())
        }
    }

awaitClose { listenerRegistration.remove() }
}
```

**Explanation:** A Firestore listener streams open tasks. The repository excludes
tasks posted by the current worker, computes the exact distance from worker to
task coordinates with `calculateDistanceMeters`, filters to the configured
radius, and sorts the remaining results by distance.

**Implementation note:** The current code uses an open-status Firestore query
followed by client-side exact-distance filtering. Although tasks store a
geohash, this method does not currently query geohash ranges first; therefore
it is not the geohash-range discovery approach described in `SPEC.md` A10.

**Significance:** This flow provides live nearby listings and avoids presenting
the worker with their own tasks or tasks outside their selected search radius.

## 6. Secure Task Acceptance with a Firestore Transaction

**Source file:** [tasks.ts](../../functions/src/tasks.ts)

```typescript
export async function executeAcceptTask(
  workerUid: string,
  taskId: string
): Promise<AcceptTaskResult> {
  const userRef = db.collection("users").doc(workerUid);
  const taskRef = db.collection("tasks").doc(taskId);

  return await db.runTransaction(async (transaction) => {
    // 1. Verify worker profile and availability
    const userSnap = await transaction.get(userRef);
    if (!userSnap.exists) {
      throw new functions.https.HttpsError(
        "not-found",
        "Worker user profile not found."
      );
    }

    const userData = userSnap.data()!;
    if (userData.available !== true) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Worker is currently set to unavailable. Please turn on 'I am free now' to accept tasks."
      );
    }

    // 2. Verify task status and eligibility
    const taskSnap = await transaction.get(taskRef);
    if (!taskSnap.exists) {
      throw new functions.https.HttpsError(
        "not-found",
        "Task not found."
      );
    }

    const taskData = taskSnap.data()!;
    if (taskData.posterId === workerUid) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "You cannot accept your own task."
      );
    }

    if (taskData.status !== "open") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "This task is no longer open for acceptance."
      );
    }

    // 3. Atomically assign task to worker (preserves existing escrow held balance)
    transaction.update(taskRef, {
      status: "accepted",
      workerId: workerUid,
      acceptedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });

    functions.logger.info(
      `Task ${taskId} accepted by worker ${workerUid}.`
    );

    return {
      success: true,
      taskId: taskId,
      status: "accepted",
    };
  });
}
```

**Explanation:** The callable uses the authenticated worker UID and performs
eligibility checks and task assignment in a Firestore transaction. It rejects
missing profiles, unavailable workers, self-acceptance, and tasks that are no
longer open. Concurrent acceptance attempts contend on the task document, so
only an eligible transaction can change its status from `open` and assign its
worker.

**Significance:** Centralizing acceptance in a backend transaction prevents
clients from directly assigning themselves or competing workers to the same
task and leaves the existing escrow balance untouched.

The callable obtains the worker identity from Firebase Authentication before
calling the transaction logic:

```typescript
export const acceptTask = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "User must be authenticated to accept a task."
    );
  }

  const workerUid = context.auth.uid;
  const taskId = String(data?.taskId || "").trim();

  if (!taskId) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Task ID is required."
    );
  }

  return await executeAcceptTask(workerUid, taskId);
});
```

## 7. Photo Proof Upload and Submission

**Source files:** [TaskProofRepository.kt](../../app/src/main/java/com/rozkaam/app/data/repository/TaskProofRepository.kt), [lifecycle.ts](../../functions/src/lifecycle.ts)

The Android repository uploads the selected image, then invokes the backend
callable with the exact Storage path and description:

```kotlin
val extension = if (contentType == "image/jpeg") "jpg" else "png"
val proofPath = "task-proofs/$taskId/$workerUid/${UUID.randomUUID()}.$extension"
val metadata = StorageMetadata.Builder()
    .setContentType(contentType)
    .build()

storage.reference
    .child(proofPath)
    .putFile(imageUri, metadata)
    .await()

...

val result = functions.getHttpsCallable("submitTaskProof")
    .call(
        mapOf(
            "taskId" to taskId,
            "proofPath" to proofPath,
            "description" to normalizedDescription
        )
    )
    .await()
```

The backend confirms the object exists and validates it before applying the
task update transactionally:

```typescript
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
```

**Explanation:** The client creates a task- and worker-scoped Storage path and
uploads the actual selected file with its image content type. The backend then
checks the path, object existence, content type, size, task, and task secret.
`createProofSubmissionUpdate` additionally verifies that the caller is the
assigned worker and that the task is `started`, and returns the `submitted`
status with the proof path and description. The Firestore transaction records
that update and ensures a proof QR token is present.

**Significance:** Upload and metadata submission are linked by the Storage
reference; task state advances only after backend checks, rather than relying
on a client-only success state.

## 8. Task Approval and Wallet Settlement

**Source file:** [lifecycle.ts](../../functions/src/lifecycle.ts)

```typescript
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
```

**Explanation:** The backend checks that the caller owns the task, the task is
submitted, proof QR verification has taken place, and the worker and price are
valid. In one Firestore transaction it reduces the poster's held amount,
credits the worker's available balance, increments the worker's completion
count, marks the task completed, and creates poster/worker release-ledger
entries. Deterministic ledger document IDs provide a duplicate-release guard.

**Significance:** Wallet mutation is kept on the trusted backend and coupled to
task completion and ledger writes, preserving a consistent record of the
escrow-style in-app settlement. This is an application wallet ledger, not a
claim that regulated real-world escrow or bank payouts are enabled.

The callable binds the operation to the signed-in poster:

```typescript
export const approveTask = functions.https.onCall(async (data, context) => {
  return executeApproveTask(requireAuth(context), requireTaskId(data?.taskId));
});
```

## Suggested Optional Snippets

- **Razorpay payment verification:** [topup.ts](../../functions/src/topup.ts) —
  `verifyTopUp` shows server-side signature verification and wallet credit
  handling. Keep the Key Secret in server configuration; do not reproduce
  secrets or credentials in the report.
- **Firebase security rules:** [firestore.rules](../../firestore.rules) —
  the owner-only profile and wallet reads, server-only task/wallet writes, and
  default-deny rule are useful examples. [storage.rules](../../storage.rules)
  documents participant-only proof reads and assigned-worker proof uploads.
