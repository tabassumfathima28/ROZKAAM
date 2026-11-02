package com.rozkaam.app.data.model

/**
 * Wallet data model representing a user's wallet in Firestore.
 * Stored at wallets/{uid} per SPEC.md Section A6.
 *
 * All money is stored in paise as 64-bit integers.
 * The UI presents amounts in Indian Rupees (₹).
 */
data class Wallet(
    val available: Long = 0L, // in paise
    val held: Long = 0L       // in paise
) {
    val availableRupees: Double get() = available / 100.0
    val heldRupees: Double get() = held / 100.0

    val formattedAvailable: String get() = "₹${available / 100}"
    val formattedHeld: String get() = "₹${held / 100}"

    val totalBalance: Long get() = available + held
    val formattedTotal: String get() = "₹${totalBalance / 100}"

    companion object {
        fun fromMap(map: Map<String, Any?>?): Wallet {
            if (map == null) return Wallet()
            val available = (map["available"] as? Number)?.toLong() ?: 0L
            val held = (map["held"] as? Number)?.toLong() ?: 0L
            return Wallet(
                available = available,
                held = held
            )
        }
    }
}

