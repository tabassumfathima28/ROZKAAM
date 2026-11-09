package com.rozkaam.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozkaam.app.data.model.Wallet
import com.rozkaam.app.data.model.WithdrawalRequest
import com.rozkaam.app.data.repository.AuthRepository
import com.rozkaam.app.data.repository.FirebaseAuthRepository
import com.rozkaam.app.data.repository.FirestoreWithdrawalRepository
import com.rozkaam.app.data.repository.FirestoreWalletRepository
import com.rozkaam.app.data.repository.TaskWorkflowRepository
import com.rozkaam.app.data.repository.FirebaseTaskWorkflowRepository
import com.rozkaam.app.data.repository.WalletRepository
import com.rozkaam.app.data.repository.WithdrawalRepository
import com.rozkaam.app.BuildConfig
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkerWithdrawUiState(
    val isLoading: Boolean = true,
    val wallet: Wallet = Wallet(),
    val amount: String = "",
    val upiId: String = "",
    val isSubmitting: Boolean = false,
    val withdrawalId: String? = null,
    val withdrawalStatus: String? = null,
    val withdrawals: List<WithdrawalRequest> = emptyList(),
    val errorMessage: String? = null
)

class WorkerWithdrawViewModel(
    private val walletRepository: WalletRepository = FirestoreWalletRepository(),
    private val workflowRepository: TaskWorkflowRepository = FirebaseTaskWorkflowRepository(),
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val withdrawalRepository: WithdrawalRepository = FirestoreWithdrawalRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkerWithdrawUiState())
    val uiState: StateFlow<WorkerWithdrawUiState> = _uiState.asStateFlow()

    init {
        val uid = authRepository.currentUserId
        if (uid.isNullOrBlank()) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "Sign in to request a withdrawal.")
            }
        } else {
            walletRepository.getWallet(uid)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Could not load wallet balance."
                        )
                    }
                }
                .onEach { wallet ->
                    _uiState.update { it.copy(isLoading = false, wallet = wallet) }
                }
                .launchIn(viewModelScope)
            withdrawalRepository.observeWithdrawals(uid)
                .catch { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.localizedMessage ?: "Could not load withdrawal status.")
                    }
                }
                .onEach { requests ->
                    _uiState.update { state ->
                        val currentStatus = state.withdrawalId
                            ?.let { id -> requests.firstOrNull { it.id == id }?.status }
                        state.copy(
                            withdrawals = requests,
                            withdrawalStatus = currentStatus ?: state.withdrawalStatus
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    fun setAmount(value: String) {
        if (value.matches(Regex("^\\d{0,8}(\\.\\d{0,2})?$"))) {
            _uiState.update {
                it.copy(amount = value, errorMessage = null, withdrawalId = null, withdrawalStatus = null)
            }
        }
    }

    fun setUpiId(value: String) {
        _uiState.update {
            it.copy(
                upiId = value.take(256),
                errorMessage = null,
                withdrawalId = null,
                withdrawalStatus = null
            )
        }
    }

    fun requestWithdrawal() {
        val state = _uiState.value
        if (state.withdrawalId != null || state.isSubmitting) return
        val uid = authRepository.currentUserId
        if (uid.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Sign in to request a withdrawal.") }
            return
        }
        val amountPaise = try {
            BigDecimal(state.amount)
                .setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact()
        } catch (_: NumberFormatException) {
            null
        } catch (_: ArithmeticException) {
            null
        }
        if (amountPaise == null || amountPaise <= 0L) {
            _uiState.update { it.copy(errorMessage = "Enter a valid withdrawal amount.") }
            return
        }
        if (amountPaise > state.wallet.available) {
            _uiState.update { it.copy(errorMessage = "Amount exceeds your available balance.") }
            return
        }
        if (!Regex("^[A-Za-z0-9._-]{2,256}@[A-Za-z0-9]{2,64}$").matches(state.upiId.trim())) {
            _uiState.update { it.copy(errorMessage = "Enter a valid UPI ID.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, withdrawalId = null) }
            workflowRepository.requestWithdrawal(amountPaise, state.upiId.trim()).fold(
                onSuccess = { id ->
                    _uiState.update {
                        it.copy(isSubmitting = false, withdrawalId = id, withdrawalStatus = "pending")
                    }
                    if (BuildConfig.DEBUG) {
                        workflowRepository.processTestWithdrawal(id).fold(
                            onSuccess = {
                                _uiState.update { it.copy(withdrawalStatus = "processed") }
                            },
                            onFailure = { error ->
                                _uiState.update {
                                    it.copy(
                                        withdrawalStatus = "pending",
                                        errorMessage = "Request saved as pending, but test processing failed: ${error.localizedMessage ?: "unknown error"}"
                                    )
                                }
                            }
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = error.localizedMessage ?: "Withdrawal request failed."
                        )
                    }
                }
            )
        }
    }
}
