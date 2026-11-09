package com.rozkaam.app.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.util.UUID

interface TaskProofRepository {
    suspend fun getProofImage(storagePath: String): Result<Bitmap>

    suspend fun submitProof(
        taskId: String,
        imageUri: Uri,
        contentType: String,
        description: String
    ): Result<Unit>
}

class FirebaseTaskProofRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : TaskProofRepository {

    override suspend fun getProofImage(storagePath: String): Result<Bitmap> = runCatching {
        require(storagePath.startsWith("task-proofs/")) { "Invalid proof storage path." }
        val bytes = storage.reference.child(storagePath)
            .getBytes(MAX_PROOF_SIZE_BYTES)
            .await()
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalStateException("The uploaded proof is not a readable image.")
    }

    override suspend fun submitProof(
        taskId: String,
        imageUri: Uri,
        contentType: String,
        description: String
    ): Result<Unit> = runCatching {
        require(taskId.isNotBlank() && !taskId.contains('/')) { "Invalid task ID." }
        require(contentType == "image/jpeg" || contentType == "image/png") {
            "Choose a JPEG or PNG photo."
        }
        val workerUid = auth.currentUser?.uid
            ?: throw IllegalStateException("Sign in as the assigned worker to submit proof.")
        val projectId = FirebaseApp.getInstance().options.projectId
        require(
            projectId == "rozkaam-70984" &&
                storage.app.options.projectId == projectId
        ) {
            "Firebase Auth, Storage, and Functions must use project rozkaam-70984."
        }
        val normalizedDescription = description.trim()
        require(normalizedDescription.length in 5..500) {
            "Proof description must be between 5 and 500 characters."
        }

        val extension = if (contentType == "image/jpeg") "jpg" else "png"
        val proofPath = "task-proofs/$taskId/$workerUid/${UUID.randomUUID()}.$extension"
        val metadata = StorageMetadata.Builder()
            .setContentType(contentType)
            .build()

        try {
            storage.reference
                .child(proofPath)
                .putFile(imageUri, metadata)
                .await()
        } catch (error: StorageException) {
            if (error.errorCode == StorageException.ERROR_NOT_AUTHORIZED) {
                throw IllegalStateException(
                    "Firebase Storage denied this proof upload. Confirm this signed-in account is the assigned worker, the task is started, and the Storage and Firestore emulators are running for project rozkaam-70984.",
                    error
                )
            }
            throw error
        }

        val result = try {
            functions.getHttpsCallable("submitTaskProof")
                .call(
                    mapOf(
                        "taskId" to taskId,
                        "proofPath" to proofPath,
                        "description" to normalizedDescription
                    )
                )
                .await()
        } catch (error: java.util.concurrent.ExecutionException) {
            throw (error.cause ?: error)
        }

        val response = result.getData() as? Map<*, *>
            ?: throw IllegalStateException("Invalid response from submitTaskProof function.")
        if (response["success"] != true) {
            throw IllegalStateException("Proof submission was not confirmed.")
        }
    }

    private companion object {
        const val MAX_PROOF_SIZE_BYTES = 5L * 1024L * 1024L
    }
}
