package com.rozkaam.app.ui.screens.poster

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.outlined.Yard
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.rozkaam.app.data.model.TaskCategory
import com.rozkaam.app.ui.components.RozkaamCard
import com.rozkaam.app.ui.components.RozkaamChip
import com.rozkaam.app.ui.components.RozkaamEmptyState
import com.rozkaam.app.ui.components.RozkaamErrorState
import com.rozkaam.app.ui.components.RozkaamLoadingState
import com.rozkaam.app.ui.components.RozkaamOutlinedButton
import com.rozkaam.app.ui.components.RozkaamPrimaryButton
import com.rozkaam.app.ui.components.RozkaamSecondaryButton
import com.rozkaam.app.ui.components.RozkaamStepTracker
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.Buttermilk
import com.rozkaam.app.ui.theme.ErrorRed
import com.rozkaam.app.ui.theme.MutedRed
import com.rozkaam.app.ui.theme.NunitoFontFamily
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.PastelBlue
import com.rozkaam.app.ui.theme.PoppinsFontFamily
import com.rozkaam.app.ui.theme.RozkaamCardShape
import com.rozkaam.app.ui.theme.SuccessGreen
import com.rozkaam.app.ui.theme.TextSecondary
import com.rozkaam.app.ui.viewmodels.PostTaskUiState
import com.rozkaam.app.ui.viewmodels.PostTaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

val postTaskSteps = listOf(
    "What",
    "Where & When",
    "Price & Pay"
)

@Composable
fun PosterPostTaskScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddMoney: () -> Unit,
    viewModel: PostTaskViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            RozkaamTopBar(
                title = "Post a Task",
                canNavigateBack = true,
                onNavigateBack = {
                    if (uiState.currentStep > 0 && !uiState.postSuccess) {
                        viewModel.previousStep()
                    } else {
                        onNavigateBack()
                    }
                }
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        if (uiState.postSuccess) {
            PostTaskSuccessView(
                uiState = uiState,
                onViewTasks = onNavigateBack,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
            ) {
                // Step Tracker per SPEC.md A5
                RozkaamStepTracker(
                    steps = postTaskSteps,
                    currentStepIndex = uiState.currentStep
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Error Banner if present
                uiState.errorMessage?.let { error ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MutedRed.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MutedRed.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = MutedRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                fontFamily = NunitoFontFamily,
                                fontSize = 13.sp,
                                color = MutedRed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Wizard Step Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (uiState.currentStep) {
                        0 -> Step1WhatScreen(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                        1 -> Step2WhereWhenScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            context = context
                        )
                        2 -> Step3PricePayScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onNavigateToAddMoney = onNavigateToAddMoney
                        )
                    }
                }

                // Step Progression Controls
                WizardBottomBar(
                    currentStep = uiState.currentStep,
                    isStepValid = when (uiState.currentStep) {
                        0 -> uiState.isStep1Valid
                        1 -> uiState.isStep2Valid
                        2 -> uiState.isStep3Valid
                        else -> false
                    },
                    isSubmitting = uiState.isSubmitting,
                    onPrevious = { viewModel.previousStep() },
                    onNext = { viewModel.nextStep() },
                    onSubmit = { viewModel.submitTask() }
                )
            }
        }
    }
}

/**
 * Step 1: What is the task?
 * Dynamic category chips from Firestore config/categories, task title and description.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step1WhatScreen(
    uiState: PostTaskUiState,
    viewModel: PostTaskViewModel
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = "Select Category",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = OldBurgundy
        )
        Text(
            text = "Categories are loaded live from Firestore config/categories",
            fontFamily = NunitoFontFamily,
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        when {
            uiState.isLoadingCategories -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = OldBurgundy,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                }
            }
            uiState.categoriesError != null -> {
                RozkaamErrorState(
                    title = "Could not load categories",
                    message = uiState.categoriesError,
                    onRetry = { viewModel.loadCategories() },
                    modifier = Modifier.height(160.dp)
                )
            }
            uiState.categories.isEmpty() -> {
                RozkaamEmptyState(
                    title = "No Categories Available",
                    message = "Please configure categories in Firestore collection config/categories.",
                    actionText = "Refresh",
                    onActionClick = { viewModel.loadCategories() },
                    modifier = Modifier.height(180.dp)
                )
            }
            else -> {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.categories.forEach { category ->
                        val isSelected = uiState.selectedCategory == category.id
                        RozkaamChip(
                            label = category.label,
                            selected = isSelected,
                            icon = getCategoryIcon(category.icon, category.id),
                            onClick = { viewModel.onSelectCategory(category.id) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Task Title
        Text(
            text = "Task Title",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = OldBurgundy
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = uiState.title,
            onValueChange = { viewModel.onTitleChange(it) },
            placeholder = {
                Text(
                    text = "e.g. Move 5 boxes to 2nd floor",
                    fontFamily = NunitoFontFamily,
                    color = TextSecondary
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OldBurgundy,
                unfocusedBorderColor = OldBurgundy.copy(alpha = 0.25f),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = true
        )
        Text(
            text = "Minimum 3 characters",
            fontFamily = NunitoFontFamily,
            fontSize = 12.sp,
            color = if (uiState.title.isNotBlank() && uiState.title.length < 3) MutedRed else TextSecondary,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Task Description
        Text(
            text = "Short Description",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = OldBurgundy
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = uiState.description,
            onValueChange = { viewModel.onDescriptionChange(it) },
            placeholder = {
                Text(
                    text = "Describe the task details, requirements, tools needed, etc.",
                    fontFamily = NunitoFontFamily,
                    color = TextSecondary
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OldBurgundy,
                unfocusedBorderColor = OldBurgundy.copy(alpha = 0.25f),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            minLines = 3,
            maxLines = 5
        )
        Text(
            text = "Minimum 5 characters",
            fontFamily = NunitoFontFamily,
            fontSize = 12.sp,
            color = if (uiState.description.isNotBlank() && uiState.description.length < 5) MutedRed else TextSecondary,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )
    }
}

/**
 * Step 2: Where and when?
 * Google Maps SDK with draggable location pin, Geocoder address resolution,
 * Use My Current Location option with graceful fallback, and scheduling options.
 */
@SuppressLint("MissingPermission")
@Composable
private fun Step2WhereWhenScreen(
    uiState: PostTaskUiState,
    viewModel: PostTaskViewModel,
    context: Context
) {
    val scrollState = rememberScrollState()
    var permissionDeniedNotice by remember { mutableStateOf<String?>(null) }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(uiState.latitude, uiState.longitude),
            15f
        )
    }

    val markerState = remember {
        MarkerState(position = LatLng(uiState.latitude, uiState.longitude))
    }

    // Keep marker position synced when coordinates change
    LaunchedEffect(uiState.latitude, uiState.longitude) {
        val currentLatLng = LatLng(uiState.latitude, uiState.longitude)
        if (markerState.position != currentLatLng) {
            markerState.position = currentLatLng
        }
    }

    // Permission launcher for Location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            permissionDeniedNotice = null
            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        val latLng = LatLng(loc.latitude, loc.longitude)
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 16f)
                        viewModel.updateLocation(loc.latitude, loc.longitude) { lat, lng ->
                            resolveAddressFromCoordinates(context, lat, lng)
                        }
                    }
                }
        } else {
            // Graceful fallback per SPEC.md Section A10
            permissionDeniedNotice = "Location permission denied. You can drag the pin on the map to set your exact location."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Task Location",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = OldBurgundy
            )

            // "Use My Current Location" button per SPEC.md A5
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        val fineGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val coarseGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        if (fineGranted || coarseGranted) {
                            permissionDeniedNotice = null
                            val cts = CancellationTokenSource()
                            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                                .addOnSuccessListener { loc ->
                                    if (loc != null) {
                                        val latLng = LatLng(loc.latitude, loc.longitude)
                                        cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 16f)
                                        viewModel.updateLocation(loc.latitude, loc.longitude) { lat, lng ->
                                            resolveAddressFromCoordinates(context, lat, lng)
                                        }
                                    }
                                }
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                shape = RoundedCornerShape(20.dp),
                color = PastelBlue
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = OldBurgundy
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Current GPS",
                        fontFamily = PoppinsFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OldBurgundy
                    )
                }
            }
        }

        if (permissionDeniedNotice != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Buttermilk.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, OldBurgundy.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = OldBurgundy,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = permissionDeniedNotice!!,
                        fontFamily = NunitoFontFamily,
                        fontSize = 12.sp,
                        color = OldBurgundy
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Google Map with Draggable Pin
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RozkaamCardShape)
                .border(1.dp, OldBurgundy.copy(alpha = 0.2f), RozkaamCardShape)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled = true
                )
            ) {
                Marker(
                    state = markerState,
                    title = "Task Location (Drag to change)",
                    snippet = uiState.address,
                    draggable = true
                )
            }

            // Sync marker drag back to ViewModel
            LaunchedEffect(markerState.position) {
                if (markerState.position.latitude != uiState.latitude ||
                    markerState.position.longitude != uiState.longitude
                ) {
                    viewModel.updateLocation(
                        markerState.position.latitude,
                        markerState.position.longitude
                    ) { lat, lng ->
                        resolveAddressFromCoordinates(context, lat, lng)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Resolved Address Card
        RozkaamCard(
            containerColor = Color.White,
            borderColor = OldBurgundy.copy(alpha = 0.15f),
            padding = 12.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = OldBurgundy,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Resolved Address",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = OldBurgundy
                    )
                    if (uiState.isGeocoding) {
                        Text(
                            text = "Resolving address via Geocoder...",
                            fontFamily = NunitoFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    } else {
                        OutlinedTextField(
                            value = uiState.address,
                            onValueChange = { viewModel.onAddressChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OldBurgundy,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = NunitoFontFamily,
                                fontSize = 13.sp,
                                color = OldBurgundy
                            ),
                            singleLine = false,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Scheduling Options: Now, Today, Schedule per SPEC.md A5
        Text(
            text = "When is this task needed?",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = OldBurgundy
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Now", "Today", "Schedule").forEach { option ->
                val isSelected = uiState.scheduledFor == option
                RozkaamChip(
                    label = option,
                    selected = isSelected,
                    icon = if (option == "Schedule") Icons.Outlined.Schedule else null,
                    onClick = { viewModel.onScheduledForChange(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (uiState.scheduledFor == "Schedule") {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = uiState.customScheduleText,
                onValueChange = { viewModel.onCustomScheduleTextChange(it) },
                placeholder = {
                    Text(
                        text = "e.g. Tomorrow at 10:00 AM",
                        fontFamily = NunitoFontFamily,
                        color = TextSecondary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OldBurgundy,
                    unfocusedBorderColor = OldBurgundy.copy(alpha = 0.25f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )
        }
    }
}

/**
 * Step 3: Price and payment
 * Price input, bounds validation from config/app, wallet available balance,
 * escrow hold explanation notice, "Post and Pay from Wallet", or "Add Money".
 */
@Composable
private fun Step3PricePayScreen(
    uiState: PostTaskUiState,
    viewModel: PostTaskViewModel,
    onNavigateToAddMoney: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = "Task Price & Payment",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = OldBurgundy
        )
        Text(
            text = "Enter the amount you wish to pay the worker upon task completion.",
            fontFamily = NunitoFontFamily,
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Price Input Card
        RozkaamCard(
            containerColor = Buttermilk,
            padding = 16.dp
        ) {
            Text(
                text = "Task Offer Price",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = OldBurgundy
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.priceInputRupees,
                onValueChange = { viewModel.onPriceInputChange(it) },
                leadingIcon = {
                    Text(
                        text = "₹",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = OldBurgundy,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                placeholder = {
                    Text(
                        text = "e.g. 350",
                        fontFamily = PoppinsFontFamily,
                        fontSize = 20.sp,
                        color = TextSecondary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OldBurgundy,
                    unfocusedBorderColor = OldBurgundy.copy(alpha = 0.3f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = OldBurgundy
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Allowed range: ₹${uiState.appConfig.minPriceRupees} – ₹${uiState.appConfig.maxPriceRupees}",
                fontFamily = NunitoFontFamily,
                fontSize = 12.sp,
                color = if (!uiState.isPriceWithinBounds && uiState.priceInputRupees.isNotBlank()) MutedRed else TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Available Balance Card
        RozkaamCard(
            containerColor = Color.White,
            borderColor = OldBurgundy.copy(alpha = 0.15f),
            padding = 16.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Buttermilk),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = OldBurgundy,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Wallet Available Balance",
                        fontFamily = NunitoFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "₹${uiState.wallet.availableRupees}",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = OldBurgundy
                    )
                }

                if (uiState.pricePaise > 0L) {
                    if (uiState.hasSufficientBalance) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SuccessGreen.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sufficient",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MutedRed.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = MutedRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Short",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedRed
                                )
                            }
                        }
                    }
                }
            }

            // Insufficient Balance Notice & Top Up Link
            if (uiState.pricePaise > 0L && !uiState.hasSufficientBalance) {
                val neededRupees = uiState.priceRupees - uiState.wallet.availableRupees
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "You need ₹$neededRupees more to post this task.",
                    fontFamily = NunitoFontFamily,
                    fontSize = 13.sp,
                    color = MutedRed
                )
                Spacer(modifier = Modifier.height(8.dp))
                RozkaamSecondaryButton(
                    text = "Add Money (Top Up)",
                    onClick = onNavigateToAddMoney,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Escrow Notice per SPEC.md Section A5 & A9
        RozkaamCard(
            containerColor = PastelBlue.copy(alpha = 0.45f),
            borderColor = OldBurgundy.copy(alpha = 0.15f),
            padding = 16.dp
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    tint = OldBurgundy,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Funds Held Safely in Escrow",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = OldBurgundy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "When you post, ₹${uiState.priceRupees} is moved from Available to Held balance. It is NEVER released until the worker completes the task, uploads photo proof, and you personally approve it.",
                        fontFamily = NunitoFontFamily,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = OldBurgundy
                    )
                }
            }
        }
    }
}

/**
 * Bottom Navigation & Submission Bar for Wizard.
 */
@Composable
private fun WizardBottomBar(
    currentStep: Int,
    isStepValid: Boolean,
    isSubmitting: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currentStep > 0) {
            RozkaamSecondaryButton(
                text = "Back",
                onClick = onPrevious,
                modifier = Modifier.weight(1f)
            )
        }

        if (currentStep < postTaskSteps.size - 1) {
            RozkaamPrimaryButton(
                text = "Continue",
                onClick = onNext,
                enabled = isStepValid,
                modifier = Modifier.weight(if (currentStep > 0) 1.5f else 1f)
            )
        } else {
            RozkaamPrimaryButton(
                text = "Post & Pay from Wallet",
                onClick = onSubmit,
                enabled = isStepValid,
                isLoading = isSubmitting,
                modifier = Modifier.weight(1.5f)
            )
        }
    }
}

/**
 * Task Creation Success View.
 */
@Composable
private fun PostTaskSuccessView(
    uiState: PostTaskUiState,
    onViewTasks: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(SuccessGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Task Posted Successfully!",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = OldBurgundy,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your task is now live and visible to nearby workers. ₹${uiState.priceRupees} has been moved to escrow hold.",
            fontFamily = NunitoFontFamily,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        RozkaamCard(
            containerColor = Buttermilk,
            padding = 16.dp
        ) {
            Text(
                text = "Arrival 4-Digit OTP",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = OldBurgundy
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "When an accepted worker arrives at your location, ask for their arrival and share the 4-digit OTP shown on your task details card.",
                fontFamily = NunitoFontFamily,
                fontSize = 12.sp,
                color = OldBurgundy
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        RozkaamPrimaryButton(
            text = "View My Tasks",
            onClick = onViewTasks,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Android Geocoder address resolver helper executing on background dispatcher.
 */
suspend fun resolveAddressFromCoordinates(
    context: Context,
    lat: Double,
    lng: Double
): String? = withContext(Dispatchers.IO) {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocation(lat, lng, 1)
        if (!addresses.isNullOrEmpty()) {
            val addr = addresses[0]
            val feature = addr.featureName
            val thoroughfare = addr.thoroughfare
            val subLocality = addr.subLocality
            val locality = addr.locality
            val joined = listOfNotNull(feature, thoroughfare, subLocality, locality)
                .distinct()
                .filter { it.isNotBlank() }
                .joinToString(", ")
            joined.ifBlank { addr.getAddressLine(0) }
        } else null
    } catch (e: Exception) {
        null
    }
}

/**
 * Category Icon Resolver based on category icon string and ID.
 */
fun getCategoryIcon(iconName: String, categoryId: String): ImageVector {
    val key = iconName.lowercase().ifBlank { categoryId.lowercase() }
    return when {
        key.contains("clean") -> Icons.Outlined.CleaningServices
        key.contains("deliver") -> Icons.Outlined.WorkOutline
        key.contains("move") || key.contains("pack") || key.contains("box") -> Icons.Outlined.Inventory2
        key.contains("repair") || key.contains("fix") || key.contains("tool") -> Icons.Outlined.Build
        key.contains("garden") || key.contains("yard") -> Icons.Outlined.Yard
        key.contains("car") || key.contains("wash") -> Icons.Outlined.DirectionsCar
        key.contains("pet") -> Icons.Outlined.Pets
        else -> Icons.Outlined.WorkOutline
    }
}
