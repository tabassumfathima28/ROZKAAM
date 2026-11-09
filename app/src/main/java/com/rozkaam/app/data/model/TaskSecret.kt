package com.rozkaam.app.data.model

data class TaskSecret(
    val taskId: String = "",
    val otp: String? = null,
    val qrToken: String? = null,
    val proofQrToken: String? = null,
    val failedAttempts: Int = 0,
    val locked: Boolean = false
) {
    val isAvailable: Boolean
        get() = !otp.isNullOrBlank() && !locked
}
