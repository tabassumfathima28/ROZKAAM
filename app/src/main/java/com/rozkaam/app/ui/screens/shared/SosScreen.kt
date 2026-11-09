package com.rozkaam.app.ui.screens.shared

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.rozkaam.app.ui.components.RozkaamCard
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.Buttermilk
import com.rozkaam.app.ui.theme.MutedRed
import com.rozkaam.app.ui.theme.NunitoFontFamily
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.RozkaamButtonShape
import com.rozkaam.app.ui.theme.TextSecondary
import com.rozkaam.app.ui.viewmodels.SosViewModel

@Composable
fun SosScreen(
    onNavigateBack: () -> Unit,
    viewModel: SosViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun openEmergencySms(latitude: Double, longitude: Double) {
        val phone = uiState.emergencyContact.filter { it.isDigit() || it == '+' }
        if (phone.count(Char::isDigit) < 10) {
            viewModel.setError("Add a valid emergency contact in your profile before using SOS.")
            return
        }
        val message = "I need help. My current location is https://maps.google.com/?q=$latitude,$longitude"
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.fromParts("smsto", phone, null)
            putExtra("sms_body", message)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            viewModel.setError("No SMS app is available on this device.")
        }
    }

    @SuppressLint("MissingPermission")
    fun requestCurrentLocation() {
        val tokenSource = CancellationTokenSource()
        locationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            tokenSource.token
        ).addOnSuccessListener { location ->
            if (location == null) {
                viewModel.setError("Current location is unavailable. Try again outdoors.")
            } else {
                openEmergencySms(location.latitude, location.longitude)
            }
        }.addOnFailureListener { error ->
            viewModel.setError(error.localizedMessage ?: "Could not get your current location.")
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            requestCurrentLocation()
        } else {
            viewModel.setError("Location access is required to include your live location in the SOS message.")
        }
    }

    fun triggerSos() {
        if (uiState.emergencyContact.isBlank()) {
            viewModel.setError("Set an emergency contact in your profile first.")
            return
        }
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFineLocation || hasCoarseLocation) {
            requestCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = {
            RozkaamTopBar(
                title = "Emergency SOS",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OldBurgundy)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .padding(16.dp),
                    tint = MutedRed
                )
                Text(
                    text = "Safety & Emergency Assistance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = OldBurgundy,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "ROZKAAM will open your SMS app with your registered emergency contact and current map link. Review and send the message in that app.",
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                RozkaamCard(containerColor = Buttermilk) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = MutedRed
                        )
                        Text(
                            text = if (uiState.emergencyContact.isBlank()) {
                                "No emergency contact is saved. Add one in Profile before using SOS."
                            } else {
                                "Emergency contact: ${uiState.emergencyContact}"
                            },
                            fontFamily = NunitoFontFamily,
                            fontSize = 14.sp,
                            color = OldBurgundy
                        )
                        uiState.errorMessage?.takeIf(String::isNotBlank)?.let { message ->
                            Text(
                                text = message,
                                fontFamily = NunitoFontFamily,
                                fontSize = 13.sp,
                                color = MutedRed
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = ::triggerSos,
                    enabled = uiState.emergencyContact.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RozkaamButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MutedRed,
                        contentColor = BackgroundLight
                    )
                ) {
                    Text(
                        text = "Open Emergency SMS",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
