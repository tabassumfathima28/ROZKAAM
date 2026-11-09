package com.rozkaam.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozkaam.app.data.model.Task
import com.rozkaam.app.data.repository.AuthRepository
import com.rozkaam.app.data.repository.FirebaseAuthRepository
import com.rozkaam.app.data.repository.FirestoreTaskRepository
import com.rozkaam.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PosterTasksUiState(
    val isLoading: Boolean = true,
    val tasks: List<Task> = emptyList(),
    val selectedFilter: String = "Open",
    val errorMessage: String? = null
) {
    val openCount: Int
        get() = tasks.count { it.status == Task.STATUS_OPEN }

    val activeCount: Int
        get() = tasks.count { it.status == Task.STATUS_ACCEPTED || it.status == Task.STATUS_STARTED }

    val awaitingApprovalCount: Int
        get() = tasks.count { it.status == Task.STATUS_SUBMITTED }

    val completedCount: Int
        get() = tasks.count { it.status == Task.STATUS_COMPLETED }

    val filteredTasks: List<Task>
        get() = when (selectedFilter) {
            "Open" -> tasks.filter { it.status == Task.STATUS_OPEN }
            "Active" -> tasks.filter { it.status == Task.STATUS_ACCEPTED || it.status == Task.STATUS_STARTED }
            "Awaiting approval" -> tasks.filter { it.status == Task.STATUS_SUBMITTED }
            "Completed" -> tasks.filter { it.status == Task.STATUS_COMPLETED }
            else -> tasks
        }
}

class PosterTasksViewModel(
    private val taskRepository: TaskRepository = FirestoreTaskRepository(),
    private val authRepository: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosterTasksUiState())
    val uiState: StateFlow<PosterTasksUiState> = _uiState.asStateFlow()

    init {
        observePosterTasks()
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    private fun observePosterTasks() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            taskRepository.getPosterTasks(uid)
                .catch { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.localizedMessage ?: "Failed to load tasks."
                        )
                    }
                }
                .collect { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            tasks = list,
                            errorMessage = null
                        )
                    }
                }
        }
    }
}

