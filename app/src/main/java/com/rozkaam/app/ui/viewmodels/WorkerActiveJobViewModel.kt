package com.rozkaam.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozkaam.app.data.model.Task
import com.rozkaam.app.data.repository.AuthRepository
import com.rozkaam.app.data.repository.FirebaseAuthRepository
import com.rozkaam.app.data.repository.FirebaseTaskWorkflowRepository
import com.rozkaam.app.data.repository.FirestoreTaskRepository
import com.rozkaam.app.data.repository.TaskRepository
import com.rozkaam.app.data.repository.TaskWorkflowRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.rozkaam.app.utils.TaskQrPayload
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkerActiveJobUiState(
    val isLoading: Boolean = true,
    val task: Task? = null,
    val otp: String = "",
    val qrToken: String? = null,
    val isScannerVisible: Boolean = false,
    val isCallingContact: Boolean = false,
    val counterpartPhone: String? = null,
    val isStarting: Boolean = false,
    val isStarted: Boolean = false,
    val errorMessage: String? = null
)

class WorkerActiveJobViewModel(
    private val taskRepository: TaskRepository = FirestoreTaskRepository(),
    private val workflowRepository: TaskWorkflowRepository = FirebaseTaskWorkflowRepository(),
    private val authRepository: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkerActiveJobUiState())
    val uiState: StateFlow<WorkerActiveJobUiState> = _uiState.asStateFlow()

    fun loadTask(taskId: String) {
        if (taskId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Task ID is missing.") }
            return
        }
        viewModelScope.launch {
            taskRepository.getTask(taskId)
                .catch { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.localizedMessage ?: "Failed to load task.")
                    }
                }
                .collect { task ->
                    val isWorker = task?.workerId == authRepository.currentUserId
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            task = task,
                            isStarted = task?.isStarted == true,
                            errorMessage = when {
                                task == null -> "Task not found."
                                !isWorker -> "Only the assigned worker can start this task."
                                !task.isAccepted && !task.isStarted -> "This task is not awaiting arrival verification."
                                else -> null
                            }
                        )
                    }
                }
        }
    }

    fun updateOtp(value: String) {
        _uiState.update { it.copy(otp = value.filter(Char::isDigit).take(4), errorMessage = null) }
    }

    fun setScannerVisible(visible: Boolean) {
        _uiState.update { it.copy(isScannerVisible = visible, errorMessage = null) }
    }

    fun applyScannedQr(payload: TaskQrPayload, lat: Double?, lng: Double?) {
        val currentTask = _uiState.value.task ?: return
        if (payload.taskId != currentTask.id) {
            _uiState.update { it.copy(errorMessage = "This QR code belongs to a different task.") }
            return
        }
        _uiState.update {
            it.copy(
                otp = payload.otp,
                qrToken = payload.qrToken,
                isScannerVisible = false,
                errorMessage = null
            )
        }
        if (lat != null && lng != null) {
            startTask(lat, lng)
        }
    }

    fun fetchCounterpartContact(onSuccess: (String) -> Unit) {
        val taskId = _uiState.value.task?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCallingContact = true, errorMessage = null) }
            workflowRepository.getCounterpartContact(taskId).fold(
                onSuccess = { phone ->
                    _uiState.update { it.copy(isCallingContact = false, counterpartPhone = phone) }
                    onSuccess(phone)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isCallingContact = false,
                            errorMessage = error.localizedMessage ?: "Could not get poster contact."
                        )
                    }
                }
            )
        }
    }

    fun startTask(lat: Double?, lng: Double?) {
        val state = _uiState.value
        val task = state.task ?: return
        if (state.isStarting || state.isStarted) return
        if (state.otp.length != 4) {
            _uiState.update { it.copy(errorMessage = "Enter the 4-digit arrival OTP or scan the poster's QR code.") }
            return
        }
        if (lat == null || lng == null) {
            _uiState.update { it.copy(errorMessage = "Current location is required to verify arrival.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isStarting = true, errorMessage = null) }
            workflowRepository.startTask(task.id, state.otp, lat, lng, state.qrToken).fold(
                onSuccess = { _uiState.update { it.copy(isStarting = false, isStarted = true) } },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isStarting = false,
                            errorMessage = error.localizedMessage ?: "Could not verify arrival."
                        )
                    }
                }
            )
        }
    }
}
