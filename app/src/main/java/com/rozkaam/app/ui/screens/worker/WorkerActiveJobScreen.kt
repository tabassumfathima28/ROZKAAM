package com.rozkaam.app.ui.screens.worker

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.rozkaam.app.ui.components.RozkaamCard
import com.rozkaam.app.ui.components.RozkaamEmptyState
import com.rozkaam.app.ui.components.RozkaamPrimaryButton
import com.rozkaam.app.ui.components.RozkaamStepTracker
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.MutedRed
import com.rozkaam.app.ui.theme.NunitoFontFamily
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.SuccessGreen
import com.rozkaam.app.ui.viewmodels.WorkerActiveJobViewModel

val activeJobSteps = listOf("Accepted", "Arrived & OTP", "Working", "Photo Proof", "Paid")

@SuppressLint("MissingPermission")
@Composable
fun WorkerActiveJobScreen(
    taskId: String = "",
    onNavigateBack: () -> Unit,
    onNavigateToUploadProof: (String) -> Unit = {},
    onNavigateToJobCompleted: () -> Unit = {},
    viewModel: WorkerActiveJobViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun startWithCurrentLocation() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            viewModel.startTask(null, null)
            return
        }
        val tokenSource = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
            .addOnSuccessListener { location ->
                viewModel.startTask(location?.latitude, location?.longitude)
            }
            .addOnFailureListener {
                viewModel.startTask(null, null)
            }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) startWithCurrentLocation()
        else viewModel.startTask(null, null)
    }

    LaunchedEffect(taskId) {
        if (taskId.isNotBlank()) viewModel.loadTask(taskId)
    }

    Scaffold(
        topBar = {
            RozkaamTopBar(
                title = "Active Job",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        when {
            taskId.isBlank() -> RozkaamEmptyState(
                title = "No Active Job",
                message = "Choose an accepted task from My Work Tasks to verify your arrival and continue.",
                icon = Icons.Outlined.WorkOutline,
                actionText = "Back to Tasks",
                onActionClick = onNavigateBack,
                modifier = Modifier.padding(paddingValues)
            )
            uiState.isLoading -> androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OldBurgundy, strokeWidth = 3.dp)
            }
            uiState.task == null -> RozkaamEmptyState(
                title = "Task Not Found",
                message = uiState.errorMessage ?: "The task is unavailable.",
                icon = Icons.Outlined.WorkOutline,
                actionText = "Back",
                onActionClick = onNavigateBack,
                modifier = Modifier.padding(paddingValues)
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                RozkaamStepTracker(
                    steps = activeJobSteps,
                    currentStepIndex = if (uiState.isStarted) 2 else 0
                )
                Text(
                    text = uiState.task?.title.orEmpty(),
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OldBurgundy
                )
                uiState.errorMessage?.let { message ->
                    RozkaamCard(containerColor = MutedRed.copy(alpha = 0.1f)) {
                        Text(
                            text = message,
                            fontFamily = NunitoFontFamily,
                            fontSize = 14.sp,
                            color = MutedRed
                        )
                    }
                }
                if (uiState.isStarted) {
                    RozkaamCard(containerColor = SuccessGreen.copy(alpha = 0.12f)) {
                        Text(
                            text = "Arrival verified. Complete the task, then submit photo proof.",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = SuccessGreen
                        )
                    }
                    RozkaamPrimaryButton(
                        text = "Upload Photo Proof",
                        onClick = { onNavigateToUploadProof(taskId) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = uiState.otp,
                        onValueChange = viewModel::updateOtp,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("4-digit arrival OTP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OldBurgundy,
                            unfocusedBorderColor = OldBurgundy.copy(alpha = 0.35f),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                    Text(
                        text = "Ask the poster for the OTP only after you arrive at the task location.",
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        color = OldBurgundy
                    )
                    RozkaamPrimaryButton(
                        text = "Verify Arrival",
                        onClick = {
                            val fine = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val coarse = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (fine || coarse) {
                                startWithCurrentLocation()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        enabled = !uiState.isStarting,
                        isLoading = uiState.isStarting,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
