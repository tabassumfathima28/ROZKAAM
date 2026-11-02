package com.rozkaam.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozkaam.app.data.model.Transaction
import com.rozkaam.app.data.model.Wallet
import com.rozkaam.app.data.repository.AuthRepository
import com.rozkaam.app.data.repository.FirebaseAuthRepository
import com.rozkaam.app.data.repository.FirestoreWalletRepository
import com.rozkaam.app.data.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WalletUiState(
    val isLoading: Boolean = true,
    val wallet: Wallet = Wallet(),
    val transactions: List<Transaction> = emptyList(),
    val errorMessage: String? = null
)

class WalletViewModel(
    private val walletRepository: WalletRepository = FirestoreWalletRepository(),
    private val authRepository: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    init {
        observeWalletAndTransactions()
    }

    fun observeWalletAndTransactions() {
        val uid = authRepository.currentUserId
        if (uid.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "User is not authenticated."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            combine(
                walletRepository.getWallet(uid),
                walletRepository.getTransactions(uid)
            ) { wallet, transactions ->
                WalletUiState(
                    isLoading = false,
                    wallet = wallet,
                    transactions = transactions,
                    errorMessage = null
                )
            }.catch { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "Failed to load wallet data."
                    )
                }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
