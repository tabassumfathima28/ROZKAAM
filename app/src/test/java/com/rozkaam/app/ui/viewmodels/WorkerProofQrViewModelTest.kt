package com.rozkaam.app.ui.viewmodels

import com.rozkaam.app.data.repository.TaskWorkflowRepository
import com.rozkaam.app.utils.TaskQrPayload
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
class WorkerProofQrViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var workflowRepository: RecordingWorkflowRepository
    private lateinit var viewModel: WorkerProofQrViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        workflowRepository = RecordingWorkflowRepository()
        viewModel = WorkerProofQrViewModel(workflowRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun validProofQrIsVerifiedByBackendForCurrentTask() = runTest {
        viewModel.verifyScannedQr("task_1", proofPayload("task_1"))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isVerified)
        assertEquals("task_1", workflowRepository.taskId)
        assertEquals("task_1", workflowRepository.scannedTaskId)
        assertEquals("a".repeat(64), workflowRepository.qrToken)
    }

    @Test
    fun differentTaskQrIsRejectedWithoutCallingBackend() = runTest {
        viewModel.verifyScannedQr("task_1", proofPayload("task_2"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isVerified)
        assertEquals("This QR code belongs to a different task.", viewModel.uiState.value.errorMessage)
        assertEquals(null, workflowRepository.taskId)
    }

    private fun proofPayload(taskId: String) = TaskQrPayload(
        app = TaskQrPayload.APP_IDENTIFIER,
        taskId = taskId,
        qrToken = "a".repeat(64),
        purpose = TaskQrPayload.PURPOSE_PROOF
    )

    private class RecordingWorkflowRepository : TaskWorkflowRepository {
        var taskId: String? = null
        var scannedTaskId: String? = null
        var qrToken: String? = null

        override suspend fun startTask(
            taskId: String,
            otp: String,
            lat: Double,
            lng: Double,
            qrToken: String?
        ) = Result.success(Unit)

        override suspend fun verifyTaskProofQr(
            taskId: String,
            scannedTaskId: String,
            qrToken: String
        ): Result<Unit> {
            this.taskId = taskId
            this.scannedTaskId = scannedTaskId
            this.qrToken = qrToken
            return Result.success(Unit)
        }

        override suspend fun approveTask(taskId: String) = Result.success(Unit)
        override suspend fun cancelTask(taskId: String) = Result.success(Unit)
        override suspend fun rateWorker(taskId: String, stars: Int, comment: String) =
            Result.success(Unit)
        override suspend fun requestWithdrawal(amountPaise: Long, upiId: String) =
            Result.success("withdrawal-test")
        override suspend fun processTestWithdrawal(withdrawalId: String) = Result.success(Unit)
    }
}
