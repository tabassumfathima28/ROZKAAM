package com.rozkaam.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserModelTest {

    @Test
    fun testUserSerializationAndDeserialization() {
        val user = User(
            uid = "user_123",
            name = "Ramesh Kumar",
            phone = "+919876543210",
            photoUrl = "https://example.com/avatar.jpg",
            activeMode = User.MODE_WORKER,
            emergencyContact = "+919876543211",
            available = false,
            radiusKm = 7,
            dailyGoal = 60000L,
            rating = 4.8,
            ratingCount = 12,
            completedCount = 15,
            fcmToken = "token_abc",
            createdAt = 1700000000000L
        )

        val map = user.toMap()
        assertEquals("Ramesh Kumar", map["name"])
        assertEquals("+919876543210", map["phone"])
        assertEquals(User.MODE_WORKER, map["activeMode"])
        assertEquals(4.8, map["rating"])
        assertEquals(15, map["completedCount"])
        assertTrue(user.isProfileComplete)

        // Test safe deserialization with mixed types (e.g. Long as Int or Double as Long)
        val firestoreMap = mapOf<String, Any?>(
            "name" to "Ramesh Kumar",
            "phone" to "+919876543210",
            "photoUrl" to "",
            "activeMode" to "poster",
            "emergencyContact" to "+919876543211",
            "available" to false,
            "radiusKm" to 10L, // Long from Firestore
            "dailyGoal" to 50000, // Int from Firestore
            "rating" to 5, // Int from Firestore
            "ratingCount" to 10L,
            "completedCount" to 20L,
            "createdAt" to 1700000000000L
        )

        val parsed = User.fromDocument("user_123", firestoreMap)
        assertEquals("user_123", parsed.uid)
        assertEquals("Ramesh Kumar", parsed.name)
        assertEquals("poster", parsed.activeMode)
        assertEquals(10, parsed.radiusKm)
        assertEquals(50000L, parsed.dailyGoal)
        assertEquals(5.0, parsed.rating, 0.001)
        assertEquals(10, parsed.ratingCount)
        assertEquals(20, parsed.completedCount)
        assertFalse(parsed.available)
        assertTrue(parsed.isProfileComplete)
    }

    @Test
    fun testIsProfileComplete_WhenNameOrEmergencyMissing_ReturnsFalse() {
        val userNoName = User(uid = "123", name = "", emergencyContact = "+919876543210")
        assertFalse(userNoName.isProfileComplete)

        val userNoEmergency = User(uid = "123", name = "Ramesh Kumar", emergencyContact = "")
        assertFalse(userNoEmergency.isProfileComplete)

        val userBoth = User(uid = "123", name = "Ramesh Kumar", emergencyContact = "+919876543210")
        assertTrue(userBoth.isProfileComplete)
    }

    @Test
    fun testPublicProfileFromDocument() {
        val firestoreMap = mapOf<String, Any?>(
            "name" to "Ayesha Fathima",
            "photoUrl" to "https://photo.url",
            "rating" to 4.9,
            "completedCount" to 8
        )

        val profile = PublicProfile.fromDocument("user_456", firestoreMap)
        assertEquals("user_456", profile.uid)
        assertEquals("Ayesha Fathima", profile.name)
        assertEquals("https://photo.url", profile.photoUrl)
        assertEquals(4.9, profile.rating, 0.001)
        assertEquals(8, profile.completedCount)
    }
}
