package com.rozkaam.app.ui.screens.poster

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.razorpay.Checkout
import com.rozkaam.app.BuildConfig
import com.rozkaam.app.ui.components.RozkaamCard
import com.rozkaam.app.ui.components.RozkaamPrimaryButton
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.Buttermilk
import com.rozkaam.app.ui.theme.ErrorRed
import com.rozkaam.app.ui.theme.NunitoFontFamily
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.PastelBlue
import com.rozkaam.app.ui.theme.PoppinsFontFamily
import com.rozkaam.app.ui.theme.SuccessGreen
import com.rozkaam.app.ui.theme.TextSecondary
import com.rozkaam.app.ui.viewmodels.AddMoneyViewModel
import org.json.JSONObject

@Composable
fun PosterAddMoneyScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddMoneyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // Launch Razorpay Checkout when order is prepared by Cloud Functions
    LaunchedEffect(uiState.orderReadyForCheckout) {
        uiState.orderReadyForCheckout?.let { order ->
            val keyId = order.keyId.ifBlank { BuildConfig.RAZORPAY_KEY_ID }
            if (keyId.isBlank()) {
                viewModel.handlePaymentError(0, "Razorpay Key ID is not configured in local.properties or backend.")
                return@LaunchedEffect
            }

            if (activity != null) {
                try {
                    val checkout = Checkout()
                    checkout.setKeyID(keyId)

                    val user = FirebaseAuth.getInstance().currentUser
                    val phone = user?.phoneNumber ?: ""

                    val options = JSONObject().apply {
                        put("name", "ROZKAAM")
                        put("description", "Wallet Balance Top-up")
                        put("currency", order.currency)
                        put("amount", order.amount)
                        put("order_id", order.orderId)
                        put("prefill", JSONObject().apply {
                            if (phone.isNotBlank()) put("contact", phone)
                        })
                        put("theme", JSONObject().apply {
                            put("color", "#43302E") // Old Burgundy branding
                        })
                    }

                    viewModel.onOrderCheckoutLaunched()
                    checkout.open(activity, options)
                } catch (e: Exception) {
                    viewModel.handlePaymentError(0, e.localizedMessage ?: "Failed to launch Razorpay Checkout.")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            RozkaamTopBar(
                title = "Add Money to Wallet",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Success State: Celebratory screen with amount credited
                uiState.paymentSuccess -> {
                    PaymentSuccessContent(
                        amountPaise = uiState.successAmountPaise,
                        orderId = uiState.successOrderId,
                        onDone = {
                            viewModel.resetState()
                            onNavigateBack()
                        }
                    )
                }

                // Main Form Content
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        // Error Alert Banner
                        uiState.errorMessage?.let { errorMsg ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = ErrorRed.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = "Error",
                                        tint = ErrorRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = errorMsg,
                                        fontFamily = NunitoFontFamily,
                                        fontSize = 14.sp,
                                        color = ErrorRed
                                    )
                                }
                            }
                        }

                        // Intro Card
                        RozkaamCard(
                            containerColor = Buttermilk,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(OldBurgundy.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = OldBurgundy,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Razorpay Test Top-up",
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = OldBurgundy
                                    )
                                    Text(
                                        text = "Top-ups are verified on the server and credited instantly to your wallet.",
                                        fontFamily = NunitoFontFamily,
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Quick Amount Chips title
                        Text(
                            text = "Select Amount",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = OldBurgundy
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Predefined Amount Chips from config/app
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            uiState.appConfig.quickAmounts.forEach { amountPaise ->
                                val isSelected = uiState.selectedQuickAmount == amountPaise && uiState.customAmountText.isBlank()
                                AmountChip(
                                    amountRupees = amountPaise / 100,
                                    isSelected = isSelected,
                                    onClick = { viewModel.selectQuickAmount(amountPaise) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Custom Amount Input
                        Text(
                            text = "Or enter custom amount",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = uiState.customAmountText,
                            onValueChange = { viewModel.updateCustomAmount(it) },
                            placeholder = {
                                Text(
                                    text = "e.g. 1500",
                                    fontFamily = NunitoFontFamily,
                                    color = TextSecondary.copy(alpha = 0.5f)
                                )
                            },
                            prefix = {
                                Text(
                                    text = "₹ ",
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = OldBurgundy
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OldBurgundy,
                                unfocusedBorderColor = PastelBlue,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Summary Breakdown Card
                        RozkaamCard(
                            containerColor = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Payment Summary",
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = OldBurgundy
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Amount to add",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "₹${uiState.effectiveAmountRupees}",
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = OldBurgundy
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Platform & GST fee",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "₹0 (Free)",
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = SuccessGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(BackgroundLight)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total Payable",
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = OldBurgundy
                                )
                                Text(
                                    text = "₹${uiState.effectiveAmountRupees}",
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = OldBurgundy
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Pay Button
                        RozkaamPrimaryButton(
                            text = if (uiState.isCreatingOrder) "Creating Order..." else "Proceed to Pay ₹${uiState.effectiveAmountRupees}",
                            icon = Icons.Outlined.Payment,
                            enabled = !uiState.isCreatingOrder && !uiState.isVerifyingPayment && uiState.isValidAmount,
                            isLoading = uiState.isCreatingOrder || uiState.isVerifyingPayment,
                            onClick = { viewModel.initiateTopUp() }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            // Fullscreen Verification Overlay
            if (uiState.isVerifyingPayment) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = OldBurgundy)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Verifying Payment Signature",
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = OldBurgundy,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Validating cryptographic signature with the backend and crediting your wallet...",
                                fontFamily = NunitoFontFamily,
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountChip(
    amountRupees: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) OldBurgundy else Buttermilk
    val textColor = if (isSelected) Color.White else OldBurgundy
    val borderColor = if (isSelected) OldBurgundy else PastelBlue.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "₹$amountRupees",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = textColor
        )
    }
}

@Composable
private fun PaymentSuccessContent(
    amountPaise: Long,
    orderId: String,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(SuccessGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = "Success",
                tint = SuccessGreen,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Top-Up Successful!",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = OldBurgundy
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "₹${amountPaise / 100} has been securely added to your wallet available balance.",
            fontFamily = NunitoFontFamily,
            fontSize = 15.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        if (orderId.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Order ID: $orderId",
                fontFamily = NunitoFontFamily,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        RozkaamPrimaryButton(
            text = "Done & Go to Wallet",
            onClick = onDone
        )
    }
}
