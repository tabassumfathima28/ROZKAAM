package com.rozkaam.app.fakes

import com.rozkaam.app.data.model.PostTaskResult
import com.rozkaam.app.data.model.Task
import com.rozkaam.app.data.model.TaskCategory
import com.rozkaam.app.data.model.TaskSecret
import com.rozkaam.app.data.model.TaskWithDistance
import com.rozkaam.app.data.repository.TaskRepository
import com.rozkaam.app.data.repository.calculateDistanceMeters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeTaskRepository(
    initialCategories: List<TaskCategory> = listOf(
        TaskCategory("cleaning", "Cleaning", "cleaning"),
        TaskCategory("delivery", "Delivery", "delivery"),
        TaskCategory("moving", "Moving", "moving")
    ),
    initialTasks: List<Task> = emptyList()
) : TaskRepository {

    val categoriesFlow = MutableStateFlow(initialCategories)
    val tasksFlow = MutableStateFlow(initialTasks)
    val secretsMap = mutableMapOf<String, String>()

    var shouldFailPostTask: Boolean = false
    var shouldFailAcceptTask: Boolean = false
    var lastPostedParams: Map<String, Any?>? = null
    var lastAcceptedTaskId: String? = null

    override fun getCategories(): Flow<List<TaskCategory>> = categoriesFlow.asStateFlow()

    override fun getPosterTasks(posterId: String): Flow<List<Task>> =
        tasksFlow.map { list -> list.filter { it.posterId == posterId } }

    override fun getWorkerTasks(workerId: String): Flow<List<Task>> =
        tasksFlow.map { list -> list.filter { it.workerId == workerId } }

    override fun getTask(taskId: String): Flow<Task?> =
        tasksFlow.map { list -> list.find { it.id == taskId } }

    override fun getTaskSecret(taskId: String): Flow<String?> = MutableStateFlow(
        secretsMap[taskId] ?: "1234"
    ).asStateFlow()

    override fun getTaskSecretDetails(taskId: String): Flow<TaskSecret?> = MutableStateFlow(
        TaskSecret(taskId = taskId, otp = secretsMap[taskId] ?: "1234", qrToken = "qr_$taskId")
    ).asStateFlow()

    override fun getNearbyOpenTasks(
        workerLat: Double,
        workerLng: Double,
        radiusKm: Double,
        workerUid: String
    ): Flow<List<TaskWithDistance>> = tasksFlow.map { list ->
        val maxDist = radiusKm * 1000.0
        list.filter { it.status == Task.STATUS_OPEN && it.posterId != workerUid }
            .mapNotNull { t ->
                val dist = calculateDistanceMeters(workerLat, workerLng, t.lat, t.lng)
                if (dist <= maxDist) TaskWithDistance(t, dist) else null
            }
            .sortedBy { it.distanceMeters }
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
    ): Result<PostTaskResult> {
        lastPostedParams = mapOf(
            "title" to title,
            "description" to description,
            "category" to category,
            "price" to pricePaise,
            "lat" to lat,
            "lng" to lng,
            "address" to address,
            "scheduledFor" to scheduledFor
        )

        if (shouldFailPostTask) {
            return Result.failure(Exception("Failed to post task to backend"))
        }

        val taskId = "task_${System.currentTimeMillis()}"
        val createdTask = Task(
            id = taskId,
            title = title,
            description = description,
            category = category,
            price = pricePaise,
            lat = lat,
            lng = lng,
            address = address,
            scheduledFor = scheduledFor,
            status = Task.STATUS_OPEN
        )

        secretsMap[taskId] = "4321"
        tasksFlow.value = listOf(createdTask) + tasksFlow.value

        return Result.success(
            PostTaskResult(
                success = true,
                taskId = taskId,
                price = pricePaise,
                available = 200000L,
                held = 50000L
            )
        )
    }

    override suspend fun acceptTask(taskId: String): Result<Unit> {
        lastAcceptedTaskId = taskId
        if (shouldFailAcceptTask) {
            return Result.failure(Exception("Task is no longer open or acceptance failed"))
        }

        val updated = tasksFlow.value.map { t ->
            if (t.id == taskId) {
                if (t.status != Task.STATUS_OPEN) {
                    return Result.failure(Exception("Task is already accepted"))
                }
                t.copy(status = Task.STATUS_ACCEPTED, workerId = "test_worker")
            } else t
        }
        tasksFlow.value = updated
        return Result.success(Unit)
    }
}
