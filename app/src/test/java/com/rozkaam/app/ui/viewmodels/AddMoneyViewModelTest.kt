package com.rozkaam.app.ui.viewmodels

import com.rozkaam.app.data.model.AppConfig
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddMoneyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeWalletRepository: FakeWalletRepository
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var viewModel: AddMoneyViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeWalletRepository = FakeWalletRepository()
        fakeAuthRepository = FakeAuthRepository(fakeUserId = "user_test_999")
        viewModel = AddMoneyViewModel(
            walletRepository = fakeWalletRepository,
            authRepository = fakeAuthRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDefaultState_loadsConfigAndSelectsFirstChip() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingConfig)
        assertEquals(listOf(50000L, 100000L, 200000L, 500000L), state.appConfig.quickAmounts)
        assertEquals(50000L, state.selectedQuickAmount)
        assertEquals(500L, state.effectiveAmountRupees)
        assertTrue(state.isValidAmount)
    }

    @Test
    fun testSelectQuickAmount_updatesEffectiveAmount() = runTest {
        advanceUntilIdle()

        viewModel.selectQuickAmount(100000L) // ₹1,000 in paise

        val state = viewModel.uiState.value
        assertEquals(100000L, state.selectedQuickAmount)
        assertEquals(1000L, state.effectiveAmountRupees)
        assertEquals("", state.customAmountText)
    }

    @Test
    fun testUpdateCustomAmount_overridesQuickAmount() = runTest {
        advanceUntilIdle()

        viewModel.updateCustomAmount("1500")

        val state = viewModel.uiState.value
        assertNull(state.selectedQuickAmount)
        assertEquals("1500", state.customAmountText)
        assertEquals(150000L, state.effectiveAmountPaise)
        assertEquals(1500L, state.effectiveAmountRupees)
    }

    @Test
    fun testInitiateTopUp_amountBelowMinimum_showsError() = runTest {
        advanceUntilIdle()

        // Minimum is ₹100 = 10000 paise. Enter ₹50
        viewModel.updateCustomAmount("50")
        viewModel.initiateTopUp()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Minimum top-up amount"))
        assertNull(state.orderReadyForCheckout)
    }

    @Test
    fun testInitiateTopUp_validAmount_createsOrderReadyForCheckout() = runTest {
        advanceUntilIdle()

        viewModel.selectQuickAmount(50000L)
        viewModel.initiateTopUp()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isCreatingOrder)
        assertNull(state.errorMessage)
        assertNotNull(state.orderReadyForCheckout)
        assertEquals(50000L, state.orderReadyForCheckout?.amount)
        assertEquals("INR", state.orderReadyForCheckout?.currency)
    }

    @Test
    fun testInitiateTopUp_whenOrderFails_showsError() = runTest {
        fakeWalletRepository.shouldFailOrderCreation = true
        advanceUntilIdle()

        viewModel.initiateTopUp()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isCreatingOrder)
        assertNotNull(state.errorMessage)
        assertNull(state.orderReadyForCheckout)
    }

    @Test
    fun testVerifyPayment_validSignature_marksPaymentSuccess() = runTest {
        advanceUntilIdle()

        viewModel.verifyPayment("order_123", "pay_456", "sig_789")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isVerifyingPayment)
        assertTrue(state.paymentSuccess)
        assertEquals("order_123", state.successOrderId)
        assertEquals(50000L, state.successAmountPaise)
        assertNull(state.errorMessage)
    }

    @Test
    fun testVerifyPayment_forgedSignature_showsVerificationError() = runTest {
        fakeWalletRepository.shouldFailVerification = true
        advanceUntilIdle()

        viewModel.verifyPayment("order_123", "pay_456", "forged_sig")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isVerifyingPayment)
        assertFalse(state.paymentSuccess)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Invalid signature"))
    }

    @Test
    fun testResetState_clearsAllFields() = runTest {
        advanceUntilIdle()

        viewModel.verifyPayment("order_123", "pay_456", "sig_789")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.paymentSuccess)

        viewModel.resetState()
        val state = viewModel.uiState.value
        assertFalse(state.paymentSuccess)
        assertEquals(0L, state.successAmountPaise)
        assertEquals("", state.successOrderId)
        assertNull(state.errorMessage)
    }
}

