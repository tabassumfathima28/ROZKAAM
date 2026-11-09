package com.rozkaam.app.ui.screens.worker

import android.content.Intent
import android.net.Uri
import com.rozkaam.app.utils.NavigationUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.rozkaam.app.ui.components.RozkaamCard
import com.rozkaam.app.ui.components.RozkaamEmptyState
import com.rozkaam.app.ui.components.RozkaamOutlinedButton
import com.rozkaam.app.ui.components.RozkaamPrimaryButton
import com.rozkaam.app.ui.components.RozkaamTopBar
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
import com.rozkaam.app.ui.viewmodels.WorkerTaskDetailsViewModel
import com.rozkaam.app.ui.viewmodels.WalletViewModel

@Composable
fun WorkerTaskDetailsScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    onAcceptTask: (String) -> Unit,
    viewModel: WorkerTaskDetailsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val walletState by androidx.lifecycle.viewmodel.compose.viewModel<WalletViewModel>()
        .uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    LaunchedEffect(uiState.acceptSuccess, taskId) {
        if (uiState.acceptSuccess) {
            onAcceptTask(taskId)
            viewModel.clearAcceptSuccess()
        }
    }

    Scaffold(
        topBar = {
            RozkaamTopBar(
                title = "Task Details",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            val task = uiState.task
            if (task?.isOpen == true &&
                uiState.currentWorkerUid.isNotBlank() &&
                task.posterId != uiState.currentWorkerUid
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundLight)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    RozkaamPrimaryButton(
                        text = "Accept Task",
                        onClick = { viewModel.acceptTask() },
                        isLoading = uiState.isAccepting,
                        enabled = !uiState.isAccepting,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = OldBurgundy, strokeWidth = 3.dp)
                }
            }
            uiState.task == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    RozkaamEmptyState(
                        title = "Task Not Found",
                        message = "The requested task does not exist or has already been taken by another worker.",
                        icon = Icons.AutoMirrored.Outlined.Assignment,
                        actionText = "Back to Nearby Tasks",
                        onActionClick = onNavigateBack
                    )
                }
            }
            else -> {
                val task = uiState.task!!
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(LatLng(task.lat, task.lng), 15f)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Error Notice if accept failed
                    uiState.errorMessage?.let { errorMsg ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
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
                                    text = errorMsg,
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 13.sp,
                                    color = MutedRed,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Map View with Marker and Open in Google Maps Intent per SPEC.md A5 & A10
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
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
                            Marker(
                                state = MarkerState(position = LatLng(task.lat, task.lng)),
                                title = task.title,
                                snippet = task.address
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Open in Google Maps Web Directions Button per SPEC.md A5 & A10
                    RozkaamOutlinedButton(
                        text = "Open in Google Maps",
                        icon = Icons.Outlined.Map,
                        onClick = {
                            NavigationUtils.openDirectionsInBrowser(
                                context = context,
                                lat = task.lat,
                                lng = task.lng
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Task Header Card
                    RozkaamCard(
                        containerColor = Buttermilk,
                        padding = 16.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(task.category, task.category),
                                        contentDescription = null,
                                        tint = OldBurgundy,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = task.title,
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = OldBurgundy
                                    )
                                    Text(
                                        text = task.address,
                                        fontFamily = NunitoFontFamily,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Price Tag
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = OldBurgundy
                            ) {
                                Text(
                                    text = task.formattedPrice,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = BackgroundLight,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Schedule info
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = OldBurgundy,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Required: ${task.scheduledFor}",
                                fontFamily = NunitoFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OldBurgundy
                            )
                        }
                    }

                    if (task.isCompleted) {
                        Spacer(modifier = Modifier.height(14.dp))
                        RozkaamCard(
                            containerColor = SuccessGreen.copy(alpha = 0.12f),
                            borderColor = SuccessGreen.copy(alpha = 0.3f),
                            padding = 16.dp
                        ) {
                            Text(
                                text = "Payment released",
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = SuccessGreen
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${task.formattedPrice} credited to your available wallet balance.",
                                fontFamily = NunitoFontFamily,
                                fontSize = 14.sp,
                                color = OldBurgundy
                            )
                            if (walletState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(top = 12.dp),
                                    color = OldBurgundy
                                )
                            } else if (walletState.errorMessage == null) {
                                Text(
                                    text = "Updated balance: ${walletState.wallet.formattedAvailable}",
                                    modifier = Modifier.padding(top = 6.dp),
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = OldBurgundy
                                )
                            } else {
                                Text(
                                    text = walletState.errorMessage.orEmpty(),
                                    modifier = Modifier.padding(top = 6.dp),
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 13.sp,
                                    color = MutedRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Description Card
                    RozkaamCard(
                        containerColor = Color.White,
                        borderColor = OldBurgundy.copy(alpha = 0.15f),
                        padding = 16.dp
                    ) {
                        Text(
                            text = "Task Description",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = OldBurgundy
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = task.description,
                            fontFamily = NunitoFontFamily,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = OldBurgundy
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Poster Profile Card
                    val poster = uiState.posterProfile
                    val posterName = poster?.name?.takeIf { it.isNotBlank() } ?: "Verified Poster"
                    val posterRating = poster?.rating
                        ?.takeIf { it > 0.0 }
                        ?.let { "%.1f".format(it) }
                        ?: "Not rated"

                    RozkaamCard(
                        containerColor = PastelBlue.copy(alpha = 0.4f),
                        borderColor = OldBurgundy.copy(alpha = 0.15f),
                        padding = 14.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Buttermilk),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = OldBurgundy,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = posterName,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = OldBurgundy
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Star,
                                        contentDescription = null,
                                        tint = OldBurgundy,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "$posterRating • ${poster?.completedCount ?: 0} tasks completed",
                                        fontFamily = NunitoFontFamily,
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Contact button: Locked until accepted per SPEC.md Section A5
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.05f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Contact locked until task accepted",
                                fontFamily = NunitoFontFamily,
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
