package com.rozkaam.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozkaam.app.data.model.PublicProfile
import com.rozkaam.app.data.model.Task
import com.rozkaam.app.data.repository.AuthRepository
import com.rozkaam.app.data.repository.FirebaseAuthRepository
import com.rozkaam.app.data.repository.FirestorePublicProfileRepository
import com.rozkaam.app.data.repository.FirestoreTaskRepository
import com.rozkaam.app.data.repository.PublicProfileRepository
import com.rozkaam.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkerTaskDetailsUiState(
    val isLoading: Boolean = true,
    val task: Task? = null,
    val posterProfile: PublicProfile? = null,
    val currentWorkerUid: String = "",
    val isAccepting: Boolean = false,
    val acceptSuccess: Boolean = false,
    val errorMessage: String? = null
)

class WorkerTaskDetailsViewModel(
    private val taskRepository: TaskRepository = FirestoreTaskRepository(),
    private val publicProfileRepository: PublicProfileRepository = FirestorePublicProfileRepository(),
    private val authRepository: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkerTaskDetailsUiState())
    val uiState: StateFlow<WorkerTaskDetailsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(currentWorkerUid = authRepository.currentUserId.orEmpty()) }
        viewModelScope.launch {
            authRepository.observeAuthState().collect { user ->
                _uiState.update {
                    it.copy(currentWorkerUid = user?.uid ?: authRepository.currentUserId.orEmpty())
                }
            }
        }
    }

    fun loadTask(taskId: String) {
        if (taskId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, task = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            taskRepository.getTask(taskId)
                .catch { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.localizedMessage ?: "Failed to load task details."
                        )
                    }
                }
                .collect { task ->
                    _uiState.update { it.copy(isLoading = false, task = task) }
                    if (task != null && task.posterId.isNotBlank()) {
                        loadPosterProfile(task.posterId)
                    }
                }
        }
    }

    private fun loadPosterProfile(posterId: String) {
        viewModelScope.launch {
            publicProfileRepository.observePublicProfile(posterId)
                .catch { /* Fall back to default profile */ }
                .collect { profile ->
                    _uiState.update { it.copy(posterProfile = profile) }
                }
        }
    }

    fun acceptTask() {
        val state = _uiState.value
        val task = state.task ?: return
        if (state.isAccepting || !task.isOpen || task.posterId == state.currentWorkerUid) return
        if (state.currentWorkerUid.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Sign in as a worker to accept this task.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAccepting = true, errorMessage = null) }
            val result = taskRepository.acceptTask(task.id)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isAccepting = false, acceptSuccess = true) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isAccepting = false,
                            errorMessage = err.localizedMessage ?: "Failed to accept task."
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearAcceptSuccess() {
        _uiState.update { it.copy(acceptSuccess = false) }
    }
}
