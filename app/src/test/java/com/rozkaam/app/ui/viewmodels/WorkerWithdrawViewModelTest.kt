package com.rozkaam.app.ui.viewmodels

import com.rozkaam.app.data.model.Wallet
import com.rozkaam.app.data.model.WithdrawalRequest
import com.rozkaam.app.data.repository.TaskWorkflowRepository
import com.rozkaam.app.data.repository.WithdrawalRepository
import com.rozkaam.app.fakes.FakeAuthRepository
import com.rozkaam.app.fakes.FakeWalletRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkerWithdrawViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var walletRepository: FakeWalletRepository
    private lateinit var workflowRepository: RecordingWorkflowRepository
    private lateinit var viewModel: WorkerWithdrawViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        walletRepository = FakeWalletRepository(initialWallet = Wallet(available = 25000L))
        workflowRepository = RecordingWorkflowRepository()
        viewModel = WorkerWithdrawViewModel(
            walletRepository = walletRepository,
            workflowRepository = workflowRepository,
            authRepository = FakeAuthRepository(fakeUserId = "worker_1"),
            withdrawalRepository = object : WithdrawalRepository {
                override fun observeWithdrawals(uid: String) =
                    flowOf<List<WithdrawalRequest>>(emptyList())
            }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun requestWithdrawalSendsPaiseAndNormalizedUpi() = runTest {
        viewModel.setAmount("125.50")
        viewModel.setUpiId(" worker@bank ")
        viewModel.requestWithdrawal()
        advanceUntilIdle()

        assertEquals(12550L, workflowRepository.amountPaise)
        assertEquals("worker@bank", workflowRepository.upiId)
        assertEquals("withdrawal_1", viewModel.uiState.value.withdrawalId)
        assertEquals("processed", viewModel.uiState.value.withdrawalStatus)
        assertEquals(1, workflowRepository.processedWithdrawalCount)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun requestWithdrawalRejectsAmountsAboveAvailableBalance() = runTest {
        advanceUntilIdle()
        viewModel.setAmount("300")
        viewModel.setUpiId("worker@bank")
        viewModel.requestWithdrawal()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals(null, workflowRepository.amountPaise)
    }

    private class RecordingWorkflowRepository : TaskWorkflowRepository {
        var amountPaise: Long? = null
        var upiId: String? = null
        var processedWithdrawalCount = 0

        override suspend fun startTask(
            taskId: String,
            otp: String,
            lat: Double,
            lng: Double,
            qrToken: String?
        ) =
            Result.success(Unit)

        override suspend fun verifyTaskProofQr(taskId: String, scannedTaskId: String, qrToken: String) =
            Result.success(Unit)

        override suspend fun approveTask(taskId: String) = Result.success(Unit)
        override suspend fun cancelTask(taskId: String) = Result.success(Unit)
        override suspend fun rateWorker(taskId: String, stars: Int, comment: String) =
            Result.success(Unit)

        override suspend fun requestWithdrawal(amountPaise: Long, upiId: String): Result<String> {
            this.amountPaise = amountPaise
            this.upiId = upiId
            return Result.success("withdrawal_1")
        }

        override suspend fun processTestWithdrawal(withdrawalId: String): Result<Unit> {
            processedWithdrawalCount += 1
            return Result.success(Unit)
        }
    }
}
