package com.rozkaam.app.ui.viewmodels

import com.rozkaam.app.data.model.Task
import com.rozkaam.app.fakes.FakeAuthRepository
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PosterTasksViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeTaskRepo: FakeTaskRepository
    private lateinit var fakeAuthRepo: FakeAuthRepository
    private lateinit var viewModel: PosterTasksViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeTaskRepo = FakeTaskRepository(
            initialTasks = listOf(
                Task(id = "t_1", title = "Task 1", posterId = "poster_1", status = Task.STATUS_OPEN),
                Task(id = "t_2", title = "Task 2", posterId = "poster_1", status = Task.STATUS_OPEN),
                Task(id = "t_3", title = "Task 3", posterId = "poster_1", status = Task.STATUS_ACCEPTED),
                Task(id = "t_4", title = "Task 4", posterId = "poster_1", status = Task.STATUS_SUBMITTED),
                Task(id = "t_5", title = "Task 5", posterId = "poster_1", status = Task.STATUS_COMPLETED),
                Task(id = "t_other", title = "Other poster", posterId = "poster_2", status = Task.STATUS_OPEN)
            )
        )
        fakeAuthRepo = FakeAuthRepository(fakeUserId = "poster_1")
        viewModel = PosterTasksViewModel(
            taskRepository = fakeTaskRepo,
            authRepository = fakeAuthRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun observeTasks_loadsPosterOnlyAndComputesCounts() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(5, state.tasks.size) // excludes t_other
        assertEquals(2, state.openCount)
        assertEquals(1, state.activeCount)
        assertEquals(1, state.awaitingApprovalCount)
        assertEquals(1, state.completedCount)
    }

    @Test
    fun setFilter_updatesFilteredTasks() = runTest {
        advanceUntilIdle()

        // Default filter is "Open"
        assertEquals("Open", viewModel.uiState.value.selectedFilter)
        assertEquals(2, viewModel.uiState.value.filteredTasks.size)

        // Switch to "Active"
        viewModel.setFilter("Active")
        assertEquals(1, viewModel.uiState.value.filteredTasks.size)
        assertEquals("t_3", viewModel.uiState.value.filteredTasks.first().id)

        // Switch to "Awaiting approval"
        viewModel.setFilter("Awaiting approval")
        assertEquals(1, viewModel.uiState.value.filteredTasks.size)
        assertEquals("t_4", viewModel.uiState.value.filteredTasks.first().id)

        // Switch to "Completed"
        viewModel.setFilter("Completed")
        assertEquals(1, viewModel.uiState.value.filteredTasks.size)
        assertEquals("t_5", viewModel.uiState.value.filteredTasks.first().id)
    }
}

