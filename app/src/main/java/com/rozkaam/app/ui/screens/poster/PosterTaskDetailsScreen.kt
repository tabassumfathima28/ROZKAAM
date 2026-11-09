package com.rozkaam.app.ui.screens.poster

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TaskAlt
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rozkaam.app.data.model.Task
import com.rozkaam.app.ui.components.RozkaamCard
import com.rozkaam.app.ui.components.RozkaamChip
import com.rozkaam.app.ui.components.RozkaamEmptyState
import com.rozkaam.app.ui.components.RozkaamOutlinedButton
import com.rozkaam.app.ui.components.RozkaamPrimaryButton
import com.rozkaam.app.ui.components.RozkaamSecondaryButton
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.Buttermilk
import com.rozkaam.app.ui.theme.NunitoFontFamily
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.MutedRed
import com.rozkaam.app.ui.theme.PastelBlue
import com.rozkaam.app.ui.theme.PoppinsFontFamily
import com.rozkaam.app.ui.theme.SuccessGreen
import com.rozkaam.app.ui.theme.TextSecondary
import com.rozkaam.app.ui.viewmodels.PosterTaskDetailsViewModel

@Composable
fun PosterTaskDetailsScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    viewModel: PosterTaskDetailsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    Scaffold(
        topBar = {
            RozkaamTopBar(
                title = "Task Details",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
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
                        title = "No Task Selected",
                        message = "Select one of your posted tasks to view worker status and arrival OTP details.",
                        icon = Icons.Outlined.TaskAlt,
                        actionText = "Back to Tasks",
                        onActionClick = onNavigateBack
                    )
                }
            }
            else -> {
                val task = uiState.task!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Status Header Card
                    RozkaamCard(
                        containerColor = Buttermilk,
                        padding = 16.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = OldBurgundy
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationOn,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = task.address,
                                        fontFamily = NunitoFontFamily,
                                        fontSize = 13.sp,
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
                                    fontSize = 16.sp,
                                    color = BackgroundLight,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = OldBurgundy,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Scheduled: ${task.scheduledFor}",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OldBurgundy
                                )
                            }

                            // Status Chip
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when (task.status) {
                                    Task.STATUS_OPEN -> PastelBlue
                                    Task.STATUS_ACCEPTED -> SuccessGreen.copy(alpha = 0.15f)
                                    else -> Color.Black.copy(alpha = 0.08f)
                                }
                            ) {
                                Text(
                                    text = when (task.status) {
                                        Task.STATUS_SUBMITTED -> "Awaiting approval"
                                        Task.STATUS_STARTED -> "In progress"
                                        else -> task.status.replaceFirstChar { it.uppercase() }
                                    },
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (task.status) {
                                        Task.STATUS_ACCEPTED -> SuccessGreen
                                        else -> OldBurgundy
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (task.isSubmitted) {
                        val worker = uiState.workerProfile
                        RozkaamCard(
                            containerColor = Color.White,
                            borderColor = OldBurgundy.copy(alpha = 0.15f),
                            padding = 16.dp
                        ) {
                            Text(
                                text = "Completion proof",
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = OldBurgundy
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Submitted by ${worker?.name?.takeIf { it.isNotBlank() } ?: "Assigned Worker"}",
                                fontFamily = NunitoFontFamily,
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            uiState.proofImage?.let { image ->
                                Image(
                                    bitmap = image.asImageBitmap(),
                                    contentDescription = "Worker completion proof photo",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 360.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            if (uiState.proofImage == null && task.proofPath != null) {
                                CircularProgressIndicator(color = OldBurgundy, strokeWidth = 2.dp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = task.proofDescription.orEmpty(),
                                fontFamily = NunitoFontFamily,
                                fontSize = 14.sp,
                                color = OldBurgundy
                            )
                            uiState.errorMessage?.let { error ->
                                Text(
                                    text = error,
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 13.sp,
                                    color = MutedRed
                                )
                            }
                            if (!task.isProofQrVerified) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Waiting for the assigned Worker to scan this task's proof QR code before payment approval.",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            RozkaamPrimaryButton(
                                text = "Approve & Release Payment",
                                onClick = viewModel::approveTask,
                                enabled = !uiState.isActionLoading && task.isProofQrVerified,
                                isLoading = uiState.isActionLoading,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (uiState.isProofQr && uiState.qrBitmap != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "The assigned Worker can scan this task-specific QR after submitting proof.",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                                RozkaamSecondaryButton(
                                    text = "Show Proof Verification QR",
                                    onClick = { viewModel.setQrDialogVisible(true) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else if (task.isCompleted) {
                        RozkaamCard(
                            containerColor = SuccessGreen.copy(alpha = 0.12f),
                            padding = 16.dp
                        ) {
                            Text(
                                text = "Payment released",
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = SuccessGreen
                            )
                            Text(
                                text = "${task.formattedPrice} released to ${uiState.workerProfile?.name?.takeIf(String::isNotBlank) ?: "the assigned worker"}.",
                                fontFamily = NunitoFontFamily,
                                fontSize = 14.sp,
                                color = OldBurgundy
                            )
                            if (task.rated) {
                                Text(
                                    text = "Thank you for rating the worker.",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 14.sp,
                                    color = OldBurgundy
                                )
                            } else {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Rate this worker",
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OldBurgundy
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    (1..5).forEach { stars ->
                                        RozkaamChip(
                                            label = "$stars ★",
                                            selected = uiState.rating == stars,
                                            onClick = { viewModel.setRating(stars) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = uiState.reviewComment,
                                    onValueChange = viewModel::setReviewComment,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Feedback (optional)") },
                                    maxLines = 4,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OldBurgundy,
                                        unfocusedBorderColor = OldBurgundy.copy(alpha = 0.35f)
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                RozkaamPrimaryButton(
                                    text = "Submit Rating",
                                    onClick = viewModel::rateWorker,
                                    enabled = !uiState.isActionLoading,
                                    isLoading = uiState.isActionLoading,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else if (task.isCancelled) {
                        RozkaamCard(containerColor = PastelBlue.copy(alpha = 0.4f)) {
                            Text(
                                text = "This task was cancelled and the escrow refund was processed.",
                                fontFamily = NunitoFontFamily,
                                fontSize = 14.sp,
                                color = OldBurgundy
                            )
                        }
                    }

                    if (task.isOpen || task.isAccepted) {
                        Spacer(modifier = Modifier.height(10.dp))
                        RozkaamOutlinedButton(
                            text = "Cancel Task & Refund",
                            onClick = viewModel::cancelTask,
                            enabled = !uiState.isActionLoading
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic Section depending on status per SPEC.md Section A5
                    if (task.isOpen) {
                        // While open: Waiting for workers
                        RozkaamCard(
                            containerColor = PastelBlue.copy(alpha = 0.45f),
                            borderColor = OldBurgundy.copy(alpha = 0.15f),
                            padding = 16.dp
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Buttermilk),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.HourglassEmpty,
                                        contentDescription = null,
                                        tint = OldBurgundy,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Waiting for Worker Acceptance",
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = OldBurgundy
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Your task is broadcast to nearby verified workers within the area.",
                                        fontFamily = NunitoFontFamily,
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    } else if (task.isAccepted || task.isStarted) {
                        // While accepted: Worker Card & Large Arrival OTP Card per SPEC.md Section A5
                        val worker = uiState.workerProfile
                        val workerName = worker?.name?.takeIf { it.isNotBlank() } ?: "Assigned Worker"
                        val workerRating = worker?.rating
                            ?.takeIf { it > 0.0 }
                            ?.let { "%.1f".format(it) }
                            ?: "Not rated"

                        // Worker Profile Card
                        RozkaamCard(
                            containerColor = Color.White,
                            borderColor = OldBurgundy.copy(alpha = 0.15f),
                            padding = 14.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(PastelBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = OldBurgundy,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = workerName,
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
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
                                            text = "$workerRating • ${worker?.completedCount ?: 0} tasks completed",
                                            fontFamily = NunitoFontFamily,
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.fetchCounterpartContact { phone ->
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                            context.startActivity(intent)
                                        }
                                    },
                                    enabled = !uiState.isCallingContact
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Phone,
                                        contentDescription = "Call Worker",
                                        tint = OldBurgundy
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large Arrival OTP Card per SPEC.md Section A5:
                        // "While accepted: worker card and a large OTP card (share it when the worker arrives)"
                        RozkaamCard(
                            containerColor = Buttermilk,
                            padding = 20.dp
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(OldBurgundy),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Key,
                                        contentDescription = null,
                                        tint = BackgroundLight,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Worker Arrival OTP",
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = OldBurgundy
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Share this 4-digit code ONLY when the worker physically arrives at your task location.",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Large 4-Digit Display
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, OldBurgundy.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        text = uiState.arrivalOtp ?: "••••",
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp,
                                        letterSpacing = 8.sp,
                                        color = OldBurgundy,
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                RozkaamSecondaryButton(
                                    text = "Show Arrival QR Code",
                                    onClick = { viewModel.setQrDialogVisible(true) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    if (uiState.isQrDialogVisible && uiState.qrBitmap != null) {
                        AlertDialog(
                            onDismissRequest = { viewModel.setQrDialogVisible(false) },
                            confirmButton = {
                                RozkaamPrimaryButton(
                                    text = "Close",
                                    onClick = { viewModel.setQrDialogVisible(false) }
                                )
                            },
                            title = {
                                Text(
                                    text = if (uiState.isProofQr) {
                                        "Task Proof Verification QR"
                                    } else {
                                        "Worker Arrival QR Code"
                                    },
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = OldBurgundy
                                )
                            },
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Image(
                                        bitmap = uiState.qrBitmap!!.asImageBitmap(),
                                        contentDescription = "Task Arrival QR Code",
                                        modifier = Modifier
                                            .size(220.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White)
                                            .padding(8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Arrival OTP: ${uiState.arrivalOtp ?: "••••"}",
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = OldBurgundy
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (uiState.isProofQr) {
                                            "The assigned Worker scans this code after submitting photo proof."
                                        } else {
                                            "Worker can scan this QR code or enter the 4-digit code above to verify arrival."
                                        },
                                        fontFamily = NunitoFontFamily,
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            },
                            containerColor = BackgroundLight,
                            shape = RoundedCornerShape(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Task Description Details
                    RozkaamCard(
                        containerColor = Color.White,
                        borderColor = OldBurgundy.copy(alpha = 0.15f),
                        padding = 16.dp
                    ) {
                        Text(
                            text = "Description",
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

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
