package com.rozkaam.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozkaam.app.data.model.AppConfig
import com.rozkaam.app.data.model.TopUpOrderResult
import com.rozkaam.app.data.repository.AuthRepository
import com.rozkaam.app.data.repository.FirebaseAuthRepository
import com.rozkaam.app.data.repository.FirestoreWalletRepository
import com.rozkaam.app.data.repository.WalletRepository
import com.rozkaam.app.ui.payment.RazorpayPaymentBridge
import com.rozkaam.app.ui.payment.RazorpayResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddMoneyUiState(
    val isLoadingConfig: Boolean = true,
    val appConfig: AppConfig = AppConfig(),
    val selectedQuickAmount: Long? = 50000L, // default to first chip (₹500 in paise)
    val customAmountText: String = "",
    val isCreatingOrder: Boolean = false,
    val isVerifyingPayment: Boolean = false,
    val orderReadyForCheckout: TopUpOrderResult? = null,
    val paymentSuccess: Boolean = false,
    val successAmountPaise: Long = 0L,
    val successOrderId: String = "",
    val errorMessage: String? = null
) {
    /**
     * Resolves the effective amount to top up in paise:
     * Either from custom amount input (if entered) or selected quick chip.
     */
    val effectiveAmountPaise: Long
        get() {
            val custom = customAmountText.trim().toLongOrNull()
            return if (custom != null && custom > 0) {
                custom * 100 // Rupees to paise
            } else {
                selectedQuickAmount ?: 0L
            }
        }

    val effectiveAmountRupees: Long
        get() = effectiveAmountPaise / 100

    val isValidAmount: Boolean
        get() = effectiveAmountPaise in appConfig.minPrice..appConfig.maxPrice
}

class AddMoneyViewModel(
    private val walletRepository: WalletRepository = FirestoreWalletRepository(),
    private val authRepository: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMoneyUiState())
    val uiState: StateFlow<AddMoneyUiState> = _uiState.asStateFlow()

    init {
        loadConfig()
        listenToPaymentResults()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            walletRepository.getAppConfig()
                .catch {
                    _uiState.update { it.copy(isLoadingConfig = false) }
                }
                .collect { config ->
                    _uiState.update { current ->
                        current.copy(
                            isLoadingConfig = false,
                            appConfig = config,
                            selectedQuickAmount = current.selectedQuickAmount ?: config.quickAmounts.firstOrNull()
                        )
                    }
                }
        }
    }

    private fun listenToPaymentResults() {
        viewModelScope.launch {
            RazorpayPaymentBridge.paymentResult.collect { result ->
                when (result) {
                    is RazorpayResult.Success -> {
                        verifyPayment(
                            orderId = result.orderId,
                            paymentId = result.paymentId,
                            signature = result.signature
                        )
                    }
                    is RazorpayResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isCreatingOrder = false,
                                isVerifyingPayment = false,
                                orderReadyForCheckout = null,
                                errorMessage = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun selectQuickAmount(amountPaise: Long) {
        _uiState.update {
            it.copy(
                selectedQuickAmount = amountPaise,
                customAmountText = "",
                errorMessage = null
            )
        }
    }

    fun updateCustomAmount(input: String) {
        val sanitized = input.filter { it.isDigit() }
        _uiState.update {
            it.copy(
                customAmountText = sanitized,
                selectedQuickAmount = null,
                errorMessage = null
            )
        }
    }

    fun initiateTopUp() {
        val state = _uiState.value
        val amount = state.effectiveAmountPaise

        if (amount < state.appConfig.minPrice) {
            _uiState.update {
                it.copy(errorMessage = "Minimum top-up amount is ₹${state.appConfig.minPrice / 100}")
            }
            return
        }

        if (amount > state.appConfig.maxPrice) {
            _uiState.update {
                it.copy(errorMessage = "Maximum top-up amount is ₹${state.appConfig.maxPrice / 100}")
            }
            return
        }

        val uid = authRepository.currentUserId
        if (uid.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Please sign in to add money.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingOrder = true, errorMessage = null) }

            val result = walletRepository.createTopUpOrder(amount)
            result.onSuccess { order ->
                _uiState.update {
                    it.copy(
                        isCreatingOrder = false,
                        orderReadyForCheckout = order,
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isCreatingOrder = false,
                        errorMessage = err.localizedMessage ?: "Failed to create payment order."
                    )
                }
            }
        }
    }

    fun onOrderCheckoutLaunched() {
        _uiState.update { it.copy(orderReadyForCheckout = null) }
    }

    fun verifyPayment(orderId: String, paymentId: String, signature: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isVerifyingPayment = true,
                    errorMessage = null
                )
            }

            val result = walletRepository.verifyTopUp(orderId, paymentId, signature)
            result.onSuccess { verifyResult ->
                _uiState.update {
                    it.copy(
                        isVerifyingPayment = false,
                        paymentSuccess = true,
                        successAmountPaise = verifyResult.amount,
                        successOrderId = verifyResult.orderId,
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isVerifyingPayment = false,
                        errorMessage = err.localizedMessage ?: "Payment verification failed."
                    )
                }
            }
        }
    }

    fun handlePaymentError(code: Int, message: String) {
        _uiState.update {
            it.copy(
                isCreatingOrder = false,
                isVerifyingPayment = false,
                orderReadyForCheckout = null,
                errorMessage = message
            )
        }
    }

    fun resetState() {
        _uiState.update {
            it.copy(
                isCreatingOrder = false,
                isVerifyingPayment = false,
                orderReadyForCheckout = null,
                paymentSuccess = false,
                successAmountPaise = 0L,
                successOrderId = "",
                errorMessage = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
