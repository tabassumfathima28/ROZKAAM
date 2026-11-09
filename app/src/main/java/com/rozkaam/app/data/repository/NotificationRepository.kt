package com.rozkaam.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.rozkaam.app.data.model.AppNotification
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface NotificationRepository {
    fun observeNotifications(uid: String): Flow<List<AppNotification>>
    suspend fun markAsRead(uid: String, notificationId: String): Result<Unit>
}

class FirestoreNotificationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : NotificationRepository {
    override fun observeNotifications(uid: String): Flow<List<AppNotification>> = callbackFlow {
        require(uid.isNotBlank()) { "Sign in to view notifications." }
        val registration = firestore.collection("notifications")
            .document(uid)
            .collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val notifications = snapshot?.documents.orEmpty()
                    .map { AppNotification.fromMap(it.id, it.data) }
                    .sortedByDescending(AppNotification::createdAt)
                trySend(notifications)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun markAsRead(uid: String, notificationId: String): Result<Unit> =
        runCatching {
            require(uid.isNotBlank() && notificationId.isNotBlank()) {
                "A signed-in account and notification are required."
            }
            firestore.collection("notifications")
                .document(uid)
                .collection("items")
                .document(notificationId)
                .update("read", true)
                .await()
        }
}
