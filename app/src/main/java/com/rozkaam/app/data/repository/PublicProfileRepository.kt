package com.rozkaam.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.rozkaam.app.data.model.PublicProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface PublicProfileRepository {
    suspend fun getPublicProfile(uid: String): Result<PublicProfile?>
    fun observePublicProfile(uid: String): Flow<PublicProfile?>
}

class FirestorePublicProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : PublicProfileRepository {

    private val publicProfilesCollection = firestore.collection("publicProfiles")

    override suspend fun getPublicProfile(uid: String): Result<PublicProfile?> {
        return try {
            val doc = publicProfilesCollection.document(uid).get().await()
            if (doc.exists()) {
                val profile = PublicProfile.fromDocument(uid, doc.data)
                Result.success(profile)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observePublicProfile(uid: String): Flow<PublicProfile?> = callbackFlow {
        val registration = publicProfilesCollection.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val profile = PublicProfile.fromDocument(uid, snapshot.data)
                trySend(profile)
            } else {
                trySend(null)
            }
        }
        awaitClose {
            registration.remove()
        }
    }
}

