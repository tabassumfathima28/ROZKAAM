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
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeUserRepository: FakeUserRepository
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private val testUser = User(
        uid = "test_user_id",
        name = "Ramesh Kumar",
        phone = "+919876543210",
        activeMode = User.MODE_WORKER,
        emergencyContact = "+919876543211",
        rating = 4.7,
        completedCount = 14
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeUserRepository = FakeUserRepository(initialUser = testUser)
        fakeAuthRepository = FakeAuthRepository(fakeUserId = "test_user_id")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState_WhenUserNotSignedIn_SetsSignedOut() = runTest {
        val signedOutAuthRepo = FakeAuthRepository(fakeUserId = null)
        val viewModel = ProfileViewModel(
            authRepository = signedOutAuthRepo,
            userRepository = fakeUserRepository
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSignedOut)
        assertFalse(state.isLoading)
    }

    @Test
    fun testUserProfile_LoadedSuccessfully() = runTest {
        val viewModel = ProfileViewModel(
            authRepository = fakeAuthRepository,
            userRepository = fakeUserRepository
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.user)
        assertEquals("Ramesh Kumar", state.user?.name)
        assertEquals(User.MODE_WORKER, state.user?.activeMode)
    }

    @Test
    fun testToggleMode_SwitchesBetweenWorkerAndPoster() = runTest {
        val viewModel = ProfileViewModel(
            authRepository = fakeAuthRepository,
            userRepository = fakeUserRepository
        )
        advanceUntilIdle()

        var updatedMode = ""
        viewModel.toggleMode { newMode -> updatedMode = newMode }
        advanceUntilIdle()

        assertEquals(User.MODE_POSTER, updatedMode)
        assertEquals(User.MODE_POSTER, viewModel.uiState.value.user?.activeMode)
        assertEquals(User.MODE_POSTER, fakeUserRepository.savedUser?.activeMode)
    }

    @Test
    fun testUpdateEmergencyContact_Validates10Digits() = runTest {
        val viewModel = ProfileViewModel(
            authRepository = fakeAuthRepository,
            userRepository = fakeUserRepository
        )
        advanceUntilIdle()

        viewModel.updateEmergencyContact("123")
        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("10-digit", ignoreCase = true))
    }

    @Test
    fun testUpdateEmergencyContact_Success_UpdatesRepo() = runTest {
        val viewModel = ProfileViewModel(
            authRepository = fakeAuthRepository,
            userRepository = fakeUserRepository
        )
        advanceUntilIdle()

        viewModel.updateEmergencyContact("+919988776655")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertEquals("+919988776655", state.user?.emergencyContact)
        assertEquals("+919988776655", fakeUserRepository.savedUser?.emergencyContact)
    }

    @Test
    fun testUpdateName_ValidatesMinLength() = runTest {
        val viewModel = ProfileViewModel(
            authRepository = fakeAuthRepository,
            userRepository = fakeUserRepository
        )
        advanceUntilIdle()

        viewModel.updateName("A")
        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("at least 2 characters", ignoreCase = true))
    }

    @Test
    fun testUpdateName_Success_UpdatesRepo() = runTest {
        val viewModel = ProfileViewModel(
            authRepository = fakeAuthRepository,
            userRepository = fakeUserRepository
        )
        advanceUntilIdle()

        viewModel.updateName("Suresh Kumar")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertEquals("Suresh Kumar", state.user?.name)
        assertEquals("Suresh Kumar", fakeUserRepository.savedUser?.name)
    }

    @Test
    fun testSignOut_InvokesAuthSignOut() = runTest {
        val viewModel = ProfileViewModel(
            authRepository = fakeAuthRepository,
            userRepository = fakeUserRepository
        )
        advanceUntilIdle()

        var callbackCalled = false
        viewModel.signOut {
            callbackCalled = true
        }

        assertTrue(fakeAuthRepository.signOutCalled)
        assertTrue(viewModel.uiState.value.isSignedOut)
        assertNull(viewModel.uiState.value.user)
        assertTrue(callbackCalled)
    }
}
