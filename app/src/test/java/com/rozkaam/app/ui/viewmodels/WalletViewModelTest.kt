package com.rozkaam.app.ui.viewmodels

import com.rozkaam.app.data.model.Transaction
import com.rozkaam.app.data.model.Wallet
import com.rozkaam.app.fakes.FakeAuthRepository
import com.rozkaam.app.fakes.FakeWalletRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WalletViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeWalletRepository: FakeWalletRepository
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var viewModel: WalletViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeWalletRepository = FakeWalletRepository(
            initialWallet = Wallet(available = 100000L, held = 20000L), // ₹1,000 available, ₹200 held
            initialTransactions = listOf(
                Transaction(
                    id = "tx_1",
                    uid = "user_123",
                    type = "topup",
                    amount = 100000L,
                    createdAt = 1700000000000L
                )
            )
        )
        fakeAuthRepository = FakeAuthRepository(fakeUserId = "user_123")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoading_emitsLiveWalletAndTransactions() = runTest {
        viewModel = WalletViewModel(
            walletRepository = fakeWalletRepository,
            authRepository = fakeAuthRepository
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(100000L, state.wallet.available)
        assertEquals(20000L, state.wallet.held)
        assertEquals("₹1000", state.wallet.formattedAvailable)
        assertEquals("₹200", state.wallet.formattedHeld)
        assertEquals(1, state.transactions.size)
        assertEquals("topup", state.transactions[0].type)
    }

    @Test
    fun testUnauthenticatedUser_showsAuthErrorMessage() = runTest {
        val unauthRepository = FakeAuthRepository(fakeUserId = null)
        viewModel = WalletViewModel(
            walletRepository = fakeWalletRepository,
            authRepository = unauthRepository
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("User is not authenticated.", state.errorMessage)
    }

    @Test
    fun testRealtimeWalletUpdate_reflectsInUiState() = runTest {
        viewModel = WalletViewModel(
            walletRepository = fakeWalletRepository,
            authRepository = fakeAuthRepository
        )
        advanceUntilIdle()

        // Simulate Cloud Function updating wallet balance
        fakeWalletRepository.walletFlow.value = Wallet(available = 350000L, held = 0L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(350000L, state.wallet.available)
        assertEquals("₹3500", state.wallet.formattedAvailable)
    }

    @Test
    fun testRealtimeTransactionAdded_reflectsInUiState() = runTest {
        viewModel = WalletViewModel(
            walletRepository = fakeWalletRepository,
            authRepository = fakeAuthRepository
        )
        advanceUntilIdle()

        val newTransactions = listOf(
            Transaction(id = "tx_2", uid = "user_123", type = "release", amount = 50000L),
            Transaction(id = "tx_1", uid = "user_123", type = "topup", amount = 100000L)
        )
        fakeWalletRepository.transactionsFlow.value = newTransactions
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.transactions.size)
        assertEquals("release", viewModel.uiState.value.transactions[0].type)
    }

    @Test
    fun testClearError_resetsErrorMessage() = runTest {
        val unauthRepository = FakeAuthRepository(fakeUserId = null)
        viewModel = WalletViewModel(
            walletRepository = fakeWalletRepository,
            authRepository = unauthRepository
        )
        advanceUntilIdle()

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}

