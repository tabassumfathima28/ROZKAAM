package com.rozkaam.app.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Data model for publicProfiles/{uid} in Firestore.
 * Matches SPEC.md Section A6:
 * Readable by signed-in users, maintained by a Cloud Function trigger.
 * Phone numbers, emergency contacts, and private fields are strictly excluded.
 */
@IgnoreExtraProperties
data class PublicProfile(
    val uid: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val rating: Double = 0.0,
    val completedCount: Int = 0
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "photoUrl" to photoUrl,
            "rating" to rating,
            "completedCount" to completedCount
        )
    }

    companion object {
        fun fromDocument(uid: String, data: Map<String, Any?>?): PublicProfile {
            if (data == null) return PublicProfile(uid = uid)
            val ratingVal = when (val r = data["rating"]) {
                is Number -> r.toDouble()
                else -> 0.0
            }
            val completedCountVal = when (val cc = data["completedCount"]) {
                is Number -> cc.toInt()
                else -> 0
            }
            return PublicProfile(
                uid = uid,
                name = data["name"] as? String ?: "",
                photoUrl = data["photoUrl"] as? String ?: "",
                rating = ratingVal,
                completedCount = completedCountVal
            )
        }
    }
}
