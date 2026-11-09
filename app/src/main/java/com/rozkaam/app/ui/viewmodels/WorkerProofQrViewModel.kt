package com.rozkaam.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozkaam.app.data.repository.FirebaseTaskWorkflowRepository
import com.rozkaam.app.data.repository.TaskWorkflowRepository
import com.rozkaam.app.utils.TaskQrPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkerProofQrUiState(
    val isVerifying: Boolean = false,
    val isVerified: Boolean = false,
    val errorMessage: String? = null
)

class WorkerProofQrViewModel(
    private val workflowRepository: TaskWorkflowRepository = FirebaseTaskWorkflowRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkerProofQrUiState())
    val uiState: StateFlow<WorkerProofQrUiState> = _uiState.asStateFlow()

    fun verifyScannedQr(taskId: String, payload: TaskQrPayload) {
        val state = _uiState.value
        if (state.isVerifying || state.isVerified) return
        if (taskId.isBlank() || payload.taskId != taskId) {
            _uiState.update { it.copy(errorMessage = "This QR code belongs to a different task.") }
            return
        }
        if (payload.purpose != TaskQrPayload.PURPOSE_PROOF) {
            _uiState.update { it.copy(errorMessage = "Scan the poster's proof verification QR code.") }
            return
        }
        val qrToken = payload.qrToken
            ?.takeIf { it.matches(Regex("^[0-9a-fA-F]{64}$")) }
        if (qrToken == null) {
            _uiState.update { it.copy(errorMessage = "This proof QR code is invalid.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true, errorMessage = null) }
            workflowRepository.verifyTaskProofQr(taskId, payload.taskId, qrToken).fold(
                onSuccess = {
                    _uiState.update { it.copy(isVerifying = false, isVerified = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isVerifying = false,
                            errorMessage = error.localizedMessage ?: "The task QR code could not be verified."
                        )
                    }
                }
            )
        }
    }
}
