package com.rozkaam.app.data.model

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Immutable ledger entry representing an escrow, top-up, release, refund, or withdrawal event.
 * Stored in append-only collection transactions/{id} per SPEC.md Section A6.
 */
data class Transaction(
    val id: String = "",
    val uid: String = "",
    val type: String = "", // "topup", "hold", "release", "refund", "withdrawal"
    val amount: Long = 0L, // in paise
    val taskId: String? = null,
    val razorpayPaymentId: String? = null,
    val createdAt: Long = 0L,
    val direction: String? = null
) {
    val amountRupees: Double get() = amount / 100.0

    /**
     * Determines whether this transaction credited the user's available balance.
     */
    val isCredit: Boolean
        get() = when (type) {
            "topup", "refund" -> true
            "release" -> direction != "debit"
            else -> false
        }

    val formattedAmount: String
        get() = "${if (isCredit) "+" else "-"}₹${amount / 100}"

    val displayTitle: String
        get() = when (type) {
            "topup" -> "Wallet Top-up"
            "hold" -> "Escrow Hold"
            "release" -> "Payment Released"
            "refund" -> "Escrow Refund"
            "withdrawal" -> "Bank Withdrawal"
            else -> "Transaction"
        }

    val formattedDate: String
        get() {
            if (createdAt == 0L) return "Recent"
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            return sdf.format(Date(createdAt))
        }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>?): Transaction {
            if (map == null) return Transaction(id = id)
            val uid = map["uid"] as? String ?: ""
            val type = map["type"] as? String ?: ""
            val direction = map["direction"] as? String
            val amount = (map["amount"] as? Number)?.toLong() ?: 0L
            val taskId = map["taskId"] as? String
            val razorpayPaymentId = map["razorpayPaymentId"] as? String
            val createdAt = when (val c = map["createdAt"]) {
                is Timestamp -> c.toDate().time
                is Number -> c.toLong()
                else -> 0L
            }
            return Transaction(
                id = id,
                uid = uid,
                type = type,
                direction = direction,
                amount = amount,
                taskId = taskId,
                razorpayPaymentId = razorpayPaymentId,
                createdAt = createdAt
            )
        }
    }
}
