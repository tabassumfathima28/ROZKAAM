package com.rozkaam.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.rozkaam.app.data.model.PostTaskResult
import com.rozkaam.app.data.model.Task
import com.rozkaam.app.data.model.TaskCategory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository interface for Task management and category configuration.
 * Adheres to Section A2, A6, and A7 of SPEC.md.
 */
interface TaskRepository {
    fun getCategories(): Flow<List<TaskCategory>>
    fun getPosterTasks(posterId: String): Flow<List<Task>>
    fun getTask(taskId: String): Flow<Task?>
    suspend fun postTask(
        title: String,
        description: String,
        category: String,
        pricePaise: Long,
        lat: Double,
        lng: Double,
        address: String,
        scheduledFor: String
    ): Result<PostTaskResult>
}

class FirestoreTaskRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) : TaskRepository {

    override fun getCategories(): Flow<List<TaskCategory>> = callbackFlow {
        val listenerRegistration = firestore.collection("config")
            .document("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data
                    val rawList = (data?.get("items") as? List<*>)
                        ?: (data?.get("categories") as? List<*>)
                        ?: (data?.get("list") as? List<*>)

                    if (rawList != null) {
                        val categories = rawList.mapNotNull { item ->
                            if (item is Map<*, *>) {
                                @Suppress("UNCHECKED_CAST")
                                TaskCategory.fromMap("", item as Map<String, Any?>)
                            } else null
                        }
                        trySend(categories)
                    } else if (data != null && data.isNotEmpty()) {
                        val categories = data.entries.mapNotNull { entry ->
                            val value = entry.value
                            if (value is Map<*, *>) {
                                @Suppress("UNCHECKED_CAST")
                                TaskCategory.fromMap(entry.key, value as Map<String, Any?>)
                            } else null
                        }
                        trySend(categories)
                    } else {
                        trySend(emptyList())
                    }
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    override fun getPosterTasks(posterId: String): Flow<List<Task>> = callbackFlow {
        if (posterId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listenerRegistration = firestore.collection("tasks")
            .whereEqualTo("posterId", posterId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        Task.fromMap(doc.id, doc.data)
                    }
                    trySend(list)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    override fun getTask(taskId: String): Flow<Task?> = callbackFlow {
        if (taskId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listenerRegistration = firestore.collection("tasks")
            .document(taskId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(Task.fromMap(snapshot.id, snapshot.data))
                } else {
                    trySend(null)
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun postTask(
        title: String,
        description: String,
        category: String,
        pricePaise: Long,
        lat: Double,
        lng: Double,
        address: String,
        scheduledFor: String
    ): Result<PostTaskResult> = runCatching {
        val data = hashMapOf(
            "title" to title,
            "description" to description,
            "category" to category,
            "price" to pricePaise,
            "lat" to lat,
            "lng" to lng,
            "address" to address,
            "scheduledFor" to scheduledFor
        )

        val result = functions
            .getHttpsCallable("postTask")
            .call(data)
            .await()

        @Suppress("UNCHECKED_CAST")
        val response = result.getData() as? Map<String, Any?>
            ?: throw IllegalStateException("Invalid response from postTask function.")

        val success = response["success"] as? Boolean ?: false
        val taskId = response["taskId"] as? String
            ?: throw IllegalStateException("Missing taskId in response.")
        val price = (response["price"] as? Number)?.toLong() ?: pricePaise
        val available = (response["available"] as? Number)?.toLong() ?: 0L
        val held = (response["held"] as? Number)?.toLong() ?: 0L

        PostTaskResult(
            success = success,
            taskId = taskId,
            price = price,
            available = available,
            held = held
        )
    }
}

