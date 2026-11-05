package com.rozkaam.app.fakes

import com.rozkaam.app.data.model.PostTaskResult
import com.rozkaam.app.data.model.Task
import com.rozkaam.app.data.model.TaskCategory
import com.rozkaam.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    var shouldFailPostTask: Boolean = false
    var lastPostedParams: Map<String, Any?>? = null

    override fun getCategories(): Flow<List<TaskCategory>> = categoriesFlow.asStateFlow()

    override fun getPosterTasks(posterId: String): Flow<List<Task>> = tasksFlow.asStateFlow()

    override fun getTask(taskId: String): Flow<Task?> = MutableStateFlow(
        tasksFlow.value.find { it.id == taskId }
    ).asStateFlow()

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
}

