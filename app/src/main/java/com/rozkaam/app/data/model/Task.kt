package com.rozkaam.app.data.model

import com.google.firebase.Timestamp

/**
 * Task domain and Firestore model according to Section A6 of SPEC.md.
 * All financial amounts are represented in integer paise.
 */
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
    val priceRupees: Double
        get() = price / 100.0

    val formattedPrice: String
        get() = "₹${price / 100}"

    val isOpen: Boolean
        get() = status == STATUS_OPEN

    val isAccepted: Boolean
        get() = status == STATUS_ACCEPTED

    val isStarted: Boolean
        get() = status == STATUS_STARTED

    val isSubmitted: Boolean
        get() = status == STATUS_SUBMITTED

    val isProofQrVerified: Boolean
        get() = proofQrVerifiedAt != null

    val isCompleted: Boolean
        get() = status == STATUS_COMPLETED

    val isCancelled: Boolean
        get() = status == STATUS_CANCELLED

    companion object {
        const val STATUS_OPEN = "open"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_STARTED = "started"
        const val STATUS_SUBMITTED = "submitted"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_CANCELLED = "cancelled"

        fun fromMap(id: String, map: Map<String, Any?>?): Task {
            if (map == null) return Task(id = id)
            return Task(
                id = id,
                title = (map["title"] as? String).orEmpty(),
                description = (map["description"] as? String).orEmpty(),
                category = (map["category"] as? String).orEmpty(),
                price = (map["price"] as? Number)?.toLong() ?: 0L,
                lat = (map["lat"] as? Number)?.toDouble() ?: 0.0,
                lng = (map["lng"] as? Number)?.toDouble() ?: 0.0,
                geohash = (map["geohash"] as? String).orEmpty(),
                address = (map["address"] as? String).orEmpty(),
                posterId = (map["posterId"] as? String).orEmpty(),
                workerId = map["workerId"] as? String,
                status = (map["status"] as? String) ?: STATUS_OPEN,
                scheduledFor = (map["scheduledFor"] as? String) ?: "Now",
                proofPath = map["proofPath"] as? String,
                proofDescription = map["proofDescription"] as? String,
                proofQrVerifiedAt = map["proofQrVerifiedAt"] as? Timestamp,
                createdAt = map["createdAt"] as? Timestamp,
                updatedAt = map["updatedAt"] as? Timestamp,
                acceptedAt = map["acceptedAt"] as? Timestamp,
                rated = (map["rated"] as? Boolean) ?: false
            )
        }
    }
}

/**
 * Task item paired with calculated geospatial distance from worker location.
 */
data class TaskWithDistance(
    val task: Task,
    val distanceMeters: Double
) {
    val formattedDistance: String
        get() = if (distanceMeters < 1000) {
            "${distanceMeters.toInt()} m away"
        } else {
            "%.1f km away".format(distanceMeters / 1000.0)
        }
}

/**
 * Result returned by the postTask Cloud Function.
 */
data class PostTaskResult(
    val success: Boolean,
    val taskId: String,
    val price: Long,
    val available: Long,
    val held: Long
)
