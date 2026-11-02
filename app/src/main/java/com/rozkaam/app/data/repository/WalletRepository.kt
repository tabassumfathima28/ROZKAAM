package com.rozkaam.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.rozkaam.app.data.model.AppConfig
import com.rozkaam.app.data.model.TopUpOrderResult
import com.rozkaam.app.data.model.Transaction
import com.rozkaam.app.data.model.VerifyTopUpResult
import com.rozkaam.app.data.model.Wallet
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface WalletRepository {
    fun getWallet(uid: String): Flow<Wallet>
    fun getTransactions(uid: String): Flow<List<Transaction>>
    suspend fun createTopUpOrder(amountPaise: Long): Result<TopUpOrderResult>
    suspend fun verifyTopUp(orderId: String, paymentId: String, signature: String): Result<VerifyTopUpResult>
    fun getAppConfig(): Flow<AppConfig>
}

class FirestoreWalletRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) : WalletRepository {

    override fun getWallet(uid: String): Flow<Wallet> = callbackFlow {
        if (uid.isBlank()) {
            trySend(Wallet())
            close()
            return@callbackFlow
        }

        val listenerRegistration = firestore.collection("wallets")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(Wallet.fromMap(snapshot.data))
                } else {
                    trySend(Wallet(available = 0L, held = 0L))
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    override fun getTransactions(uid: String): Flow<List<Transaction>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listenerRegistration = firestore.collection("transactions")
            .whereEqualTo("uid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        Transaction.fromMap(doc.id, doc.data)
                    }
                    trySend(list)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun createTopUpOrder(amountPaise: Long): Result<TopUpOrderResult> = runCatching {
        val data = hashMapOf(
            "amount" to amountPaise
        )

        val result = functions
            .getHttpsCallable("createTopUpOrder")
            .call(data)
            .await()

        @Suppress("UNCHECKED_CAST")
        val response = result.getData() as? Map<String, Any?>
            ?: throw IllegalStateException("Invalid response from createTopUpOrder function.")

        val orderId = response["orderId"] as? String
            ?: throw IllegalStateException("Order ID missing from response.")
        val amount = (response["amount"] as? Number)?.toLong() ?: amountPaise
        val currency = response["currency"] as? String ?: "INR"
        val keyId = response["keyId"] as? String ?: ""

        TopUpOrderResult(
            orderId = orderId,
            amount = amount,
            currency = currency,
            keyId = keyId
        )
    }

    override suspend fun verifyTopUp(
        orderId: String,
        paymentId: String,
        signature: String
    ): Result<VerifyTopUpResult> = runCatching {
        val data = hashMapOf(
            "orderId" to orderId,
            "paymentId" to paymentId,
            "signature" to signature
        )

        val result = functions
            .getHttpsCallable("verifyTopUp")
            .call(data)
            .await()

        @Suppress("UNCHECKED_CAST")
        val response = result.getData() as? Map<String, Any?>
            ?: throw IllegalStateException("Invalid response from verifyTopUp function.")

        val success = response["success"] as? Boolean ?: false
        val alreadyProcessed = response["alreadyProcessed"] as? Boolean ?: false
        val returnedOrderId = response["orderId"] as? String ?: orderId
        val available = (response["available"] as? Number)?.toLong() ?: 0L
        val amount = (response["amount"] as? Number)?.toLong() ?: 0L

        VerifyTopUpResult(
            success = success,
            alreadyProcessed = alreadyProcessed,
            orderId = returnedOrderId,
            available = available,
            amount = amount
        )
    }

    override fun getAppConfig(): Flow<AppConfig> = callbackFlow {
        val listenerRegistration = firestore.collection("config")
            .document("app")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Fall back to default config if error (e.g. offline)
                    trySend(AppConfig())
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(AppConfig.fromMap(snapshot.data))
                } else {
                    trySend(AppConfig())
                }
            }

        awaitClose { listenerRegistration.remove() }
    }
}
