package com.rozkaam.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozkaam.app.data.repository.AuthRepository
import com.rozkaam.app.data.repository.FirebaseAuthRepository
import com.rozkaam.app.data.repository.FirestoreUserRepository
import com.rozkaam.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SosUiState(
    val isLoading: Boolean = true,
    val emergencyContact: String = "",
    val errorMessage: String? = null
)

class SosViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val userRepository: UserRepository = FirestoreUserRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(SosUiState())
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeAuthUserId()
                .distinctUntilChanged()
                .collectLatest { uid ->
                    if (uid.isNullOrBlank()) {
                        _uiState.value = SosUiState(
                            isLoading = false,
                            errorMessage = "Sign in to use emergency assistance."
                        )
                    } else {
                        userRepository.observeUser(uid)
                            .catch { error ->
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        errorMessage = error.localizedMessage
                                            ?: "Could not load your emergency contact."
                                    )
                                }
                            }
                            .collect { user ->
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        emergencyContact = user?.emergencyContact.orEmpty(),
                                        errorMessage = if (user == null) {
                                            "Set an emergency contact in your profile first."
                                        } else {
                                            null
                                        }
                                    )
                                }
                            }
                    }
                }
        }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }
}
