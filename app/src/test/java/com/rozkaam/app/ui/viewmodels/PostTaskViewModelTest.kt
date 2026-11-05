package com.rozkaam.app.ui.viewmodels

import com.rozkaam.app.data.model.AppConfig
import com.rozkaam.app.data.model.TaskCategory
import com.rozkaam.app.data.model.Wallet
import com.rozkaam.app.fakes.FakeAuthRepository
import com.rozkaam.app.fakes.FakeTaskRepository
import com.rozkaam.app.fakes.FakeWalletRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PostTaskViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeTaskRepo: FakeTaskRepository
    private lateinit var fakeWalletRepo: FakeWalletRepository
    private lateinit var fakeAuthRepo: FakeAuthRepository
    private lateinit var viewModel: PostTaskViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeTaskRepo = FakeTaskRepository(
            initialCategories = listOf(
                TaskCategory("cleaning", "Cleaning", "cleaning"),
                TaskCategory("delivery", "Delivery", "delivery"),
                TaskCategory("moving", "Moving", "moving")
            )
        )
        fakeWalletRepo = FakeWalletRepository(
            initialWallet = Wallet(available = 50000L, held = 0L), // ₹500 available
            initialConfig = AppConfig(minPrice = 10000L, maxPrice = 1000000L) // ₹100 to ₹10,000
        )
        fakeAuthRepo = FakeAuthRepository(fakeUserId = "poster_123")
        viewModel = PostTaskViewModel(
            taskRepository = fakeTaskRepo,
            walletRepository = fakeWalletRepo,
            authRepository = fakeAuthRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsCategoriesAndWallet() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.currentStep)
        assertFalse(state.isLoadingCategories)
        assertEquals(3, state.categories.size)
        assertEquals("cleaning", state.selectedCategory)
        assertEquals(50000L, state.wallet.available)
        assertEquals(10000L, state.appConfig.minPrice)
        assertEquals(1000000L, state.appConfig.maxPrice)
        assertNull(state.errorMessage)
    }

    @Test
    fun step1_validationRequiresTitleDescriptionCategory() = runTest {
        advanceUntilIdle()

        // Initially title and description are blank -> invalid
        assertFalse(viewModel.uiState.value.isStep1Valid)
        assertFalse(viewModel.nextStep())
        assertEquals(0, viewModel.uiState.value.currentStep)
        assertNotNull(viewModel.uiState.value.errorMessage)

        // Set title too short (< 3 chars)
        viewModel.onTitleChange("ab")
        viewModel.onDescriptionChange("Long enough description here")
        assertFalse(viewModel.uiState.value.isStep1Valid)

        // Set valid title & description
        viewModel.onTitleChange("Shift 3 boxes")
        viewModel.onDescriptionChange("Boxes are in apartment 204.")
        assertTrue(viewModel.uiState.value.isStep1Valid)

        // Now nextStep succeeds to step 1 (Where & When)
        assertTrue(viewModel.nextStep())
        assertEquals(1, viewModel.uiState.value.currentStep)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun step2_locationAndSchedulingValidation() = runTest {
        advanceUntilIdle()

        // Fast forward to step 1
        viewModel.onTitleChange("Valid title")
        viewModel.onDescriptionChange("Valid description details")
        viewModel.nextStep()
        assertEquals(1, viewModel.uiState.value.currentStep)

        // Default address is non-blank -> valid
        assertTrue(viewModel.uiState.value.isStep2Valid)

        // Custom schedule text required if "Schedule" is chosen
        viewModel.onScheduledForChange("Schedule")
        assertFalse(viewModel.uiState.value.isStep2Valid)

        viewModel.onCustomScheduleTextChange("Tomorrow 11 AM")
        assertTrue(viewModel.uiState.value.isStep2Valid)
        assertEquals("Tomorrow 11 AM", viewModel.uiState.value.effectiveScheduledFor)

        // Progress to step 2 (Price & Pay)
        assertTrue(viewModel.nextStep())
        assertEquals(2, viewModel.uiState.value.currentStep)
    }

    @Test
    fun step3_priceBoundsAndBalanceValidation() = runTest {
        advanceUntilIdle()

        // User enters ₹50 (below minPrice of ₹100 = 10000 paise)
        viewModel.onPriceInputChange("50")
        assertEquals(5000L, viewModel.uiState.value.pricePaise)
        assertFalse(viewModel.uiState.value.isPriceWithinBounds)

        // User enters ₹200 (within bounds ₹100..₹10000, wallet has ₹500 available)
        viewModel.onPriceInputChange("200")
        assertEquals(20000L, viewModel.uiState.value.pricePaise)
        assertTrue(viewModel.uiState.value.isPriceWithinBounds)
        assertTrue(viewModel.uiState.value.hasSufficientBalance)
        assertTrue(viewModel.uiState.value.isStep3Valid)

        // User enters ₹800 (within bounds, but exceeds wallet available of ₹500)
        viewModel.onPriceInputChange("800")
        assertEquals(80000L, viewModel.uiState.value.pricePaise)
        assertTrue(viewModel.uiState.value.isPriceWithinBounds)
        assertFalse(viewModel.uiState.value.hasSufficientBalance)
        assertFalse(viewModel.uiState.value.isStep3Valid)
    }

    @Test
    fun submitTask_successfulEscrowPlacement() = runTest {
        advanceUntilIdle()

        // Step 1
        viewModel.onSelectCategory("moving")
        viewModel.onTitleChange("Move study table")
        viewModel.onDescriptionChange("Move a wooden study table to the terrace.")
        viewModel.nextStep()

        // Step 2
        viewModel.updateLocation(12.9716, 77.5946)
        viewModel.onAddressChange("Indiranagar 100ft Road, Bengaluru")
        viewModel.onScheduledForChange("Today")
        viewModel.nextStep()

        // Step 3
        viewModel.onPriceInputChange("300") // ₹300 = 30000 paise <= available ₹500
        assertTrue(viewModel.uiState.value.isStep3Valid)

        // Submit
        viewModel.submitTask()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertTrue(state.postSuccess)
        assertNotNull(state.createdTaskId)
        assertNull(state.errorMessage)

        // Verify fake repo received exact parameters
        val params = fakeTaskRepo.lastPostedParams
        assertNotNull(params)
        assertEquals("Move study table", params?.get("title"))
        assertEquals("Move a wooden study table to the terrace.", params?.get("description"))
        assertEquals("moving", params?.get("category"))
        assertEquals(30000L, params?.get("price"))
        assertEquals(12.9716, params?.get("lat"))
        assertEquals(77.5946, params?.get("lng"))
        assertEquals("Indiranagar 100ft Road, Bengaluru", params?.get("address"))
        assertEquals("Today", params?.get("scheduledFor"))
    }

    @Test
    fun submitTask_backendFailure_setsErrorMessage() = runTest {
        advanceUntilIdle()

        viewModel.onSelectCategory("cleaning")
        viewModel.onTitleChange("Clean garden patio")
        viewModel.onDescriptionChange("Sweep and water the potted plants.")
        viewModel.nextStep()

        viewModel.nextStep()

        viewModel.onPriceInputChange("250")
        fakeTaskRepo.shouldFailPostTask = true

        viewModel.submitTask()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertFalse(state.postSuccess)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Failed to post task"))
    }

    @Test
    fun previousStep_navigatesBackwards() = runTest {
        advanceUntilIdle()

        viewModel.onTitleChange("Valid title")
        viewModel.onDescriptionChange("Valid description")
        viewModel.nextStep()
        assertEquals(1, viewModel.uiState.value.currentStep)

        viewModel.nextStep()
        assertEquals(2, viewModel.uiState.value.currentStep)

        viewModel.previousStep()
        assertEquals(1, viewModel.uiState.value.currentStep)

        viewModel.previousStep()
        assertEquals(0, viewModel.uiState.value.currentStep)
    }
}

