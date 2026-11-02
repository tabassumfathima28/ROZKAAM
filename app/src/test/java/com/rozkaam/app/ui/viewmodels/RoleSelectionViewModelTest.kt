package com.rozkaam.app.ui.viewmodels

import com.rozkaam.app.data.model.User
import com.rozkaam.app.fakes.FakeAuthRepository
import com.rozkaam.app.fakes.FakeUserRepository
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
class RoleSelectionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeUserRepository: FakeUserRepository
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var viewModel: RoleSelectionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeUserRepository = FakeUserRepository()
        fakeAuthRepository = FakeAuthRepository(fakeUserId = "test_user_123")
        viewModel = RoleSelectionViewModel(
            userRepository = fakeUserRepository,
            authRepository = fakeAuthRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDefaultRole_IsWorker() {
        assertEquals(User.MODE_WORKER, viewModel.uiState.value.selectedRole)
        assertEquals(OnboardingStep.ROLE_SELECTION, viewModel.uiState.value.currentStep)
    }

    @Test
    fun testRoleChange_SwitchesRole() {
        viewModel.onRoleChange(User.MODE_POSTER)
        assertEquals(User.MODE_POSTER, viewModel.uiState.value.selectedRole)

        viewModel.onRoleChange(User.MODE_WORKER)
        assertEquals(User.MODE_WORKER, viewModel.uiState.value.selectedRole)
    }

    @Test
    fun testStepTransitions_RoleSelectionToProfileSetupAndBack() {
        assertEquals(OnboardingStep.ROLE_SELECTION, viewModel.uiState.value.currentStep)

        viewModel.goToProfileSetup()
        assertEquals(OnboardingStep.PROFILE_SETUP, viewModel.uiState.value.currentStep)

        viewModel.goToRoleSelection()
        assertEquals(OnboardingStep.ROLE_SELECTION, viewModel.uiState.value.currentStep)
    }

    @Test
    fun testSaveProfile_WithoutSignIn_SetsError() = runTest {
        fakeAuthRepository.fakeUserId = null
        viewModel.onFullNameChange("Ramesh Kumar")
        viewModel.onEmergencyContactChange("+919876543210")

        var successCallbackCalled = false
        viewModel.saveProfile { successCallbackCalled = true }

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("session expired", ignoreCase = true))
        assertFalse(successCallbackCalled)
    }

    @Test
    fun testSaveProfile_WithBlankName_SetsError() = runTest {
        viewModel.onFullNameChange("   ")
        viewModel.onEmergencyContactChange("+919876543210")

        viewModel.saveProfile {}

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("full name", ignoreCase = true))
    }

    @Test
    fun testSaveProfile_WithInvalidEmergencyContact_SetsError() = runTest {
        viewModel.onFullNameChange("Ramesh Kumar")
        viewModel.onEmergencyContactChange("123")

        viewModel.saveProfile {}

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("emergency contact", ignoreCase = true))
    }

    @Test
    fun testSaveProfile_Success_SavesUserAndTriggersCallback() = runTest {
        viewModel.onFullNameChange("Ramesh Kumar")
        viewModel.onEmergencyContactChange("+919876543210")
        viewModel.onRoleChange(User.MODE_WORKER)

        var passedMode = ""
        viewModel.saveProfile { mode -> passedMode = mode }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertTrue(state.isComplete)
        assertEquals(User.MODE_WORKER, passedMode)
        assertNotNull(fakeUserRepository.savedUser)
        assertEquals("Ramesh Kumar", fakeUserRepository.savedUser?.name)
        assertEquals("+919876543210", fakeUserRepository.savedUser?.emergencyContact)
    }
}
