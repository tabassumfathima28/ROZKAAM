package com.rozkaam.app.ui.viewmodels

import com.rozkaam.app.data.model.Task
import com.rozkaam.app.fakes.FakeAuthRepository
import com.rozkaam.app.fakes.FakeTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkerTasksViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var taskRepository: FakeTaskRepository
    private lateinit var viewModel: WorkerTasksViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        taskRepository = FakeTaskRepository(
            initialTasks = listOf(
                Task(
                    id = "accepted_task",
                    title = "Accepted task",
                    posterId = "poster_1",
                    workerId = "worker_1",
                    status = Task.STATUS_ACCEPTED
                ),
                Task(
                    id = "started_task",
                    title = "Started task",
                    posterId = "poster_2",
                    workerId = "worker_1",
                    status = Task.STATUS_STARTED
                ),
                Task(
                    id = "submitted_task",
                    title = "Submitted task",
                    posterId = "poster_3",
                    workerId = "worker_1",
                    status = Task.STATUS_SUBMITTED
                ),
                Task(
                    id = "completed_task",
                    title = "Completed task",
                    posterId = "poster_4",
                    workerId = "worker_1",
                    status = Task.STATUS_COMPLETED
                ),
                Task(
                    id = "cancelled_task",
                    title = "Cancelled assigned task",
                    posterId = "poster_cancelled",
                    workerId = "worker_1",
                    status = Task.STATUS_CANCELLED
                ),
                Task(
                    id = "other_worker_task",
                    title = "Another worker's task",
                    posterId = "poster_5",
                    workerId = "worker_2",
                    status = Task.STATUS_ACCEPTED
                ),
                Task(
                    id = "available_task",
                    title = "Available nearby task",
                    posterId = "poster_6",
                    status = Task.STATUS_OPEN,
                    lat = 12.9716,
                    lng = 77.5946
                )
            )
        )
        viewModel = WorkerTasksViewModel(
            taskRepository = taskRepository,
            authRepository = FakeAuthRepository(fakeUserId = "worker_1")
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun observeTasks_loadsOnlyAssignedWorkerTasksAndGroupsByStatus() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(
            setOf("accepted_task", "started_task", "submitted_task", "completed_task", "cancelled_task"),
            state.tasks.map { it.id }.toSet()
        )
        assertEquals(
            setOf("accepted_task", "started_task", "submitted_task"),
            state.activeTasks.map { it.id }.toSet()
        )
        assertEquals(listOf("completed_task"), state.completedTasks.map { it.id })
        assertEquals(listOf("cancelled_task"), state.cancelledTasks.map { it.id })
        assertEquals(state.activeTasks, state.filteredTasks)
    }

    @Test
    fun selectingCompletedShowsOnlyCompletedWorkerTasks() = runTest {
        advanceUntilIdle()

        viewModel.setSelectedTab(WorkerTasksUiState.TAB_COMPLETED)

        assertEquals(WorkerTasksUiState.TAB_COMPLETED, viewModel.uiState.value.selectedTab)
        assertEquals(listOf("completed_task"), viewModel.uiState.value.filteredTasks.map { it.id })
    }

    @Test
    fun cancelledTasksRemainAccessibleWithoutInflatingActiveOrCompletedCounts() = runTest {
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.activeTasks.size)
        assertEquals(1, viewModel.uiState.value.completedTasks.size)
        assertEquals(1, viewModel.uiState.value.cancelledTasks.size)

        viewModel.setSelectedTab(WorkerTasksUiState.TAB_CANCELLED)
        assertEquals(listOf("cancelled_task"), viewModel.uiState.value.filteredTasks.map { it.id })
    }

    @Test
    fun observesTasksWhenAuthenticationBecomesAvailableAndRefreshesLiveData() = runTest {
        val authRepository = FakeAuthRepository(fakeUserId = null)
        val delayedAuthViewModel = WorkerTasksViewModel(taskRepository, authRepository)
        advanceUntilIdle()

        assertTrue(delayedAuthViewModel.uiState.value.errorMessage?.contains("Sign in") == true)

        authRepository.updateAuthUserId("worker_1")
        advanceUntilIdle()
        assertEquals(5, delayedAuthViewModel.uiState.value.tasks.size)

        taskRepository.tasksFlow.value = taskRepository.tasksFlow.value + Task(
            id = "new_live_task",
            workerId = "worker_1",
            status = Task.STATUS_SUBMITTED
        )
        advanceUntilIdle()
        assertTrue(delayedAuthViewModel.uiState.value.activeTasks.any { it.id == "new_live_task" })
    }

    @Test
    fun acceptedTasksAreExcludedFromNearbyOpenTasks() = runTest {
        advanceUntilIdle()

        val nearby = taskRepository.getNearbyOpenTasks(
            workerLat = 12.9716,
            workerLng = 77.5946,
            radiusKm = 3.0,
            workerUid = "worker_1"
        ).first()

        assertEquals(listOf("available_task"), nearby.map { it.task.id })
    }
}
