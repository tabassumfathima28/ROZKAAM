package com.rozkaam.app.ui.viewmodels

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.rozkaam.app.data.repository.AuthRepository
import com.rozkaam.app.data.repository.FirebaseAuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val phoneNumber: String = "",
    val otpCode: String = "",
    val isCodeSent: Boolean = false,
    val resendCooldownSeconds: Int = 0,
    val isLoading: Boolean = false,
    val isCheckingProfile: Boolean = false,
    val errorMessage: String? = null,
    val signedInUser: FirebaseUser? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var countdownJob: Job? = null

    fun onPhoneNumberChange(rawNumber: String) {
        val filtered = rawNumber.filter { it.isDigit() }.take(10)
        _uiState.update { it.copy(phoneNumber = filtered, errorMessage = null) }
    }

    fun onOtpChange(rawOtp: String) {
        val filtered = rawOtp.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(otpCode = filtered, errorMessage = null) }
        // Auto-verify when 6 digits are fully entered
        if (filtered.length == 6 && verificationId != null && !_uiState.value.isLoading && !_uiState.value.isCheckingProfile) {
            verifyOtp()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetToPhoneInput() {
        countdownJob?.cancel()
        _uiState.update {
            it.copy(
                isCodeSent = false,
                otpCode = "",
                errorMessage = null,
                isLoading = false,
                isCheckingProfile = false
            )
        }
    }

    fun resetLoginState() {
        countdownJob?.cancel()
        verificationId = null
        resendToken = null
        _uiState.value = AuthUiState()
    }

    fun sendOtp(activity: Activity) {
        val phone = _uiState.value.phoneNumber.trim()

        // Validation for Indian 10-digit mobile number starting with 6, 7, 8, or 9
        if (phone.length != 10 || !phone.matches(Regex("^[6-9]\\d{9}$"))) {
            _uiState.update {
                it.copy(errorMessage = "Please enter a valid 10-digit Indian mobile number starting with 6, 7, 8, or 9.")
            }
            return
        }

        val fullPhoneNumber = "+91$phone"
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Instant auto-retrieval on device
                viewModelScope.launch {
                    val result = authRepository.signInWithCredential(credential)
                    result.fold(
                        onSuccess = { user ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isCheckingProfile = true,
                                    signedInUser = user
                                )
                            }
                        },
                        onFailure = { error ->
                            val msg = when (error) {
                                is FirebaseTooManyRequestsException -> "Too many requests. Please try again later."
                                else -> error.localizedMessage ?: "Sign-in failed. Please try again."
                            }
                            _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                        }
                    )
                }
            }

            override fun onVerificationFailed(exception: FirebaseException) {
                val message = when (exception) {
                    is FirebaseTooManyRequestsException ->
                        "Too many requests. SMS quota reached or rate-limited. Please wait a few minutes."
                    is FirebaseAuthInvalidCredentialsException ->
                        "Invalid phone number entered. Please check and try again."
                    else ->
                        exception.localizedMessage ?: "Verification failed. Please check your connection and try again."
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = message) }
            }

            override fun onCodeSent(
                verId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                verificationId = verId
                resendToken = token
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isCodeSent = true,
                        errorMessage = null
                    )
                }
                startCountdown()
            }
        }

        authRepository.sendVerificationCode(
            activity = activity,
            phoneNumber = fullPhoneNumber,
            resendToken = resendToken,
            callbacks = callbacks
        )
    }

    fun resendOtp(activity: Activity) {
        if (_uiState.value.resendCooldownSeconds > 0 || _uiState.value.isLoading) return
        sendOtp(activity)
    }

    fun verifyOtp() {
        val verId = verificationId
        val code = _uiState.value.otpCode

        if (verId == null) {
            _uiState.update { it.copy(errorMessage = "Please request an OTP first.") }
            return
        }

        if (code.length != 6) {
            _uiState.update { it.copy(errorMessage = "Please enter the complete 6-digit OTP.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = authRepository.verifyOtp(verId, code)
            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isCheckingProfile = true,
                            signedInUser = user
                        )
                    }
                },
                onFailure = { error ->
                    val message = when (error) {
                        is FirebaseAuthInvalidCredentialsException ->
                            "Wrong 6-digit OTP entered. Please check the code and try again."
                        is FirebaseTooManyRequestsException ->
                            "Too many incorrect attempts. Please wait before trying again."
                        else ->
                            error.localizedMessage ?: "Verification failed. Please check your code and try again."
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                }
            )
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (sec in 30 downTo 0) {
                _uiState.update { it.copy(resendCooldownSeconds = sec) }
                delay(1000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
