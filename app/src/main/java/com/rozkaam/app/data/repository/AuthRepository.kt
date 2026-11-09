package com.rozkaam.app.data.repository

import android.app.Activity
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

interface AuthRepository {
    val currentUser: FirebaseUser?
    val currentUserId: String?
    val currentUserPhone: String?
    val currentUserPhotoUrl: String?
    fun observeAuthState(): Flow<FirebaseUser?>
    fun observeAuthUserId(): Flow<String?> =
        observeAuthState().map { it?.uid ?: currentUserId }
    fun sendVerificationCode(
        activity: Activity,
        phoneNumber: String,
        resendToken: PhoneAuthProvider.ForceResendingToken? = null,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    )
    suspend fun verifyOtp(verificationId: String, otpCode: String): Result<FirebaseUser>
    suspend fun signInWithCredential(credential: PhoneAuthCredential): Result<FirebaseUser>
    fun signOut()
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepository"
    }

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    override val currentUserPhone: String?
        get() = auth.currentUser?.phoneNumber

    override val currentUserPhotoUrl: String?
        get() = auth.currentUser?.photoUrl?.toString()

    override fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    override fun sendVerificationCode(
        activity: Activity,
        phoneNumber: String,
        resendToken: PhoneAuthProvider.ForceResendingToken?,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        Log.d(TAG, "sendVerificationCode called for $phoneNumber (resend=${resendToken != null})")
        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (resendToken != null) {
            optionsBuilder.setForceResendingToken(resendToken)
        }

        Log.d(TAG, "Calling PhoneAuthProvider.verifyPhoneNumber — auth emulator host: ${auth.app.options.projectId}")
        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    override suspend fun verifyOtp(verificationId: String, otpCode: String): Result<FirebaseUser> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw IllegalStateException("Firebase user is null after sign in")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithCredential(credential: PhoneAuthCredential): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw IllegalStateException("Firebase user is null after sign in")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun signOut() {
        auth.signOut()
    }
}
