package com.rozkaam.app.data.repository

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

interface TaskWorkflowRepository {
    suspend fun startTask(
        taskId: String,
        otp: String,
        lat: Double,
        lng: Double,
        qrToken: String? = null
    ): Result<Unit>
    suspend fun verifyTaskProofQr(
        taskId: String,
        scannedTaskId: String,
        qrToken: String
    ): Result<Unit>
    suspend fun approveTask(taskId: String): Result<Unit>
    suspend fun cancelTask(taskId: String): Result<Unit>
    suspend fun rateWorker(taskId: String, stars: Int, comment: String): Result<Unit>
    suspend fun requestWithdrawal(amountPaise: Long, upiId: String): Result<String>
    suspend fun processTestWithdrawal(withdrawalId: String): Result<Unit>
    suspend fun getCounterpartContact(taskId: String): Result<String> = Result.success("+919876543210")
}

class FirebaseTaskWorkflowRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) : TaskWorkflowRepository {

    override suspend fun startTask(
        taskId: String,
        otp: String,
        lat: Double,
        lng: Double,
        qrToken: String?
    ): Result<Unit> {
        val payload = mutableMapOf<String, Any>(
            "taskId" to taskId,
            "otp" to otp,
            "lat" to lat,
            "lng" to lng
        )
        if (!qrToken.isNullOrBlank()) {
            payload["qrToken"] = qrToken
        }
        return call("startTask", payload)
    }

    override suspend fun verifyTaskProofQr(
        taskId: String,
        scannedTaskId: String,
        qrToken: String
    ): Result<Unit> = call(
        "verifyTaskProofQr",
        mapOf("taskId" to taskId, "scannedTaskId" to scannedTaskId, "qrToken" to qrToken)
    )

    override suspend fun getCounterpartContact(taskId: String): Result<String> =
        runCatching {
            val response = functions.getHttpsCallable("getCounterpartContact")
                .call(mapOf("taskId" to taskId))
                .await()
                .getData() as? Map<*, *>
                ?: error("Invalid response from getCounterpartContact.")
            response["phone"] as? String ?: error("Counterpart phone number is unavailable.")
        }.unwrapCallableError()

    override suspend fun approveTask(taskId: String): Result<Unit> =
        call("approveTask", mapOf("taskId" to taskId))

    override suspend fun cancelTask(taskId: String): Result<Unit> =
        call("cancelTask", mapOf("taskId" to taskId))

    override suspend fun rateWorker(taskId: String, stars: Int, comment: String): Result<Unit> =
        call("rateWorker", mapOf("taskId" to taskId, "stars" to stars, "comment" to comment))

    override suspend fun requestWithdrawal(amountPaise: Long, upiId: String): Result<String> =
        runCatching {
            val response = functions.getHttpsCallable("requestWithdrawal")
                .call(mapOf("amount" to amountPaise, "upiId" to upiId))
                .await()
                .getData() as? Map<*, *>
                ?: error("Invalid withdrawal response.")
            if (response["success"] != true) error("Withdrawal request was not confirmed.")
            response["withdrawalId"] as? String ?: error("Withdrawal reference is missing.")
        }.unwrapCallableError()

    override suspend fun processTestWithdrawal(withdrawalId: String): Result<Unit> =
        call("processWithdrawal", mapOf("withdrawalId" to withdrawalId))

    private suspend fun call(name: String, data: Map<String, Any>): Result<Unit> =
        runCatching {
            val response = functions.getHttpsCallable(name).call(data).await().getData() as? Map<*, *>
                ?: error("Invalid response from $name.")
            if (response["success"] != true) error("$name was not confirmed.")
        }.unwrapCallableError()

    private fun <T> Result<T>.unwrapCallableError(): Result<T> = recoverCatching { error ->
        if (error is java.util.concurrent.ExecutionException) throw (error.cause ?: error)
        throw error
    }
}
