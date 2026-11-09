package com.rozkaam.app.ui.viewmodels

import com.rozkaam.app.data.model.Task
import com.rozkaam.app.data.model.User
import com.rozkaam.app.fakes.FakeAuthRepository
import com.rozkaam.app.fakes.FakeTaskRepository
import com.rozkaam.app.fakes.FakeUserRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkerHomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeUserRepo: FakeUserRepository
    private lateinit var fakeTaskRepo: FakeTaskRepository
    private lateinit var fakeAuthRepo: FakeAuthRepository
    private lateinit var viewModel: WorkerHomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeUserRepo = FakeUserRepository(
            initialUser = User(
                uid = "worker_1",
                name = "Ramesh Kumar",
                activeMode = User.MODE_WORKER,
                available = false,
                radiusKm = 5
            )
        )
        fakeTaskRepo = FakeTaskRepository(
            initialTasks = listOf(
                Task(
                    id = "task_near",
                    title = "Help clean backyard",
                    posterId = "poster_99",
                    price = 30000L,
                    lat = 12.9720,
                    lng = 77.5950,
                    status = Task.STATUS_OPEN
                ),
                Task(
                    id = "task_far",
                    title = "Paint fence",
                    posterId = "poster_88",
                    price = 50000L,
                    lat = 13.5000, // Very far away (~60km)
                    lng = 77.5950,
                    status = Task.STATUS_OPEN
                ),
                Task(
                    id = "task_own",
                    title = "My own task",
                    posterId = "worker_1", // Same user as worker
                    price = 20000L,
                    lat = 12.9720,
                    lng = 77.5950,
                    status = Task.STATUS_OPEN
                )
            )
        )
        fakeAuthRepo = FakeAuthRepository(fakeUserId = "worker_1")
        viewModel = WorkerHomeViewModel(
            userRepository = fakeUserRepo,
            taskRepository = fakeTaskRepo,
            authRepository = fakeAuthRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsUserProfileAndAvailability() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Ramesh Kumar", state.user?.name)
        assertFalse(state.isFreeNow)
        assertEquals(5, state.selectedRadiusKm)
        assertFalse(state.isMapView)
    }

    @Test
    fun toggleAvailability_updatesUserRepoAndState() = runTest {
        advanceUntilIdle()

        viewModel.toggleAvailability(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isFreeNow)
        assertTrue(fakeUserRepo.savedUser?.available == true)

        viewModel.toggleAvailability(false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isFreeNow)
        assertFalse(fakeUserRepo.savedUser?.available == true)
    }

    @Test
    fun setRadius_updatesRadiusInStateAndRepo() = runTest {
        advanceUntilIdle()

        viewModel.setRadius(10)
        advanceUntilIdle()

        assertEquals(10, viewModel.uiState.value.selectedRadiusKm)
        assertEquals(10, fakeUserRepo.savedUser?.radiusKm)
    }

    @Test
    fun updateWorkerLocation_filtersTasksByRadiusAndExcludesOwnTasks() = runTest {
        advanceUntilIdle()

        // Worker location near Bangalore MG Road (12.9716, 77.5946)
        viewModel.updateWorkerLocation(12.9716, 77.5946)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isLocationPermissionGranted)
        assertEquals(12.9716, state.workerLat)
        assertEquals(77.5946, state.workerLng)

        // Only task_near should be returned (task_far is >50km, task_own is by same worker)
        assertEquals(1, state.nearbyTasks.size)
        assertEquals("task_near", state.nearbyTasks.first().task.id)
        assertTrue(state.nearbyTasks.first().distanceMeters < 500.0)
    }

    @Test
    fun setMapView_togglesViewMode() = runTest {
        advanceUntilIdle()

        viewModel.setMapView(true)
        assertTrue(viewModel.uiState.value.isMapView)

        viewModel.setMapView(false)
        assertFalse(viewModel.uiState.value.isMapView)
    }
}
