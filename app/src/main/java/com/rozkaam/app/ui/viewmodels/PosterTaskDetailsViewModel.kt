package com.rozkaam.app.ui.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozkaam.app.data.model.PublicProfile
import com.rozkaam.app.data.model.Task
import com.rozkaam.app.data.model.TaskSecret
import com.rozkaam.app.data.repository.FirebaseTaskProofRepository
import com.rozkaam.app.data.repository.FirebaseTaskWorkflowRepository
import com.rozkaam.app.data.repository.FirestorePublicProfileRepository
import com.rozkaam.app.data.repository.FirestoreTaskRepository
import com.rozkaam.app.data.repository.PublicProfileRepository
import com.rozkaam.app.data.repository.TaskProofRepository
import com.rozkaam.app.data.repository.TaskRepository
import com.rozkaam.app.data.repository.TaskWorkflowRepository
import com.rozkaam.app.utils.QrVerificationHelper
import com.rozkaam.app.utils.TaskQrPayload
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PosterTaskDetailsUiState(
    val isLoading: Boolean = true,
    val task: Task? = null,
    val arrivalOtp: String? = null,
    val qrToken: String? = null,
    val qrBitmap: Bitmap? = null,
    val isProofQr: Boolean = false,
    val isQrDialogVisible: Boolean = false,
    val isCallingContact: Boolean = false,
    val counterpartPhone: String? = null,
    val workerProfile: PublicProfile? = null,
    val proofImage: Bitmap? = null,
    val isActionLoading: Boolean = false,
    val actionSuccess: Boolean = false,
    val rating: Int = 5,
    val reviewComment: String = "",
    val errorMessage: String? = null
)

class PosterTaskDetailsViewModel(
    private val taskRepository: TaskRepository = FirestoreTaskRepository(),
    private val publicProfileRepository: PublicProfileRepository = FirestorePublicProfileRepository(),
    private val proofRepository: TaskProofRepository = FirebaseTaskProofRepository(),
    private val workflowRepository: TaskWorkflowRepository = FirebaseTaskWorkflowRepository(),
    private val qrBitmapGenerator: (String) -> Bitmap? = QrVerificationHelper::generateQrBitmap
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosterTaskDetailsUiState())
    val uiState: StateFlow<PosterTaskDetailsUiState> = _uiState.asStateFlow()
    private var loadedProofPath: String? = null
    private var observedSecretTaskId: String? = null
    private var taskSecretJob: Job? = null
    private var latestSecret: TaskSecret? = null

    fun loadTask(taskId: String) {
        if (taskId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, task = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            taskRepository.getTask(taskId)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load task details."
                        )
                    }
                }
                .collect { task ->
                    _uiState.update { it.copy(isLoading = false, task = task) }
                    if (task != null) {
                        observeArrivalOtp(task.id)
                        refreshQrBitmap(task)
                        task.workerId?.takeIf(String::isNotBlank)?.let(::loadWorkerProfile)
                        task.proofPath?.let(::loadProofImage)
                    }
                }
        }
    }

    fun approveTask() = runTaskAction { taskId -> workflowRepository.approveTask(taskId) }

    fun cancelTask() = runTaskAction { taskId -> workflowRepository.cancelTask(taskId) }

    fun setRating(rating: Int) {
        _uiState.update { it.copy(rating = rating.coerceIn(1, 5)) }
    }

    fun setReviewComment(comment: String) {
        _uiState.update { it.copy(reviewComment = comment.take(1000)) }
    }

    fun rateWorker() {
        val state = _uiState.value
        val taskId = state.task?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null, actionSuccess = false) }
            workflowRepository.rateWorker(taskId, state.rating, state.reviewComment.trim()).fold(
                onSuccess = {
                    _uiState.update { it.copy(isActionLoading = false, actionSuccess = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            errorMessage = error.localizedMessage ?: "Rating failed."
                        )
                    }
                }
            )
        }
    }

    private fun runTaskAction(action: suspend (String) -> Result<Unit>) {
        val taskId = _uiState.value.task?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null, actionSuccess = false) }
            action(taskId).fold(
                onSuccess = {
                    _uiState.update { it.copy(isActionLoading = false, actionSuccess = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            errorMessage = error.localizedMessage ?: "Task update failed."
                        )
                    }
                }
            )
        }
    }

    private fun loadProofImage(storagePath: String) {
        if (loadedProofPath == storagePath) return
        loadedProofPath = storagePath
        viewModelScope.launch {
            proofRepository.getProofImage(storagePath).fold(
                onSuccess = { image -> _uiState.update { it.copy(proofImage = image) } },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.localizedMessage ?: "Could not load proof image.")
                    }
                }
            )
        }
    }

    fun setQrDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isQrDialogVisible = visible) }
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
                            errorMessage = error.localizedMessage ?: "Could not get worker contact."
                        )
                    }
                }
            )
        }
    }

    private fun observeArrivalOtp(taskId: String) {
        if (observedSecretTaskId == taskId) return
        observedSecretTaskId = taskId
        latestSecret = null
        taskSecretJob?.cancel()
        taskSecretJob = viewModelScope.launch {
            taskRepository.getTaskSecretDetails(taskId)
                .catch { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.localizedMessage ?: "Could not load arrival code.")
                    }
                }
                .collect { secret ->
                    latestSecret = secret
                    val otp = secret?.otp
                    val qrToken = secret?.qrToken
                    _uiState.update {
                        it.copy(
                            arrivalOtp = otp,
                            qrToken = qrToken
                        )
                    }
                    refreshQrBitmap(_uiState.value.task)
                }
        }
    }

    private fun refreshQrBitmap(task: Task?) {
        val currentTask = task ?: return
        val secret = latestSecret ?: return
        val proofQr = currentTask.isSubmitted
        val payload = if (proofQr) {
            secret.proofQrToken?.let { token ->
                TaskQrPayload(
                    app = TaskQrPayload.APP_IDENTIFIER,
                    taskId = currentTask.id,
                    qrToken = token,
                    purpose = TaskQrPayload.PURPOSE_PROOF
                )
            }
        } else if (currentTask.isAccepted || currentTask.isStarted) {
            secret.otp?.takeIf(String::isNotBlank)?.let { otp ->
                TaskQrPayload(
                    app = TaskQrPayload.APP_IDENTIFIER,
                    taskId = currentTask.id,
                    otp = otp,
                    qrToken = secret.qrToken,
                    purpose = TaskQrPayload.PURPOSE_ARRIVAL
                )
            }
        } else {
            null
        }

        val bitmap = payload?.let { qrBitmapGenerator(it.toJsonString()) }
        _uiState.update { state ->
            if (state.task?.id == currentTask.id && state.task.status == currentTask.status) {
                state.copy(qrBitmap = bitmap, isProofQr = proofQr)
            } else {
                state
            }
        }
    }

    private fun loadWorkerProfile(workerId: String) {
        viewModelScope.launch {
            publicProfileRepository.observePublicProfile(workerId)
                .catch { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.localizedMessage ?: "Could not load worker details.")
                    }
                }
                .collect { profile ->
                    _uiState.update { it.copy(workerProfile = profile) }
                }
        }
    }
}
