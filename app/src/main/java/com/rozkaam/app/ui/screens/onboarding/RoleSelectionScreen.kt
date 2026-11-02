package com.rozkaam.app.ui.screens.onboarding

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rozkaam.app.data.model.User
import com.rozkaam.app.ui.components.RozkaamCard
import com.rozkaam.app.ui.components.RozkaamPrimaryButton
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.Buttermilk
import com.rozkaam.app.ui.theme.MutedRed
import com.rozkaam.app.ui.theme.NunitoFontFamily
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.OutlineColor
import com.rozkaam.app.ui.theme.PastelBlue
import com.rozkaam.app.ui.theme.PoppinsFontFamily
import com.rozkaam.app.ui.theme.TextSecondary
import com.rozkaam.app.ui.viewmodels.OnboardingStep
import com.rozkaam.app.ui.viewmodels.RoleSelectionViewModel

@Composable
fun RoleSelectionScreen(
    onComplete: (activeMode: String) -> Unit,
    viewModel: RoleSelectionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isWorker = uiState.selectedRole == User.MODE_WORKER
    val isProfileSetup = uiState.currentStep == OnboardingStep.PROFILE_SETUP

    // Intercept back button when in profile setup step to navigate back to role selection
    BackHandler(enabled = isProfileSetup) {
        viewModel.goToRoleSelection()
    }

    Scaffold(
        topBar = {
            RozkaamTopBar(
                title = if (!isProfileSetup) "Choose Mode" else "Profile Setup",
                canNavigateBack = isProfileSetup,
                onNavigateBack = { viewModel.goToRoleSelection() }
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))

                if (!isProfileSetup) {
                    // ==========================================
                    // STEP 1: ROLE SELECTION
                    // ==========================================
                    Text(
                        text = "Welcome to ROZKAAM",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = OldBurgundy
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Choose your starting mode. You can switch between Worker and Poster anytime in your Profile.",
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Option 1: Worker Mode Card
                    RozkaamCard(
                        containerColor = if (isWorker) Buttermilk else PastelBlue.copy(alpha = 0.35f),
                        borderColor = if (isWorker) OldBurgundy else null,
                        onClick = { viewModel.onRoleChange(User.MODE_WORKER) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isWorker) OldBurgundy else PastelBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Engineering,
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp),
                                    tint = if (isWorker) BackgroundLight else OldBurgundy
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "I want to work",
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = OldBurgundy
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Pick up nearby micro-tasks, complete work, and earn money today.",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }

                            if (isWorker) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(OldBurgundy),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = BackgroundLight
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Option 2: Poster Mode Card
                    RozkaamCard(
                        containerColor = if (!isWorker) Buttermilk else PastelBlue.copy(alpha = 0.35f),
                        borderColor = if (!isWorker) OldBurgundy else null,
                        onClick = { viewModel.onRoleChange(User.MODE_POSTER) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (!isWorker) OldBurgundy else PastelBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PostAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp),
                                    tint = if (!isWorker) BackgroundLight else OldBurgundy
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "I want to post tasks",
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = OldBurgundy
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Post errands, set price, and verify workers with a secure OTP.",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }

                            if (!isWorker) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(OldBurgundy),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = BackgroundLight
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                } else {
                    // ==========================================
                    // STEP 2: PROFILE SETUP (NAME & EMERGENCY CONTACT)
                    // ==========================================
                    Text(
                        text = "Complete Your Profile",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = OldBurgundy
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Enter your full name and emergency contact. These will be securely saved to your account.",
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Selected Role Chip
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Buttermilk)
                            .border(1.dp, OutlineColor, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isWorker) Icons.Outlined.Engineering else Icons.Outlined.PostAdd,
                                    contentDescription = null,
                                    tint = OldBurgundy,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isWorker) "Selected: Worker Mode" else "Selected: Poster Mode",
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = OldBurgundy
                                )
                            }
                            Text(
                                text = "Change",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = OldBurgundy,
                                modifier = Modifier.clickable { viewModel.goToRoleSelection() }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

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
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Full Name Input
                    Text(
                        text = "Full Name *",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = OldBurgundy
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.fullName,
                        onValueChange = { viewModel.onFullNameChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Ramesh Kumar", fontFamily = NunitoFontFamily, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = OldBurgundy,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OldBurgundy,
                            unfocusedBorderColor = OutlineColor,
                            focusedTextColor = OldBurgundy,
                            unfocusedTextColor = OldBurgundy
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Emergency Contact Input
                    Text(
                        text = "Emergency Contact (SOS phone number) *",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = OldBurgundy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Used for sending emergency SMS with live GPS location during active tasks.",
                        fontFamily = NunitoFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.emergencyContact,
                        onValueChange = { viewModel.onEmergencyContactChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. +91 9876543210", fontFamily = NunitoFontFamily, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Phone,
                                contentDescription = null,
                                tint = OldBurgundy,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OldBurgundy,
                            unfocusedBorderColor = OutlineColor,
                            focusedTextColor = OldBurgundy,
                            unfocusedTextColor = OldBurgundy
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Bottom Action Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                if (!isProfileSetup) {
                    RozkaamPrimaryButton(
                        text = "Continue to Profile Setup →",
                        onClick = { viewModel.goToProfileSetup() }
                    )
                } else {
                    RozkaamPrimaryButton(
                        text = if (isWorker) "Save & Start Working" else "Save & Start Posting",
                        isLoading = uiState.isLoading,
                        enabled = !uiState.isLoading,
                        onClick = {
                            viewModel.saveProfile { activeMode ->
                                onComplete(activeMode)
                            }
                        }
                    )
                }
            }
        }
    }
}
