package com.rozkaam.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.rozkaam.app.data.model.PostTaskResult
import com.rozkaam.app.data.model.Task
import com.rozkaam.app.data.model.TaskCategory
import com.rozkaam.app.data.model.TaskSecret
import com.rozkaam.app.data.model.TaskWithDistance
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Calculates great-circle distance between two points on Earth using Haversine formula.
 */
fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0 // Earth radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

/**
 * Repository interface for Task management, nearby discovery, acceptance, and category configuration.
 * Adheres to Section A2, A5, A6, A7, A8, and A10 of SPEC.md.
 */
interface TaskRepository {
    fun getCategories(): Flow<List<TaskCategory>>
    fun getPosterTasks(posterId: String): Flow<List<Task>>
    fun getWorkerTasks(workerId: String): Flow<List<Task>>
    fun getTask(taskId: String): Flow<Task?>
    fun getTaskSecret(taskId: String): Flow<String?>
    fun getTaskSecretDetails(taskId: String): Flow<TaskSecret?>
    fun getNearbyOpenTasks(
        workerLat: Double,
        workerLng: Double,
        radiusKm: Double,
        workerUid: String
    ): Flow<List<TaskWithDistance>>
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
    suspend fun acceptTask(taskId: String): Result<Unit>
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
                    android.util.Log.w("FirestoreTaskRepo", "Failed to load categories from Firestore: ${error.message}. Using bundled fallback.")
                    trySend(TaskCategory.DEFAULT_CATEGORIES)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data
                    val rawList = (data?.get("items") as? List<*>)
                        ?: (data?.get("categories") as? List<*>)
                        ?: (data?.get("list") as? List<*>)

                    val categories = if (rawList != null) {
                        rawList.mapNotNull { item ->
                            if (item is Map<*, *>) {
                                @Suppress("UNCHECKED_CAST")
                                TaskCategory.fromMap("", item as Map<String, Any?>)
                            } else null
                        }
                    } else if (data != null && data.isNotEmpty()) {
                        data.entries.mapNotNull { entry ->
                            val value = entry.value
                            if (value is Map<*, *>) {
                                @Suppress("UNCHECKED_CAST")
                                TaskCategory.fromMap(entry.key, value as Map<String, Any?>)
                            } else null
                        }
                    } else {
                        emptyList()
                    }

                    if (categories.isNotEmpty()) {
                        trySend(categories)
                    } else {
                        trySend(TaskCategory.DEFAULT_CATEGORIES)
                    }
                } else {
                    trySend(TaskCategory.DEFAULT_CATEGORIES)
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
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        Task.fromMap(doc.id, doc.data)
                    }.sortedByDescending { task -> task.createdAt?.toDate()?.time ?: 0L }
                    trySend(list)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    override fun getWorkerTasks(workerId: String): Flow<List<Task>> = callbackFlow {
        if (workerId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listenerRegistration = firestore.collection("tasks")
            .whereEqualTo("workerId", workerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val tasks = snapshot.documents
                        .map { doc -> Task.fromMap(doc.id, doc.data) }
                        .sortedByDescending { task -> task.createdAt?.toDate()?.time ?: 0L }
                    trySend(tasks)
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

    override fun getTaskSecret(taskId: String): Flow<String?> = callbackFlow {
        if (taskId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listenerRegistration = firestore.collection("taskSecrets")
            .document(taskId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val otp = snapshot.getString("otp")
                    trySend(otp)
                } else {
                    trySend(null)
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    override fun getTaskSecretDetails(taskId: String): Flow<TaskSecret?> = callbackFlow {
        if (taskId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listenerRegistration = firestore.collection("taskSecrets")
            .document(taskId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val secret = TaskSecret(
                        taskId = taskId,
                        otp = snapshot.getString("otp"),
                        qrToken = snapshot.getString("qrToken"),
                        proofQrToken = snapshot.getString("proofQrToken"),
                        failedAttempts = (snapshot.getLong("failedAttempts") ?: 0L).toInt(),
                        locked = snapshot.getBoolean("locked") ?: false
                    )
                    trySend(secret)
                } else {
                    trySend(null)
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    override fun getNearbyOpenTasks(
        workerLat: Double,
        workerLng: Double,
        radiusKm: Double,
        workerUid: String
    ): Flow<List<TaskWithDistance>> = callbackFlow {
        val maxDistanceMeters = radiusKm * 1000.0

        val listenerRegistration = firestore.collection("tasks")
            .whereEqualTo("status", Task.STATUS_OPEN)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    var totalOpen = 0
                    var selfExcluded = 0
                    var distanceExcluded = 0
                    val tasks = mutableListOf<TaskWithDistance>()

                    for (doc in snapshot.documents) {
                        totalOpen++
                        val task = Task.fromMap(doc.id, doc.data)
                        // Exclude tasks created by the worker themselves (SPEC.md A7)
                        if (task.posterId == workerUid) {
                            selfExcluded++
                            continue
                        }

                        val dist = calculateDistanceMeters(workerLat, workerLng, task.lat, task.lng)
                        if (dist <= maxDistanceMeters) {
                            tasks.add(TaskWithDistance(task = task, distanceMeters = dist))
                        } else {
                            distanceExcluded++
                        }
                    }

                    tasks.sortBy { it.distanceMeters }
                    Log.d("TaskDiscovery", "Found $totalOpen open tasks ($selfExcluded self-excluded, $distanceExcluded out of radius ${radiusKm}km). Eligible: ${tasks.size}")
                    trySend(tasks)
                } else {
                    trySend(emptyList())
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

        val result = try {
            functions
                .getHttpsCallable("postTask")
                .call(data)
                .await()
        } catch (e: java.util.concurrent.ExecutionException) {
            throw (e.cause ?: e)
        }

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

    override suspend fun acceptTask(taskId: String): Result<Unit> = runCatching {
        val data = hashMapOf("taskId" to taskId)
        val result = try {
            functions
                .getHttpsCallable("acceptTask")
                .call(data)
                .await()
        } catch (e: java.util.concurrent.ExecutionException) {
            throw (e.cause ?: e)
        }

        @Suppress("UNCHECKED_CAST")
        val response = result.getData() as? Map<String, Any?>
            ?: throw IllegalStateException("Invalid response from acceptTask function.")

        val success = response["success"] as? Boolean ?: false
        if (!success) {
            throw IllegalStateException("Failed to accept task.")
        }
    }
}
