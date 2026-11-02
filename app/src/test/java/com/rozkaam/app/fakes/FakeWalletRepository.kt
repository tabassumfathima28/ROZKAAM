package com.rozkaam.app.fakes

import com.rozkaam.app.data.model.AppConfig
import com.rozkaam.app.data.model.TopUpOrderResult
import com.rozkaam.app.data.model.Transaction
import com.rozkaam.app.data.model.VerifyTopUpResult
import com.rozkaam.app.data.model.Wallet
import com.rozkaam.app.data.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeWalletRepository(
    initialWallet: Wallet = Wallet(available = 250000L, held = 50000L), // ₹2500 available, ₹500 held
    initialTransactions: List<Transaction> = emptyList(),
    initialConfig: AppConfig = AppConfig()
) : WalletRepository {

    val walletFlow = MutableStateFlow(initialWallet)
    val transactionsFlow = MutableStateFlow(initialTransactions)
    val configFlow = MutableStateFlow(initialConfig)

    var shouldFailOrderCreation: Boolean = false
    var shouldFailVerification: Boolean = false
    var verifyCallsCount: Int = 0

    override fun getWallet(uid: String): Flow<Wallet> = walletFlow.asStateFlow()

    override fun getTransactions(uid: String): Flow<List<Transaction>> = transactionsFlow.asStateFlow()

    override fun getAppConfig(): Flow<AppConfig> = configFlow.asStateFlow()

    override suspend fun createTopUpOrder(amountPaise: Long): Result<TopUpOrderResult> {
        return if (shouldFailOrderCreation) {
            Result.failure(Exception("Order creation failed on backend"))
        } else {
            Result.success(
                TopUpOrderResult(
                    orderId = "order_test_${System.currentTimeMillis()}",
                    amount = amountPaise,
                    currency = "INR",
                    keyId = "rzp_test_123456789"
                )
            )
        }
    }

    override suspend fun verifyTopUp(
        orderId: String,
        paymentId: String,
        signature: String
    ): Result<VerifyTopUpResult> {
        verifyCallsCount++
        return if (shouldFailVerification) {
            Result.failure(Exception("Invalid signature or verification failed"))
        } else {
            val amount = 50000L // ₹500
            val newAvailable = walletFlow.value.available + amount
            walletFlow.value = walletFlow.value.copy(available = newAvailable)

            val updatedTransactions = transactionsFlow.value.toMutableList()
            updatedTransactions.add(
                0,
                Transaction(
                    id = "tx_test_${System.currentTimeMillis()}",
                    uid = "test_user",
                    type = "topup",
                    amount = amount,
                    razorpayPaymentId = paymentId,
                    createdAt = System.currentTimeMillis()
                )
            )
            transactionsFlow.value = updatedTransactions

            Result.success(
                VerifyTopUpResult(
                    success = true,
                    alreadyProcessed = false,
                    orderId = orderId,
                    available = newAvailable,
                    amount = amount
                )
            )
        }
    }
}

