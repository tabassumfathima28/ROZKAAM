package com.rozkaam.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozkaam.app.data.model.Task
import com.rozkaam.app.data.repository.AuthRepository
import com.rozkaam.app.data.repository.FirebaseAuthRepository
import com.rozkaam.app.data.repository.FirestoreTaskRepository
import com.rozkaam.app.data.repository.TaskRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkerTasksUiState(
    val isLoading: Boolean = true,
    val tasks: List<Task> = emptyList(),
    val selectedTab: String = "Active",
    val errorMessage: String? = null
) {
    val activeTasks: List<Task>
        get() = tasks.filter {
            it.status == Task.STATUS_ACCEPTED ||
                it.status == Task.STATUS_STARTED ||
                it.status == Task.STATUS_SUBMITTED
        }

    val completedTasks: List<Task>
        get() = tasks.filter { it.status == Task.STATUS_COMPLETED }

    val cancelledTasks: List<Task>
        get() = tasks.filter { it.status == Task.STATUS_CANCELLED }

    val filteredTasks: List<Task>
        get() = when (selectedTab) {
            TAB_COMPLETED -> completedTasks
            TAB_CANCELLED -> cancelledTasks
            else -> activeTasks
        }

    companion object {
        const val TAB_ACTIVE = "Active"
        const val TAB_COMPLETED = "Completed"
        const val TAB_CANCELLED = "Cancelled"
    }
}

class WorkerTasksViewModel(
    private val taskRepository: TaskRepository = FirestoreTaskRepository(),
    private val authRepository: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkerTasksUiState())
    val uiState: StateFlow<WorkerTasksUiState> = _uiState.asStateFlow()
    private var currentWorkerId: String? = null
    private var workerTasksJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.observeAuthUserId()
                .distinctUntilChanged()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Could not read the signed-in account."
                        )
                    }
                }
                .collect { workerId ->
                    currentWorkerId = workerId?.takeIf(String::isNotBlank)
                    observeWorkerTasks()
                }
        }
    }

    fun setSelectedTab(tab: String) {
        if (tab in setOf(
                WorkerTasksUiState.TAB_ACTIVE,
                WorkerTasksUiState.TAB_COMPLETED,
                WorkerTasksUiState.TAB_CANCELLED
            )
        ) {
            _uiState.update { it.copy(selectedTab = tab) }
        }
    }

    fun refresh() {
        currentWorkerId = authRepository.currentUserId?.takeIf(String::isNotBlank)
        observeWorkerTasks()
    }

    private fun observeWorkerTasks() {
        workerTasksJob?.cancel()
        val workerId = currentWorkerId
        if (workerId == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    tasks = emptyList(),
                    errorMessage = "Sign in to view your work tasks."
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, tasks = emptyList(), errorMessage = null) }
        workerTasksJob = viewModelScope.launch {
            taskRepository.getWorkerTasks(workerId)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load your tasks."
                        )
                    }
                }
                .collect { tasks ->
                    _uiState.update {
                        it.copy(isLoading = false, tasks = tasks, errorMessage = null)
                    }
                }
        }
    }

}
