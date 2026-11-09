package com.rozkaam.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.rozkaam.app.data.model.WithdrawalRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface WithdrawalRepository {
    fun observeWithdrawals(uid: String): Flow<List<WithdrawalRequest>>
}

class FirestoreWithdrawalRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : WithdrawalRepository {
    override fun observeWithdrawals(uid: String): Flow<List<WithdrawalRequest>> = callbackFlow {
        require(uid.isNotBlank()) { "Sign in to view withdrawal requests." }
        val registration = firestore.collection("withdrawals")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val withdrawals = snapshot?.documents.orEmpty()
                    .map { WithdrawalRequest.fromMap(it.id, it.data) }
                    .sortedByDescending(WithdrawalRequest::createdAt)
                trySend(withdrawals)
            }
        awaitClose { registration.remove() }
    }
}
