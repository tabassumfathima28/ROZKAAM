package com.rozkaam.app.data.model

/**
 * Result returned by createTopUpOrder Cloud Function
 */
data class TopUpOrderResult(
    val orderId: String,
    val amount: Long,      // in paise
    val currency: String,
    val keyId: String
)

/**
 * Result returned by verifyTopUp Cloud Function
 */
data class VerifyTopUpResult(
    val success: Boolean,
    val alreadyProcessed: Boolean,
    val orderId: String,
    val available: Long,   // in paise
    val amount: Long       // in paise
)

