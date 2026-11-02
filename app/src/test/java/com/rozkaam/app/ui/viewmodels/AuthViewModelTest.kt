package com.rozkaam.app.ui.viewmodels

import com.rozkaam.app.fakes.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
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
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        viewModel = AuthViewModel(authRepository = fakeAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testPhoneNumberInput_FiltersNonDigitsAndLimitsTo10() {
        viewModel.onPhoneNumberChange("987abc654-321099")
        val state = viewModel.uiState.value
        assertEquals("9876543210", state.phoneNumber)
        assertNull(state.errorMessage)
    }

    @Test
    fun testOtpInput_FiltersNonDigitsAndLimitsTo6() {
        viewModel.onOtpChange("12a34b5678")
        val state = viewModel.uiState.value
        assertEquals("123456", state.otpCode)
        assertNull(state.errorMessage)
    }

    @Test
    fun testResetToPhoneInput_ClearsOtpAndCodeSentFlag() {
        viewModel.onPhoneNumberChange("9876543210")
        viewModel.onOtpChange("123456")
        viewModel.resetToPhoneInput()

        val state = viewModel.uiState.value
        assertFalse(state.isCodeSent)
        assertEquals("", state.otpCode)
        assertNull(state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun testVerifyOtp_WithoutCodeSent_ShowsErrorMessage() {
        viewModel.verifyOtp()
        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("request an OTP first", ignoreCase = true))
    }

    @Test
    fun testClearError_ClearsErrorMessage() {
        viewModel.verifyOtp()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}

