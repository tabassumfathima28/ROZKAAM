package com.rozkaam.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozkaam.app.data.model.User
import com.rozkaam.app.data.repository.AuthRepository
import com.rozkaam.app.data.repository.FirebaseAuthRepository
import com.rozkaam.app.data.repository.FirestoreUserRepository
import com.rozkaam.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isUpdatingMode: Boolean = false,
    val isUpdatingProfile: Boolean = false,
    val errorMessage: String? = null,
    val isSignedOut: Boolean = false
)

class ProfileViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val userRepository: UserRepository = FirestoreUserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _uiState.update { it.copy(isLoading = false, isSignedOut = true) }
            return
        }

        viewModelScope.launch {
            userRepository.observeUser(uid)
                .catch { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.localizedMessage) }
                }
                .collect { user ->
                    _uiState.update {
                        it.copy(
                            user = user,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun toggleMode(onModeChanged: (newMode: String) -> Unit) {
        val currentUser = _uiState.value.user ?: return
        val newMode = if (currentUser.activeMode == User.MODE_WORKER) User.MODE_POSTER else User.MODE_WORKER

        _uiState.update { it.copy(isUpdatingMode = true, errorMessage = null) }

        viewModelScope.launch {
            val result = userRepository.updateActiveMode(currentUser.uid, newMode)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isUpdatingMode = false,
                            user = it.user?.copy(activeMode = newMode)
                        )
                    }
                    onModeChanged(newMode)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isUpdatingMode = false,
                            errorMessage = "Failed to switch mode: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun updateName(newName: String) {
        val uid = authRepository.currentUserId ?: return
        val trimmed = newName.trim()
        if (trimmed.length < 2) {
            _uiState.update { it.copy(errorMessage = "Name must be at least 2 characters.") }
            return
        }

        _uiState.update { it.copy(isUpdatingProfile = true, errorMessage = null) }

        viewModelScope.launch {
            val result = userRepository.updateUserName(uid, trimmed)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isUpdatingProfile = false,
                            user = it.user?.copy(name = trimmed)
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isUpdatingProfile = false,
                            errorMessage = "Failed to update name: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun updateEmergencyContact(newContact: String) {
        val uid = authRepository.currentUserId ?: return
        val trimmed = newContact.trim()
        val digitsOnly = trimmed.filter { it.isDigit() }
        if (digitsOnly.length < 10) {
            _uiState.update { it.copy(errorMessage = "Emergency contact must be a valid 10-digit number.") }
            return
        }

        _uiState.update { it.copy(isUpdatingProfile = true, errorMessage = null) }

        viewModelScope.launch {
            val result = userRepository.updateEmergencyContact(uid, trimmed)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isUpdatingProfile = false,
                            user = it.user?.copy(emergencyContact = trimmed)
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isUpdatingProfile = false,
                            errorMessage = "Failed to update emergency contact: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun signOut(onComplete: () -> Unit) {
        authRepository.signOut()
        _uiState.update { it.copy(user = null, isSignedOut = true) }
        onComplete()
    }
}
