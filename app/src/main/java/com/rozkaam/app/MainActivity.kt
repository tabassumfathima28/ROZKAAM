package com.rozkaam.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.rozkaam.app.ui.navigation.RozkaamApp
import com.rozkaam.app.ui.payment.RazorpayPaymentBridge
import com.rozkaam.app.ui.theme.RozkaamTheme

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Preload Razorpay Checkout for optimal performance
        Checkout.preload(applicationContext)

        enableEdgeToEdge()
        setContent {
            RozkaamTheme {
                RozkaamApp()
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        RazorpayPaymentBridge.onPaymentSuccess(razorpayPaymentId, paymentData)
    }

    override fun onPaymentError(errorCode: Int, response: String?, paymentData: PaymentData?) {
        RazorpayPaymentBridge.onPaymentError(errorCode, response, paymentData)
    }
}
