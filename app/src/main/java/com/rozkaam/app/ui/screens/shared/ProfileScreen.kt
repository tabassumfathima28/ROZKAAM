package com.rozkaam.app.ui.screens.shared

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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.rozkaam.app.ui.components.RozkaamLoadingState
import com.rozkaam.app.ui.components.RozkaamOutlinedButton
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.Buttermilk
import com.rozkaam.app.ui.theme.MutedGreen
import com.rozkaam.app.ui.theme.MutedRed
import com.rozkaam.app.ui.theme.NunitoFontFamily
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.OutlineColor
import com.rozkaam.app.ui.theme.PastelBlue
import com.rozkaam.app.ui.theme.PoppinsFontFamily
import com.rozkaam.app.ui.theme.TextSecondary
import com.rozkaam.app.ui.viewmodels.ProfileViewModel

@Composable
fun ProfileScreen(
    onModeChanged: (isWorker: Boolean) -> Unit,
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.user

    var showEditNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }

    var showEditEmergencyDialog by remember { mutableStateOf(false) }
    var emergencyContactInput by remember { mutableStateOf("") }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("English") }

    var showSignOutConfirmDialog by remember { mutableStateOf(false) }

    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(
                    text = "Edit Name",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OldBurgundy
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your full name as it will appear to other users.",
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Ramesh Kumar", fontFamily = NunitoFontFamily) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OldBurgundy,
                            unfocusedBorderColor = OutlineColor,
                            focusedTextColor = OldBurgundy,
                            unfocusedTextColor = OldBurgundy
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameInput.trim().length >= 2) {
                            viewModel.updateName(nameInput.trim())
                        }
                        showEditNameDialog = false
                    }
                ) {
                    Text("Save", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, color = OldBurgundy)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", fontFamily = NunitoFontFamily, color = TextSecondary)
                }
            },
            containerColor = BackgroundLight
        )
    }

    // Edit Emergency Contact Dialog
    if (showEditEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEditEmergencyDialog = false },
            title = {
                Text(
                    text = "Edit Emergency Contact",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OldBurgundy
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter phone number for SOS emergency notifications with live GPS location.",
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = emergencyContactInput,
                        onValueChange = { emergencyContactInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. +91 9876543210", fontFamily = NunitoFontFamily) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OldBurgundy,
                            unfocusedBorderColor = OutlineColor,
                            focusedTextColor = OldBurgundy,
                            unfocusedTextColor = OldBurgundy
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (emergencyContactInput.isNotBlank()) {
                            viewModel.updateEmergencyContact(emergencyContactInput.trim())
                        }
                        showEditEmergencyDialog = false
                    }
                ) {
                    Text("Save", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, color = OldBurgundy)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditEmergencyDialog = false }) {
                    Text("Cancel", fontFamily = NunitoFontFamily, color = TextSecondary)
                }
            },
            containerColor = BackgroundLight
        )
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Text(
                    text = "Select Language",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OldBurgundy
                )
            },
            text = {
                Column {
                    val languages = listOf(
                        "English" to "Active",
                        "हिंदी (Hindi)" to "Coming in Milestone 6",
                        "தமிழ் (Tamil)" to "Coming in Milestone 6"
                    )

                    languages.forEach { (lang, subtitle) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (lang == "English") {
                                        selectedLanguage = "English"
                                        showLanguageDialog = false
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLanguage == lang,
                                onClick = {
                                    if (lang == "English") {
                                        selectedLanguage = "English"
                                        showLanguageDialog = false
                                    }
                                },
                                enabled = lang == "English",
                                colors = RadioButtonDefaults.colors(selectedColor = OldBurgundy)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = lang,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = if (lang == "English") OldBurgundy else TextSecondary
                                )
                                Text(
                                    text = subtitle,
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Done", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, color = OldBurgundy)
                }
            },
            containerColor = BackgroundLight
        )
    }

    // Sign Out Confirmation Dialog
    if (showSignOutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmDialog = false },
            title = {
                Text(
                    text = "Sign Out",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OldBurgundy
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out from ROZKAAM?",
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirmDialog = false
                        viewModel.signOut { onSignOut() }
                    }
                ) {
                    Text("Sign Out", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, color = MutedRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirmDialog = false }) {
                    Text("Cancel", fontFamily = NunitoFontFamily, color = TextSecondary)
                }
            },
            containerColor = BackgroundLight
        )
    }

    Scaffold(
        topBar = {
            RozkaamTopBar(
                title = "My Profile",
                canNavigateBack = false
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        if (uiState.isLoading && user == null) {
            RozkaamLoadingState(message = "Loading profile...")
        } else {
            val isWorker = (user?.activeMode ?: User.MODE_WORKER) == User.MODE_WORKER

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))

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
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Profile Avatar & Identity Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Buttermilk),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "Avatar",
                                modifier = Modifier.size(42.dp),
                                tint = OldBurgundy
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user?.name?.ifBlank { "ROZKAAM Member" } ?: "ROZKAAM Member",
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = OldBurgundy
                                )
                                IconButton(
                                    onClick = {
                                        nameInput = user?.name ?: ""
                                        showEditNameDialog = true
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Edit Name",
                                        tint = OldBurgundy,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = user?.phone?.ifBlank { "Verified Account" } ?: "Verified Account",
                                fontFamily = NunitoFontFamily,
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Rating & Tasks Completed Stats Card
                    RozkaamCard(
                        containerColor = PastelBlue.copy(alpha = 0.5f),
                        padding = 14.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rating Tile
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = OldBurgundy
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format("%.1f", user?.rating ?: 0.0),
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = OldBurgundy
                                    )
                                }
                                Text(
                                    text = "${user?.ratingCount ?: 0} ratings",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .height(36.dp)
                                    .width(1.dp)
                                    .background(OutlineColor)
                            )

                            // Tasks Completed Tile
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MutedGreen
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${user?.completedCount ?: 0}",
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = OldBurgundy
                                    )
                                }
                                Text(
                                    text = "Tasks Done",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Mode Switch Card (Switches bottom bar and graph in real time)
                    RozkaamCard(
                        containerColor = Buttermilk
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(PastelBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.SwapHoriz,
                                        contentDescription = null,
                                        tint = OldBurgundy,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isWorker) "Mode: Worker" else "Mode: Poster",
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = OldBurgundy
                                    )
                                    Text(
                                        text = if (isWorker) "Toggle switch to post micro-tasks" else "Toggle switch to accept nearby tasks",
                                        fontFamily = NunitoFontFamily,
                                        fontSize = 14.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            if (uiState.isUpdatingMode) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = OldBurgundy,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Switch(
                                    checked = !isWorker, // Checked = Poster mode
                                    onCheckedChange = { _ ->
                                        viewModel.toggleMode { newMode ->
                                            onModeChanged(newMode == User.MODE_WORKER)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BackgroundLight,
                                        checkedTrackColor = OldBurgundy,
                                        uncheckedThumbColor = BackgroundLight,
                                        uncheckedTrackColor = OldBurgundy
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Emergency Contact & Language options
                    RozkaamCard(
                        containerColor = BackgroundLight,
                        borderColor = OutlineColor
                    ) {
                        // Emergency Contact Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Call,
                                contentDescription = null,
                                tint = OldBurgundy,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Emergency Contact (SOS)",
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = OldBurgundy
                                )
                                Text(
                                    text = user?.emergencyContact?.ifBlank { "Not set" } ?: "Not set",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                            IconButton(
                                onClick = {
                                    emergencyContactInput = user?.emergencyContact ?: ""
                                    showEditEmergencyDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit Emergency Contact",
                                    tint = OldBurgundy,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            thickness = 1.dp,
                            color = OutlineColor.copy(alpha = 0.5f)
                        )

                        // Language Selection Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLanguageDialog = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Language,
                                contentDescription = null,
                                tint = OldBurgundy,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Language",
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = OldBurgundy
                                )
                                Text(
                                    text = "$selectedLanguage (Tap to change)",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Sign Out Button
                Column(modifier = Modifier.padding(bottom = 20.dp)) {
                    RozkaamOutlinedButton(
                        text = "Sign Out",
                        icon = Icons.AutoMirrored.Outlined.Logout,
                        onClick = {
                            showSignOutConfirmDialog = true
                        }
                    )
                }
            }
        }
    }
}
