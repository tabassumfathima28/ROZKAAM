package com.rozkaam.app.ui.viewmodels

import com.rozkaam.app.data.model.PublicProfile
import com.rozkaam.app.data.model.Task
import com.rozkaam.app.data.repository.TaskProofRepository
import com.rozkaam.app.data.repository.TaskWorkflowRepository
import com.rozkaam.app.fakes.FakePublicProfileRepository
import com.rozkaam.app.fakes.FakeTaskRepository
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PosterTaskDetailsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeTaskRepo: FakeTaskRepository
    private lateinit var fakeProfileRepo: FakePublicProfileRepository
    private lateinit var viewModel: PosterTaskDetailsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeTaskRepo = FakeTaskRepository(
            initialTasks = listOf(
                Task(
                    id = "task_accepted_1",
                    title = "Clean office space",
                    posterId = "poster_1",
                    workerId = "worker_88",
                    status = Task.STATUS_ACCEPTED
                )
            )
        )
        fakeTaskRepo.secretsMap["task_accepted_1"] = "8765"
        fakeProfileRepo = FakePublicProfileRepository(
            initialProfiles = mapOf(
                "worker_88" to PublicProfile(
                    uid = "worker_88",
                    name = "Suresh Patel",
                    rating = 4.7,
                    completedCount = 8
                )
            )
        )
        viewModel = PosterTaskDetailsViewModel(
            taskRepository = fakeTaskRepo,
            publicProfileRepository = fakeProfileRepo,
            proofRepository = object : TaskProofRepository {
                override suspend fun getProofImage(
                    storagePath: String
                ): Result<android.graphics.Bitmap> =
                    Result.failure(UnsupportedOperationException("No proof image in this test."))

                override suspend fun submitProof(
                    taskId: String,
                    imageUri: android.net.Uri,
                    contentType: String,
                    description: String
                ): Result<Unit> = Result.success(Unit)
            },
            workflowRepository = object : TaskWorkflowRepository {
                override suspend fun startTask(
                    taskId: String,
                    otp: String,
                    lat: Double,
                    lng: Double,
                    qrToken: String?
                ): Result<Unit> = Result.success(Unit)

                override suspend fun verifyTaskProofQr(
                    taskId: String,
                    scannedTaskId: String,
                    qrToken: String
                ): Result<Unit> = Result.success(Unit)

                override suspend fun approveTask(taskId: String): Result<Unit> = Result.success(Unit)
                override suspend fun cancelTask(taskId: String): Result<Unit> = Result.success(Unit)
                override suspend fun rateWorker(
                    taskId: String,
                    stars: Int,
                    comment: String
                ): Result<Unit> = Result.success(Unit)

                override suspend fun requestWithdrawal(
                    amountPaise: Long,
                    upiId: String
                ): Result<String> = Result.success("withdrawal-test")

                override suspend fun processTestWithdrawal(withdrawalId: String): Result<Unit> =
                    Result.success(Unit)
            },
            qrBitmapGenerator = { null }
        )
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun loadTask_loadsTaskWorkerProfileAndArrivalOtp() = runTest {
        viewModel.loadTask("task_accepted_1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.task)
        assertEquals("task_accepted_1", state.task?.id)
        assertEquals("Clean office space", state.task?.title)
        assertEquals("Suresh Patel", state.workerProfile?.name)
        assertEquals("8765", state.arrivalOtp)
    }
}
