package com.rozkaam.app.data.model

import com.google.firebase.Timestamp

data class WithdrawalRequest(
    val id: String,
    val amount: Long,
    val status: String,
    val createdAt: Long
) {
    val formattedAmount: String
        get() = "₹${amount / 100}"

    companion object {
        fun fromMap(id: String, data: Map<String, Any?>?): WithdrawalRequest {
            val createdAt = when (val value = data?.get("createdAt")) {
                is Timestamp -> value.toDate().time
                is Number -> value.toLong()
                else -> 0L
            }
            return WithdrawalRequest(
                id = id,
                amount = (data?.get("amount") as? Number)?.toLong() ?: 0L,
                status = data?.get("status") as? String ?: "unknown",
                createdAt = createdAt
            )
        }
    }
}
