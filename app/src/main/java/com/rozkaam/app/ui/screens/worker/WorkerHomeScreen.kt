package com.rozkaam.app.ui.screens.worker

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sos
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.rozkaam.app.data.model.TaskWithDistance
import com.rozkaam.app.ui.components.RozkaamCard
import com.rozkaam.app.ui.components.RozkaamChip
import com.rozkaam.app.ui.components.RozkaamEmptyState
import com.rozkaam.app.ui.components.RozkaamPrimaryButton
import com.rozkaam.app.ui.screens.poster.getCategoryIcon
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.Buttermilk
import com.rozkaam.app.ui.theme.MutedRed
import com.rozkaam.app.ui.theme.NunitoFontFamily
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.PastelBlue
import com.rozkaam.app.ui.theme.PoppinsFontFamily
import com.rozkaam.app.ui.theme.RozkaamCardShape
import com.rozkaam.app.ui.theme.SuccessGreen
import com.rozkaam.app.ui.theme.TextSecondary
import com.rozkaam.app.ui.viewmodels.WorkerHomeViewModel

@SuppressLint("MissingPermission")
@Composable
fun WorkerHomeScreen(
    onNavigateToTaskDetails: (String) -> Unit,
    onNavigateToActiveJob: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSos: () -> Unit,
    viewModel: WorkerHomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(uiState.workerLat ?: 12.9716, uiState.workerLng ?: 77.5946),
            14f
        )
    }

    // Permission launcher for Location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.setLocationPermissionGranted(true)
            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        viewModel.updateWorkerLocation(loc.latitude, loc.longitude)
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(
                            LatLng(loc.latitude, loc.longitude),
                            14f
                        )
                    }
                }
        } else {
            viewModel.setLocationPermissionGranted(false)
        }
    }

    // Auto-fetch location if already granted
    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            viewModel.setLocationPermissionGranted(true)
            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        viewModel.updateWorkerLocation(loc.latitude, loc.longitude)
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(
                            LatLng(loc.latitude, loc.longitude),
                            14f
                        )
                    }
                }
        } else {
            // Request permissions
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Update map camera when worker coordinates are received
    LaunchedEffect(uiState.workerLat, uiState.workerLng) {
        val lat = uiState.workerLat
        val lng = uiState.workerLng
        if (lat != null && lng != null) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 14f)
        }
    }

    val workerName = uiState.user?.name?.takeIf { it.isNotBlank() } ?: "Worker"

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundLight)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Namaste, $workerName",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = OldBurgundy
                    )
                    Text(
                        text = "Looking for micro-tasks today?",
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateToSos,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MutedRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Sos,
                                contentDescription = "SOS",
                                tint = MutedRed
                            )
                        }
                    }

                    IconButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier.size(48.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = MutedRed) {
                                    Text("0", color = BackgroundLight)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = OldBurgundy
                            )
                        }
                    }
                }
            }
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            // Free Now Toggle Card per SPEC.md A5
            RozkaamCard(
                containerColor = Buttermilk
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "I'm Free Now",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = OldBurgundy
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (uiState.isFreeNow) {
                                "Ready to receive nearby tasks (${uiState.selectedRadiusKm} km)"
                            } else {
                                "Turn on to discover open tasks near you"
                            },
                            fontFamily = NunitoFontFamily,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = uiState.isFreeNow,
                        onCheckedChange = { viewModel.toggleAvailability(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BackgroundLight,
                            checkedTrackColor = OldBurgundy,
                            uncheckedThumbColor = OldBurgundy,
                            uncheckedTrackColor = PastelBlue
                        )
                    )
                }

                if (uiState.isFreeNow) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Radius:",
                            fontFamily = PoppinsFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OldBurgundy
                        )
                        listOf(3, 5, 10).forEach { radius ->
                            RozkaamChip(
                                label = "$radius km",
                                selected = uiState.selectedRadiusKm == radius,
                                onClick = { viewModel.setRadius(radius) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nearby Tasks / Map View Toggle Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nearby Tasks (${uiState.nearbyTasks.size})",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OldBurgundy
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RozkaamChip(
                        label = "List",
                        icon = Icons.AutoMirrored.Outlined.ListAlt,
                        selected = !uiState.isMapView,
                        onClick = { viewModel.setMapView(false) }
                    )
                    RozkaamChip(
                        label = "Map",
                        icon = Icons.Outlined.Map,
                        selected = uiState.isMapView,
                        onClick = { viewModel.setMapView(true) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    !uiState.isLocationPermissionGranted && uiState.workerLat == null -> {
                        RozkaamEmptyState(
                            title = "Location Access Needed",
                            message = "ROZKAAM needs your location to search for tasks within your chosen radius.",
                            icon = Icons.Outlined.MyLocation,
                            actionText = "Grant Location Permission",
                            onActionClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        )
                    }
                    !uiState.isFreeNow -> {
                        RozkaamEmptyState(
                            title = "You Are Currently Offline",
                            message = "Toggle 'I'm Free Now' above to discover open micro-tasks in your area.",
                            icon = Icons.Outlined.Explore,
                            actionText = "Go Online Now",
                            onActionClick = { viewModel.toggleAvailability(true) }
                        )
                    }
                    uiState.isLoadingTasks -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = OldBurgundy, strokeWidth = 3.dp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Searching for open tasks near you...",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    uiState.nearbyTasks.isEmpty() -> {
                        RozkaamEmptyState(
                            title = "No Tasks Nearby",
                            message = "No open tasks found within ${uiState.selectedRadiusKm} km right now. You'll be notified immediately when a poster creates one.",
                            icon = Icons.Outlined.Explore,
                            actionText = "Expand Radius to 10 km",
                            onActionClick = { viewModel.setRadius(10) }
                        )
                    }
                    uiState.isMapView -> {
                        // Map View showing open tasks and worker location
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RozkaamCardShape)
                                .border(1.dp, OldBurgundy.copy(alpha = 0.2f), RozkaamCardShape)
                        ) {
                            GoogleMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = cameraPositionState,
                                uiSettings = MapUiSettings(
                                    zoomControlsEnabled = false,
                                    compassEnabled = true
                                )
                            ) {
                                // Worker Location Marker
                                uiState.workerLat?.let { wLat ->
                                    uiState.workerLng?.let { wLng ->
                                        Marker(
                                            state = MarkerState(position = LatLng(wLat, wLng)),
                                            title = "You are here"
                                        )
                                    }
                                }

                                // Nearby Task Markers
                                uiState.nearbyTasks.forEach { taskWithDist ->
                                    val task = taskWithDist.task
                                    Marker(
                                        state = MarkerState(position = LatLng(task.lat, task.lng)),
                                        title = task.title,
                                        snippet = "${task.formattedPrice} • ${taskWithDist.formattedDistance}",
                                        onClick = {
                                            onNavigateToTaskDetails(task.id)
                                            true
                                        }
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        // List View
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.nearbyTasks, key = { it.task.id }) { item ->
                                WorkerTaskCard(
                                    item = item,
                                    onClick = { onNavigateToTaskDetails(item.task.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Task card showing category icon, title, price, distance, and schedule per SPEC.md Section A5.
 */
@Composable
fun WorkerTaskCard(
    item: TaskWithDistance,
    onClick: () -> Unit
) {
    val task = item.task

    RozkaamCard(
        containerColor = Color.White,
        borderColor = OldBurgundy.copy(alpha = 0.15f),
        padding = 14.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Tile
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Buttermilk),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(task.category, task.category),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = OldBurgundy
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Task Details Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = OldBurgundy,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Distance Badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = item.formattedDistance,
                            fontFamily = NunitoFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    // Schedule Tag
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = task.scheduledFor,
                            fontFamily = NunitoFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Price Badge
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = PastelBlue
            ) {
                Text(
                    text = task.formattedPrice,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = OldBurgundy,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
