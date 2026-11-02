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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep {
    ROLE_SELECTION,
    PROFILE_SETUP
}

data class RoleSelectionUiState(
    val currentStep: OnboardingStep = OnboardingStep.ROLE_SELECTION,
    val selectedRole: String = User.MODE_WORKER,
    val fullName: String = "",
    val emergencyContact: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isComplete: Boolean = false
)

class RoleSelectionViewModel(
    private val userRepository: UserRepository = FirestoreUserRepository(),
    private val authRepository: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoleSelectionUiState())
    val uiState: StateFlow<RoleSelectionUiState> = _uiState.asStateFlow()

    fun onRoleChange(role: String) {
        _uiState.update { it.copy(selectedRole = role, errorMessage = null) }
    }

    fun goToProfileSetup() {
        _uiState.update { it.copy(currentStep = OnboardingStep.PROFILE_SETUP, errorMessage = null) }
    }

    fun goToRoleSelection() {
        _uiState.update { it.copy(currentStep = OnboardingStep.ROLE_SELECTION, errorMessage = null) }
    }

    fun onFullNameChange(name: String) {
        _uiState.update { it.copy(fullName = name, errorMessage = null) }
    }

    fun onEmergencyContactChange(contact: String) {
        val filtered = contact.filter { it.isDigit() || it == '+' || it == ' ' }.take(15)
        _uiState.update { it.copy(emergencyContact = filtered, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun saveProfile(onSuccess: (activeMode: String) -> Unit) {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "User session expired. Please sign in again."
                )
            }
            return
        }

        val name = _uiState.value.fullName.trim()
        val emergency = _uiState.value.emergencyContact.trim()
        val role = _uiState.value.selectedRole

        if (name.isBlank() || name.length < 2) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Please enter your full name (at least 2 characters)."
                )
            }
            return
        }

        val emergencyDigitsOnly = emergency.filter { it.isDigit() }
        if (emergencyDigitsOnly.length < 10) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Please enter a valid 10-digit emergency contact phone number."
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val user = User(
                uid = uid,
                name = name,
                phone = authRepository.currentUserPhone ?: "",
                photoUrl = authRepository.currentUserPhotoUrl ?: "",
                activeMode = role,
                emergencyContact = emergency,
                available = false,
                radiusKm = 5,
                dailyGoal = 50000L, // 50000 paise = ₹500
                rating = 0.0,
                ratingCount = 0,
                completedCount = 0,
                fcmToken = "",
                createdAt = System.currentTimeMillis()
            )

            val result = userRepository.saveUserProfile(user)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isComplete = true) }
                    onSuccess(role)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage
                                ?: "Failed to save profile to Firestore. Please check your connection."
                        )
                    }
                }
            )
        }
    }
}
