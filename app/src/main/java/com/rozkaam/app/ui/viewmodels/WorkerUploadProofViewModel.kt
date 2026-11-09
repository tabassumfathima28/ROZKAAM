package com.rozkaam.app.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozkaam.app.data.model.Task
import com.rozkaam.app.data.repository.AuthRepository
import com.rozkaam.app.data.repository.FirebaseAuthRepository
import com.rozkaam.app.data.repository.FirebaseTaskProofRepository
import com.rozkaam.app.data.repository.FirestoreTaskRepository
import com.rozkaam.app.data.repository.TaskProofRepository
import com.rozkaam.app.data.repository.TaskRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkerUploadProofUiState(
    val isLoading: Boolean = true,
    val task: Task? = null,
    val isAuthorized: Boolean = false,
    val selectedImage: Uri? = null,
    val selectedContentType: String? = null,
    val description: String = "",
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val shouldNavigateToScanner: Boolean = false,
    val errorMessage: String? = null
) {
    val canSubmit: Boolean
        get() = task?.isStarted == true &&
            selectedImage != null &&
            selectedContentType in setOf("image/jpeg", "image/png") &&
            description.trim().length in 5..500 &&
            !isSubmitting &&
            !isSubmitted
}

class WorkerUploadProofViewModel(
    private val taskRepository: TaskRepository = FirestoreTaskRepository(),
    private val proofRepository: TaskProofRepository = FirebaseTaskProofRepository(),
    private val authRepository: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkerUploadProofUiState())
    val uiState: StateFlow<WorkerUploadProofUiState> = _uiState.asStateFlow()
    private var taskJob: Job? = null

    fun loadTask(taskId: String) {
        taskJob?.cancel()
        if (taskId.isBlank()) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "Task ID is missing.")
            }
            return
        }

        taskJob = viewModelScope.launch {
            _uiState.update {
                WorkerUploadProofUiState(isLoading = true)
            }
            taskRepository.getTask(taskId)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load task."
                        )
                    }
                }
                .collect { task ->
                    val workerUid = authRepository.currentUserId.orEmpty()
                    when {
                        task == null -> _uiState.update {
                            it.copy(isLoading = false, errorMessage = "Task not found.")
                        }
                        task.workerId != workerUid || workerUid.isBlank() -> _uiState.update {
                            it.copy(
                                isLoading = false,
                                task = task,
                                isAuthorized = false,
                                errorMessage = "Only the worker assigned to this task can submit proof."
                            )
                        }
                        task.isSubmitted || task.isCompleted -> _uiState.update {
                            it.copy(isLoading = false, task = task, isAuthorized = true, isSubmitted = true)
                        }
                        !task.isAccepted && !task.isStarted -> _uiState.update {
                            it.copy(
                                isLoading = false,
                                task = task,
                                isAuthorized = true,
                                errorMessage = "This task is not awaiting proof."
                            )
                        }
                        else -> _uiState.update {
                            it.copy(
                                isLoading = false,
                                task = task,
                                isAuthorized = true,
                                errorMessage = null
                            )
                        }
                    }
                }
        }
    }

    fun selectImage(uri: Uri?, contentType: String?) {
        _uiState.update {
            it.copy(
                selectedImage = uri,
                selectedContentType = contentType,
                errorMessage = if (uri != null && contentType !in setOf("image/jpeg", "image/png")) {
                    "Choose a JPEG or PNG photo."
                } else {
                    null
                }
            )
        }
    }

    fun updateDescription(description: String) {
        _uiState.update {
            it.copy(description = description.take(500), errorMessage = null)
        }
    }

    fun submitProof() {
        val state = _uiState.value
        if (state.isSubmitting || state.isSubmitted) return
        if (state.task?.isStarted != true) {
            _uiState.update { it.copy(errorMessage = "Verify your arrival before submitting proof.") }
            return
        }
        if (state.task.workerId != authRepository.currentUserId) {
            _uiState.update {
                it.copy(errorMessage = "Only the worker assigned to this task can submit proof.")
            }
            return
        }
        if (state.selectedImage == null) {
            _uiState.update { it.copy(errorMessage = "Choose a photo of the completed task.") }
            return
        }
        val photoContentType = state.selectedContentType
            ?.takeIf { it == "image/jpeg" || it == "image/png" }
        if (photoContentType == null) {
            _uiState.update { it.copy(errorMessage = "Choose a JPEG or PNG photo.") }
            return
        }
        val description = state.description.trim()
        if (description.length !in 5..500) {
            _uiState.update {
                it.copy(errorMessage = "Proof description must be between 5 and 500 characters.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            proofRepository.submitProof(
                taskId = state.task.id,
                imageUri = state.selectedImage,
                contentType = photoContentType,
                description = description
            ).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isSubmitted = true,
                            shouldNavigateToScanner = true
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = error.localizedMessage ?: "Failed to submit proof."
                        )
                    }
                }
            )
        }
    }

    fun onScannerNavigationStarted() {
        _uiState.update { it.copy(shouldNavigateToScanner = false) }
    }
}
