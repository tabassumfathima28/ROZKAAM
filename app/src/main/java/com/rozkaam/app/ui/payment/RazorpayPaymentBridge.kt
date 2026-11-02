package com.rozkaam.app.ui.payment

import com.razorpay.PaymentData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Result event emitted by Razorpay Checkout SDK callback
 */
sealed interface RazorpayResult {
    data class Success(
        val paymentId: String,
        val orderId: String,
        val signature: String
    ) : RazorpayResult

    data class Error(
        val code: Int,
        val message: String,
        val orderId: String?
    ) : RazorpayResult
}

/**
 * Bridge between MainActivity's PaymentResultWithDataListener and Jetpack Compose ViewModels.
 */
object RazorpayPaymentBridge {
    private val _paymentResult = MutableSharedFlow<RazorpayResult>(extraBufferCapacity = 1)
    val paymentResult: SharedFlow<RazorpayResult> = _paymentResult.asSharedFlow()

    fun onPaymentSuccess(paymentId: String?, paymentData: PaymentData?) {
        val safePaymentId = paymentId ?: paymentData?.paymentId ?: ""
        val safeOrderId = paymentData?.orderId ?: ""
        val safeSignature = paymentData?.signature ?: ""

        _paymentResult.tryEmit(
            RazorpayResult.Success(
                paymentId = safePaymentId,
                orderId = safeOrderId,
                signature = safeSignature
            )
        )
    }

    fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        _paymentResult.tryEmit(
            RazorpayResult.Error(
                code = code,
                message = response ?: "Payment cancelled or failed.",
                orderId = paymentData?.orderId
            )
        )
    }
}

