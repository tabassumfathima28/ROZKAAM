package com.rozkaam.app

import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions

/**
 * Custom Application class for ROZKAAM.
 * Configures Firebase Emulators in debug mode on process startup.
 */
class RozkaamApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Connect to local Firebase Emulator Suite only in debug / development builds
        if (BuildConfig.DEBUG) {
            configureFirebaseEmulators()
        }
    }

    private fun configureFirebaseEmulators() {
        try {
            // Android Studio Emulator alias for host machine localhost is 10.0.2.2
            val emulatorHost = "10.0.2.2"

            // 1. Firebase Authentication: 10.0.2.2:9099
            FirebaseAuth.getInstance().useEmulator(emulatorHost, 9099)

            // 2. Cloud Firestore: 10.0.2.2:8080
            FirebaseFirestore.getInstance().useEmulator(emulatorHost, 8080)

            // 3. Cloud Functions: 10.0.2.2:5001
            FirebaseFunctions.getInstance().useEmulator(emulatorHost, 5001)

            Log.i(
                TAG,
                "Connected to Firebase Emulator Suite (Auth: $emulatorHost:9099, Firestore: $emulatorHost:8080, Functions: $emulatorHost:5001)"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Firebase Emulators", e)
        }
    }

    companion object {
        private const val TAG = "RozkaamApplication"
    }
}

