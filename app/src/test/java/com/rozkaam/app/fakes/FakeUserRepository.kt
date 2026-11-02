package com.rozkaam.app.fakes

import com.rozkaam.app.data.model.User
import com.rozkaam.app.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeUserRepository(
    initialUser: User? = null
) : UserRepository {

    private val userFlow = MutableStateFlow(initialUser)
    var savedUser: User? = initialUser
    var shouldFail: Boolean = false
    var failureError: Exception = Exception("Database error")

    override suspend fun getUser(uid: String): Result<User?> {
        return if (shouldFail) {
            Result.failure(failureError)
        } else {
            Result.success(savedUser)
        }
    }

    override fun observeUser(uid: String): Flow<User?> = userFlow.asStateFlow()

    override suspend fun saveUserProfile(user: User): Result<Unit> {
        return if (shouldFail) {
            Result.failure(failureError)
        } else {
            savedUser = user
            userFlow.value = user
            Result.success(Unit)
        }
    }

    override suspend fun updateActiveMode(uid: String, activeMode: String): Result<Unit> {
        return if (shouldFail) {
            Result.failure(failureError)
        } else {
            savedUser = savedUser?.copy(activeMode = activeMode)
            userFlow.value = savedUser
            Result.success(Unit)
        }
    }

    override suspend fun updateEmergencyContact(uid: String, emergencyContact: String): Result<Unit> {
        return if (shouldFail) {
            Result.failure(failureError)
        } else {
            savedUser = savedUser?.copy(emergencyContact = emergencyContact)
            userFlow.value = savedUser
            Result.success(Unit)
        }
    }

    override suspend fun updateUserName(uid: String, name: String): Result<Unit> {
        return if (shouldFail) {
            Result.failure(failureError)
        } else {
            savedUser = savedUser?.copy(name = name)
            userFlow.value = savedUser
            Result.success(Unit)
        }
    }

    override suspend fun updateAvailability(uid: String, available: Boolean, radiusKm: Int): Result<Unit> {
        return if (shouldFail) {
            Result.failure(failureError)
        } else {
            savedUser = savedUser?.copy(available = available, radiusKm = radiusKm)
            userFlow.value = savedUser
            Result.success(Unit)
        }
    }
}

