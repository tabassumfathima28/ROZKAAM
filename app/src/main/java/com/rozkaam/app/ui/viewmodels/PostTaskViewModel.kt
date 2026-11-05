package com.rozkaam.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozkaam.app.data.model.AppConfig
import com.rozkaam.app.data.model.TaskCategory
import com.rozkaam.app.data.model.Wallet
import com.rozkaam.app.data.repository.AuthRepository
import com.rozkaam.app.data.repository.FirebaseAuthRepository
import com.rozkaam.app.data.repository.FirestoreTaskRepository
import com.rozkaam.app.data.repository.FirestoreWalletRepository
import com.rozkaam.app.data.repository.TaskRepository
import com.rozkaam.app.data.repository.WalletRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PostTaskUiState(
    val currentStep: Int = 0, // 0: What, 1: Where & When, 2: Price & Pay

    // Step 1: What is the task?
    val isLoadingCategories: Boolean = true,
    val categories: List<TaskCategory> = emptyList(),
    val categoriesError: String? = null,
    val selectedCategory: String = "",
    val title: String = "",
    val description: String = "",

    // Step 2: Where and when?
    val latitude: Double = 12.9716, // Bangalore default center
    val longitude: Double = 77.5946,
    val address: String = "MG Road, Bengaluru",
    val isGeocoding: Boolean = false,
    val scheduledFor: String = "Now", // "Now", "Today", "Schedule"
    val customScheduleText: String = "",

    // Step 3: Price and payment
    val priceInputRupees: String = "",
    val wallet: Wallet = Wallet(),
    val appConfig: AppConfig = AppConfig(),
    val isSubmitting: Boolean = false,
    val postSuccess: Boolean = false,
    val createdTaskId: String? = null,
    val errorMessage: String? = null
) {
    val pricePaise: Long
        get() = (priceInputRupees.trim().toDoubleOrNull()?.times(100))?.toLong() ?: 0L

    val priceRupees: Long
        get() = pricePaise / 100

    val isStep1Valid: Boolean
        get() = selectedCategory.isNotBlank() && title.trim().length >= 3 && description.trim().length >= 5

    val isStep2Valid: Boolean
        get() = address.isNotBlank() && (scheduledFor != "Schedule" || customScheduleText.isNotBlank())

    val isPriceWithinBounds: Boolean
        get() = pricePaise in appConfig.minPrice..appConfig.maxPrice

    val hasSufficientBalance: Boolean
        get() = wallet.available >= pricePaise && pricePaise > 0L

    val isStep3Valid: Boolean
        get() = isPriceWithinBounds && hasSufficientBalance

    val effectiveScheduledFor: String
        get() = if (scheduledFor == "Schedule" && customScheduleText.isNotBlank()) customScheduleText.trim() else scheduledFor
}

class PostTaskViewModel(
    private val taskRepository: TaskRepository = FirestoreTaskRepository(),
    private val walletRepository: WalletRepository = FirestoreWalletRepository(),
    private val authRepository: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostTaskUiState())
    val uiState: StateFlow<PostTaskUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        loadAppConfig()
        observeWallet()
    }

    fun setStep(step: Int) {
        if (step in 0..2) {
            _uiState.update { it.copy(currentStep = step) }
        }
    }

    fun nextStep(): Boolean {
        val current = _uiState.value.currentStep
        when (current) {
            0 -> {
                if (_uiState.value.isStep1Valid) {
                    _uiState.update { it.copy(currentStep = 1, errorMessage = null) }
                    return true
                } else {
                    _uiState.update { it.copy(errorMessage = "Please select a category and fill in title and description.") }
                    return false
                }
            }
            1 -> {
                if (_uiState.value.isStep2Valid) {
                    _uiState.update { it.copy(currentStep = 2, errorMessage = null) }
                    return true
                } else {
                    _uiState.update { it.copy(errorMessage = "Please confirm the location address.") }
                    return false
                }
            }
            else -> return false
        }
    }

    fun previousStep() {
        val current = _uiState.value.currentStep
        if (current > 0) {
            _uiState.update { it.copy(currentStep = current - 1, errorMessage = null) }
        }
    }

    // --- Step 1 Mutators ---
    fun onSelectCategory(categoryId: String) {
        _uiState.update { it.copy(selectedCategory = categoryId, errorMessage = null) }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle, errorMessage = null) }
    }

    fun onDescriptionChange(newDesc: String) {
        _uiState.update { it.copy(description = newDesc, errorMessage = null) }
    }

    // --- Step 2 Mutators ---
    fun updateLocation(
        lat: Double,
        lng: Double,
        addressResolver: (suspend (Double, Double) -> String?)? = null
    ) {
        _uiState.update {
            it.copy(
                latitude = lat,
                longitude = lng,
                isGeocoding = addressResolver != null
            )
        }

        if (addressResolver != null) {
            viewModelScope.launch {
                val resolvedAddress = runCatching {
                    withContext(Dispatchers.IO) {
                        addressResolver(lat, lng)
                    }
                }.getOrNull()

                _uiState.update { current ->
                    current.copy(
                        isGeocoding = false,
                        address = resolvedAddress?.takeIf { it.isNotBlank() }
                            ?: current.address.ifBlank { "Lat: %.4f, Lng: %.4f".format(lat, lng) }
                    )
                }
            }
        }
    }

    fun onAddressChange(newAddress: String) {
        _uiState.update { it.copy(address = newAddress, errorMessage = null) }
    }

    fun onScheduledForChange(option: String) {
        _uiState.update { it.copy(scheduledFor = option, errorMessage = null) }
    }

    fun onCustomScheduleTextChange(customText: String) {
        _uiState.update { it.copy(customScheduleText = customText, errorMessage = null) }
    }

    // --- Step 3 Mutators ---
    fun onPriceInputChange(input: String) {
        // Only allow numbers
        val clean = input.filter { it.isDigit() }
        _uiState.update { it.copy(priceInputRupees = clean, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCategories = true, categoriesError = null) }
            taskRepository.getCategories()
                .catch { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingCategories = false,
                            categoriesError = err.localizedMessage ?: "Failed to load categories."
                        )
                    }
                }
                .collect { list ->
                    _uiState.update { current ->
                        current.copy(
                            isLoadingCategories = false,
                            categories = list,
                            categoriesError = null,
                            selectedCategory = if (current.selectedCategory.isBlank() && list.isNotEmpty()) {
                                list.first().id
                            } else current.selectedCategory
                        )
                    }
                }
        }
    }

    private fun loadAppConfig() {
        viewModelScope.launch {
            walletRepository.getAppConfig()
                .catch { /* Fall back to defaults in AppConfig() */ }
                .collect { config ->
                    _uiState.update { it.copy(appConfig = config) }
                }
        }
    }

    private fun observeWallet() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            walletRepository.getWallet(uid)
                .catch { /* Keep existing wallet state */ }
                .collect { wallet ->
                    _uiState.update { it.copy(wallet = wallet) }
                }
        }
    }

    fun submitTask() {
        val state = _uiState.value
        if (!state.isStep1Valid) {
            _uiState.update { it.copy(currentStep = 0, errorMessage = "Please complete task details first.") }
            return
        }
        if (!state.isStep2Valid) {
            _uiState.update { it.copy(currentStep = 1, errorMessage = "Please complete location details first.") }
            return
        }
        if (!state.isPriceWithinBounds) {
            _uiState.update {
                it.copy(
                    errorMessage = "Price must be between ₹${state.appConfig.minPriceRupees} and ₹${state.appConfig.maxPriceRupees}."
                )
            }
            return
        }
        if (!state.hasSufficientBalance) {
            _uiState.update {
                it.copy(
                    errorMessage = "Insufficient balance. Please add money to your wallet before posting this task."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            val result = taskRepository.postTask(
                title = state.title.trim(),
                description = state.description.trim(),
                category = state.selectedCategory,
                pricePaise = state.pricePaise,
                lat = state.latitude,
                lng = state.longitude,
                address = state.address.trim(),
                scheduledFor = state.effectiveScheduledFor
            )

            result.fold(
                onSuccess = { postResult ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            postSuccess = true,
                            createdTaskId = postResult.taskId,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = err.localizedMessage ?: "Failed to post task. Please try again."
                        )
                    }
                }
            )
        }
    }
}

