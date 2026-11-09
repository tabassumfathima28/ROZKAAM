package com.rozkaam.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozkaam.app.data.model.AppNotification
import com.rozkaam.app.data.repository.AuthRepository
import com.rozkaam.app.data.repository.FirebaseAuthRepository
import com.rozkaam.app.data.repository.FirestoreNotificationRepository
import com.rozkaam.app.data.repository.NotificationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val notifications: List<AppNotification> = emptyList(),
    val errorMessage: String? = null
)

class NotificationsViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val notificationRepository: NotificationRepository = FirestoreNotificationRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()
    private var notificationsJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.observeAuthUserId()
                .distinctUntilChanged()
                .collect { uid ->
                    notificationsJob?.cancel()
                    if (uid.isNullOrBlank()) {
                        _uiState.value = NotificationsUiState(
                            isLoading = false,
                            errorMessage = "Sign in to view notifications."
                        )
                    } else {
                        observeNotifications(uid)
                    }
                }
        }
    }

    fun markAsRead(notificationId: String) {
        val uid = authRepository.currentUserId.orEmpty()
        if (uid.isBlank() || notificationId.isBlank()) return
        viewModelScope.launch {
            notificationRepository.markAsRead(uid, notificationId).fold(
                onSuccess = { },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.localizedMessage ?: "Could not update notification.")
                    }
                }
            )
        }
    }

    private fun observeNotifications(uid: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        notificationsJob = viewModelScope.launch {
            notificationRepository.observeNotifications(uid)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Could not load notifications."
                        )
                    }
                }
                .collect { notifications ->
                    _uiState.update {
                        it.copy(isLoading = false, notifications = notifications, errorMessage = null)
                    }
                }
        }
    }
}
