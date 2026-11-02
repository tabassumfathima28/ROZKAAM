package com.rozkaam.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Data model for users/{uid} in Firestore.
 * Matches SPEC.md Section A6:
 * users/{uid}: name, phone, photoUrl, activeMode, emergencyContact, available,
 * radiusKm, dailyGoal, rating, ratingCount, completedCount, fcmToken, createdAt.
 *
 * Readable only by the owner. The client may write only name, photoUrl, activeMode,
 * emergencyContact, available, radiusKm, dailyGoal, fcmToken.
 */
@IgnoreExtraProperties
data class User(
    val uid: String = "",
    val name: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val activeMode: String = MODE_WORKER, // "worker" or "poster"
    val emergencyContact: String = "",
    val available: Boolean = false,
    val radiusKm: Int = 5,
    val dailyGoal: Long = 50000L, // 50000 paise = ₹500
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val completedCount: Int = 0,
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * A profile is complete when the user has provided their real name
     * and an emergency contact for SOS alerts.
     */
    val isProfileComplete: Boolean
        get() = name.isNotBlank() && emergencyContact.isNotBlank()

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "phone" to phone,
            "photoUrl" to photoUrl,
            "activeMode" to activeMode,
            "emergencyContact" to emergencyContact,
            "available" to available,
            "radiusKm" to radiusKm,
            "dailyGoal" to dailyGoal,
            "rating" to rating,
            "ratingCount" to ratingCount,
            "completedCount" to completedCount,
            "fcmToken" to fcmToken,
            "createdAt" to createdAt
        )
    }

    companion object {
        const val MODE_WORKER = "worker"
        const val MODE_POSTER = "poster"

        fun fromDocument(uid: String, data: Map<String, Any?>?): User {
            if (data == null) return User(uid = uid)

            val ratingVal = when (val r = data["rating"]) {
                is Number -> r.toDouble()
                else -> 0.0
            }
            val radiusVal = when (val r = data["radiusKm"]) {
                is Number -> r.toInt()
                else -> 5
            }
            val dailyGoalVal = when (val dg = data["dailyGoal"]) {
                is Number -> dg.toLong()
                else -> 50000L
            }
            val ratingCountVal = when (val rc = data["ratingCount"]) {
                is Number -> rc.toInt()
                else -> 0
            }
            val completedCountVal = when (val cc = data["completedCount"]) {
                is Number -> cc.toInt()
                else -> 0
            }
            val createdAtVal = when (val ca = data["createdAt"]) {
                is Number -> ca.toLong()
                is Timestamp -> ca.toDate().time
                else -> System.currentTimeMillis()
            }

            return User(
                uid = uid,
                name = data["name"] as? String ?: "",
                phone = data["phone"] as? String ?: "",
                photoUrl = data["photoUrl"] as? String ?: "",
                activeMode = data["activeMode"] as? String ?: MODE_WORKER,
                emergencyContact = data["emergencyContact"] as? String ?: "",
                available = data["available"] as? Boolean ?: false,
                radiusKm = radiusVal,
                dailyGoal = dailyGoalVal,
                rating = ratingVal,
                ratingCount = ratingCountVal,
                completedCount = completedCountVal,
                fcmToken = data["fcmToken"] as? String ?: "",
                createdAt = createdAtVal
            )
        }
    }
}
