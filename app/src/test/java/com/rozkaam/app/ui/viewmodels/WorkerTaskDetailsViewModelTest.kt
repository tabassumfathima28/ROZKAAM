package com.rozkaam.app.ui.viewmodels

import com.rozkaam.app.data.model.PublicProfile
import com.rozkaam.app.data.model.Task
import com.rozkaam.app.fakes.FakeAuthRepository
import com.rozkaam.app.fakes.FakePublicProfileRepository
import com.rozkaam.app.fakes.FakeTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkerTaskDetailsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeTaskRepo: FakeTaskRepository
    private lateinit var fakeProfileRepo: FakePublicProfileRepository
    private lateinit var fakeAuthRepo: FakeAuthRepository
    private lateinit var viewModel: WorkerTaskDetailsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeTaskRepo = FakeTaskRepository(
            initialTasks = listOf(
                Task(
                    id = "task_100",
                    title = "Fix water pipe",
                    description = "Kitchen sink pipe is leaking.",
                    category = "repair",
                    price = 35000L,
                    posterId = "poster_12",
                    status = Task.STATUS_OPEN
                )
            )
        )
        fakeProfileRepo = FakePublicProfileRepository(
            initialProfiles = mapOf(
                "poster_12" to PublicProfile(
                    uid = "poster_12",
                    name = "Priya Sharma",
                    rating = 4.9,
                    completedCount = 14
                )
            )
        )
        fakeAuthRepo = FakeAuthRepository(fakeUserId = "worker_1")
        viewModel = WorkerTaskDetailsViewModel(
            taskRepository = fakeTaskRepo,
            publicProfileRepository = fakeProfileRepo,
            authRepository = fakeAuthRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadTask_loadsTaskAndPosterProfile() = runTest {
        viewModel.loadTask("task_100")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.task)
        assertEquals("task_100", state.task?.id)
        assertEquals("Fix water pipe", state.task?.title)
        assertEquals("Priya Sharma", state.posterProfile?.name)
        assertEquals(4.9, state.posterProfile?.rating ?: 0.0, 0.01)
    }

    @Test
    fun acceptTask_success() = runTest {
        viewModel.loadTask("task_100")
        advanceUntilIdle()

        viewModel.acceptTask()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isAccepting)
        assertTrue(state.acceptSuccess)
        assertNull(state.errorMessage)
        assertEquals("task_100", fakeTaskRepo.lastAcceptedTaskId)
    }

    @Test
    fun acceptTask_failure_setsErrorMessage() = runTest {
        viewModel.loadTask("task_100")
        advanceUntilIdle()

        fakeTaskRepo.shouldFailAcceptTask = true
        viewModel.acceptTask()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isAccepting)
        assertFalse(state.acceptSuccess)
        assertNotNull(state.errorMessage)
    }
}
