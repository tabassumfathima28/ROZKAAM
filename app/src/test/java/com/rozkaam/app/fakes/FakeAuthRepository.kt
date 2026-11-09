package com.rozkaam.app.fakes

import android.app.Activity
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.rozkaam.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository(
    initialUser: FirebaseUser? = null,
    var fakeUserId: String? = null,
    var fakeUserPhone: String? = "+919876543210",
    var fakeUserPhotoUrl: String? = null,
    var shouldFailVerification: Boolean = false,
    var verificationError: Exception = Exception("Invalid OTP code")
) : AuthRepository {

    private val authStateFlow = MutableStateFlow(initialUser)
    private val authUserIdFlow = MutableStateFlow(fakeUserId ?: initialUser?.uid)
    var currentFakeUser: FirebaseUser? = initialUser
    var lastSentPhoneNumber: String? = null
    var signOutCalled: Boolean = false
    var verifyOtpCalled: Boolean = false
    var lastVerificationId: String? = null
    var lastOtpCode: String? = null

    override val currentUser: FirebaseUser?
        get() = currentFakeUser

    override val currentUserId: String?
        get() = fakeUserId ?: currentFakeUser?.uid

    override val currentUserPhone: String?
        get() = fakeUserPhone ?: currentFakeUser?.phoneNumber

    override val currentUserPhotoUrl: String?
        get() = fakeUserPhotoUrl ?: currentFakeUser?.photoUrl?.toString()

    override fun observeAuthState(): Flow<FirebaseUser?> = authStateFlow
    override fun observeAuthUserId(): Flow<String?> = authUserIdFlow

    fun updateAuthUserId(uid: String?) {
        fakeUserId = uid
        authUserIdFlow.value = uid
    }

    override fun sendVerificationCode(
        activity: Activity,
        phoneNumber: String,
        resendToken: PhoneAuthProvider.ForceResendingToken?,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        lastSentPhoneNumber = phoneNumber
        callbacks.onCodeSent("fake_ver_id", resendToken ?: createDummyResendToken())
    }

    override suspend fun verifyOtp(verificationId: String, otpCode: String): Result<FirebaseUser> {
        verifyOtpCalled = true
        lastVerificationId = verificationId
        lastOtpCode = otpCode

        return if (shouldFailVerification) {
            Result.failure(verificationError)
        } else {
            val user = currentFakeUser ?: throw IllegalStateException("No fake user configured")
            Result.success(user)
        }
    }

    override suspend fun signInWithCredential(credential: PhoneAuthCredential): Result<FirebaseUser> {
        return if (shouldFailVerification) {
            Result.failure(verificationError)
        } else {
            val user = currentFakeUser ?: throw IllegalStateException("No fake user configured")
            Result.success(user)
        }
    }

    override fun signOut() {
        signOutCalled = true
        currentFakeUser = null
        updateAuthUserId(null)
        authStateFlow.value = null
    }

    private fun createDummyResendToken(): PhoneAuthProvider.ForceResendingToken {
        val constructor = PhoneAuthProvider.ForceResendingToken::class.java.getDeclaredConstructor()
        constructor.isAccessible = true
        return constructor.newInstance()
    }
}
