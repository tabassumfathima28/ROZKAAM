package com.rozkaam.app.ui.screens.onboarding

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import com.rozkaam.app.BuildConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.Surface
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseUser
import com.rozkaam.app.ui.components.RozkaamOtpInput
import com.rozkaam.app.ui.components.RozkaamPrimaryButton
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.Buttermilk
import com.rozkaam.app.ui.theme.MutedRed
import com.rozkaam.app.ui.theme.NunitoFontFamily
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.OutlineColor
import com.rozkaam.app.ui.theme.PoppinsFontFamily
import com.rozkaam.app.ui.theme.TextSecondary
import com.rozkaam.app.ui.viewmodels.AuthViewModel

@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onLoginSuccess: (FirebaseUser) -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context.findActivity()
    val otpFocusRequester = remember { FocusRequester() }

    // Intercept hardware system back button if in OTP step
    BackHandler(enabled = uiState.isCodeSent) {
        viewModel.resetToPhoneInput()
    }

    // When signedInUser becomes non-null, notify parent to route based on Firestore profile
    LaunchedEffect(uiState.signedInUser) {
        uiState.signedInUser?.let { user ->
            onLoginSuccess(user)
        }
    }

    // Auto-focus OTP when code is sent
    LaunchedEffect(uiState.isCodeSent) {
        if (uiState.isCodeSent) {
            try {
                otpFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            RozkaamTopBar(
                title = if (!uiState.isCodeSent) "Sign In" else "Verify OTP",
                canNavigateBack = true,
                onNavigateBack = {
                    if (uiState.isCodeSent) {
                        viewModel.resetToPhoneInput()
                    } else {
                        onNavigateBack()
                    }
                }
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Header Icon
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Buttermilk),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (!uiState.isCodeSent) Icons.Outlined.PhoneAndroid else Icons.Outlined.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp),
                            tint = OldBurgundy
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = if (!uiState.isCodeSent) "Enter your mobile number" else "Verify 6-digit code",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = OldBurgundy
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (!uiState.isCodeSent) {
                            "We will send a 6-digit SMS verification code to verify your account."
                        } else {
                            "Enter the 6-digit code sent to +91 ${uiState.phoneNumber}"
                        },
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Error Message Card
                    if (uiState.errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MutedRed.copy(alpha = 0.12f))
                                .border(1.dp, MutedRed.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = MutedRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = uiState.errorMessage ?: "",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 14.sp,
                                    color = MutedRed,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    if (!uiState.isCodeSent) {
                        // STEP 1: Phone Number Input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Country Code +91 Box
                            Box(
                                modifier = Modifier
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Buttermilk)
                                    .border(1.dp, OutlineColor, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🇮🇳 +91",
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = OldBurgundy
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // 10-Digit Mobile Input
                            OutlinedTextField(
                                value = uiState.phoneNumber,
                                onValueChange = { viewModel.onPhoneNumberChange(it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                placeholder = {
                                    Text(
                                        "Mobile number",
                                        fontFamily = NunitoFontFamily,
                                        fontSize = 15.sp,
                                        color = TextSecondary.copy(alpha = 0.6f)
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OldBurgundy,
                                    unfocusedBorderColor = OutlineColor,
                                    focusedTextColor = OldBurgundy,
                                    unfocusedTextColor = OldBurgundy
                                )
                            )
                        }

                        if (BuildConfig.DEBUG) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.onPhoneNumberChange("7358496770") },
                                color = Buttermilk.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, OldBurgundy.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = "⚡ Fill Test Phone: 7358496770",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 12.sp,
                                    color = OldBurgundy,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    } else {
                        // STEP 2: 6-Box OTP Input per A5
                        RozkaamOtpInput(
                            otpValue = uiState.otpCode,
                            onOtpChange = { viewModel.onOtpChange(it) },
                            focusRequester = otpFocusRequester,
                            onComplete = { viewModel.verifyOtp() }
                        )

                        if (BuildConfig.DEBUG) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.onOtpChange("123456") },
                                color = Buttermilk.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, OldBurgundy.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = "⚡ Fill Test OTP: 123456",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 12.sp,
                                    color = OldBurgundy,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Resend Timer & Action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.resendCooldownSeconds > 0) {
                                Text(
                                    text = "Resend OTP in ${uiState.resendCooldownSeconds}s",
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            } else {
                                TextButton(
                                    onClick = {
                                        if (activity != null) {
                                            viewModel.resendOtp(activity)
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "Resend OTP",
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = OldBurgundy
                                    )
                                }
                            }

                            TextButton(
                                onClick = {
                                    viewModel.resetToPhoneInput()
                                }
                            ) {
                                Text(
                                    text = "Edit Number",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = OldBurgundy
                                )
                            }
                        }
                    }
                }

                // Bottom Action Button
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!uiState.isCodeSent) {
                        RozkaamPrimaryButton(
                            text = "Send OTP",
                            isLoading = uiState.isLoading,
                            enabled = uiState.phoneNumber.length == 10 && !uiState.isLoading && !uiState.isCheckingProfile,
                            onClick = {
                                if (activity != null) {
                                    viewModel.sendOtp(activity)
                                }
                            }
                        )
                    } else {
                        RozkaamPrimaryButton(
                            text = "Verify & Continue",
                            isLoading = uiState.isLoading || uiState.isCheckingProfile,
                            enabled = uiState.otpCode.length == 6 && !uiState.isLoading && !uiState.isCheckingProfile,
                            onClick = { viewModel.verifyOtp() }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Overlay indicator when verifying account & checking Firestore profile
            if (uiState.isCheckingProfile) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(OldBurgundy.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(BackgroundLight)
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = OldBurgundy,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Verifying account & profile...",
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = OldBurgundy
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Traverses context wrappers to find the host Activity.
 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
