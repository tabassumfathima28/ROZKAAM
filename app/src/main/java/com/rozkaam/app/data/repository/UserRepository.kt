package com.rozkaam.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.rozkaam.app.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface UserRepository {
    suspend fun getUser(uid: String): Result<User?>
    fun observeUser(uid: String): Flow<User?>
    suspend fun saveUserProfile(user: User): Result<Unit>
    suspend fun updateActiveMode(uid: String, activeMode: String): Result<Unit>
    suspend fun updateEmergencyContact(uid: String, emergencyContact: String): Result<Unit>
    suspend fun updateUserName(uid: String, name: String): Result<Unit>
    suspend fun updateAvailability(uid: String, available: Boolean, radiusKm: Int): Result<Unit>
}

class FirestoreUserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    private val usersCollection = firestore.collection("users")

    override suspend fun getUser(uid: String): Result<User?> {
        return try {
            val doc = usersCollection.document(uid).get().await()
            if (doc.exists()) {
                val user = User.fromDocument(uid, doc.data)
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val registration = usersCollection.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val user = User.fromDocument(uid, snapshot.data)
                trySend(user)
            } else {
                trySend(null)
            }
        }
        awaitClose {
            registration.remove()
        }
    }

    override suspend fun saveUserProfile(user: User): Result<Unit> {
        return try {
            val docRef = usersCollection.document(user.uid)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                // Document already exists, only update client-mutable fields to comply with security rules
                val updates = mutableMapOf<String, Any?>(
                    "name" to user.name,
                    "activeMode" to user.activeMode,
                    "emergencyContact" to user.emergencyContact
                )
                if (user.photoUrl.isNotBlank()) {
                    updates["photoUrl"] = user.photoUrl
                }
                docRef.update(updates).await()
            } else {
                // Initial creation with all base fields
                docRef.set(user.toMap()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateActiveMode(uid: String, activeMode: String): Result<Unit> {
        return try {
            usersCollection.document(uid).update("activeMode", activeMode).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateEmergencyContact(uid: String, emergencyContact: String): Result<Unit> {
        return try {
            usersCollection.document(uid).update("emergencyContact", emergencyContact).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserName(uid: String, name: String): Result<Unit> {
        return try {
            usersCollection.document(uid).update("name", name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAvailability(uid: String, available: Boolean, radiusKm: Int): Result<Unit> {
        return try {
            usersCollection.document(uid).update(
                mapOf(
                    "available" to available,
                    "radiusKm" to radiusKm
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
