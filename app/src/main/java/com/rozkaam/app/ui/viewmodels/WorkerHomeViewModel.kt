package com.rozkaam.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozkaam.app.data.model.TaskWithDistance
import com.rozkaam.app.data.model.User
import com.rozkaam.app.data.repository.AuthRepository
import com.rozkaam.app.data.repository.FirebaseAuthRepository
import com.rozkaam.app.data.repository.FirestoreTaskRepository
import com.rozkaam.app.data.repository.FirestoreUserRepository
import com.rozkaam.app.data.repository.TaskRepository
import com.rozkaam.app.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkerHomeUiState(
    val user: User? = null,
    val isFreeNow: Boolean = false,
    val selectedRadiusKm: Int = 5,
    val workerLat: Double? = null,
    val workerLng: Double? = null,
    val isLocationPermissionGranted: Boolean = false,
    val isMapView: Boolean = false,
    val isLoadingTasks: Boolean = false,
    val nearbyTasks: List<TaskWithDistance> = emptyList(),
    val errorMessage: String? = null
)

class WorkerHomeViewModel(
    private val userRepository: UserRepository = FirestoreUserRepository(),
    private val taskRepository: TaskRepository = FirestoreTaskRepository(),
    private val authRepository: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkerHomeUiState())
    val uiState: StateFlow<WorkerHomeUiState> = _uiState.asStateFlow()

    private var tasksJob: Job? = null

    init {
        observeUserProfile()
    }

    private fun observeUserProfile() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            userRepository.observeUser(uid)
                .catch { /* Keep existing user state */ }
                .collect { user ->
                    _uiState.update { current ->
                        current.copy(
                            user = user,
                            isFreeNow = user?.available ?: false,
                            selectedRadiusKm = user?.radiusKm ?: 5
                        )
                    }

                    // If coordinates are already known, refresh tasks with updated radius/availability
                    val lat = _uiState.value.workerLat
                    val lng = _uiState.value.workerLng
                    if (lat != null && lng != null) {
                        observeNearbyTasks(lat, lng, _uiState.value.selectedRadiusKm)
                    }
                }
        }
    }

    fun toggleAvailability(newAvailable: Boolean) {
        val uid = authRepository.currentUserId ?: return
        val currentRadius = _uiState.value.selectedRadiusKm
        _uiState.update { it.copy(isFreeNow = newAvailable) }

        viewModelScope.launch {
            userRepository.updateAvailability(uid, newAvailable, currentRadius)
        }
    }

    fun setRadius(newRadiusKm: Int) {
        val uid = authRepository.currentUserId ?: return
        val isFree = _uiState.value.isFreeNow
        _uiState.update { it.copy(selectedRadiusKm = newRadiusKm) }

        viewModelScope.launch {
            userRepository.updateAvailability(uid, isFree, newRadiusKm)
        }
    }

    fun setMapView(isMap: Boolean) {
        _uiState.update { it.copy(isMapView = isMap) }
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(isLocationPermissionGranted = granted) }
    }

    fun updateWorkerLocation(lat: Double, lng: Double) {
        _uiState.update {
            it.copy(
                workerLat = lat,
                workerLng = lng,
                isLocationPermissionGranted = true
            )
        }
        observeNearbyTasks(lat, lng, _uiState.value.selectedRadiusKm)
    }

    private fun observeNearbyTasks(lat: Double, lng: Double, radiusKm: Int) {
        tasksJob?.cancel()
        val workerUid = authRepository.currentUserId.orEmpty()

        _uiState.update { it.copy(isLoadingTasks = true, errorMessage = null) }

        tasksJob = viewModelScope.launch {
            taskRepository.getNearbyOpenTasks(
                workerLat = lat,
                workerLng = lng,
                radiusKm = radiusKm.toDouble(),
                workerUid = workerUid
            )
                .catch { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingTasks = false,
                            errorMessage = err.localizedMessage ?: "Failed to query nearby tasks."
                        )
                    }
                }
                .collect { tasks ->
                    _uiState.update {
                        it.copy(
                            isLoadingTasks = false,
                            nearbyTasks = tasks,
                            errorMessage = null
                        )
                    }
                }
        }
    }
}

