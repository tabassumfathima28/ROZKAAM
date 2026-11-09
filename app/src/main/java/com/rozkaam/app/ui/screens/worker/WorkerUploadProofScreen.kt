package com.rozkaam.app.ui.screens.worker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.rozkaam.app.ui.components.RozkaamPrimaryButton
import com.rozkaam.app.ui.components.RozkaamSecondaryButton
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.MutedRed
import com.rozkaam.app.ui.theme.NunitoFontFamily
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.PoppinsFontFamily
import com.rozkaam.app.ui.theme.RozkaamCardShape
import com.rozkaam.app.ui.theme.SuccessGreen
import com.rozkaam.app.ui.theme.TextSecondary
import com.rozkaam.app.ui.viewmodels.WorkerUploadProofViewModel

@Composable
fun WorkerUploadProofScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    onNavigateToActiveJob: (String) -> Unit,
    onNavigateToProofQrScanner: (String) -> Unit,
    viewModel: WorkerUploadProofViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.selectImage(uri, context.contentResolver.getType(uri))
        }
    }

    LaunchedEffect(uiState.shouldNavigateToScanner) {
        if (uiState.shouldNavigateToScanner) {
            viewModel.onScannerNavigationStarted()
            onNavigateToProofQrScanner(taskId)
        }
    }

    Scaffold(
        topBar = {
            RozkaamTopBar(
                title = "Upload Photo Proof",
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
            uiState.isSubmitted -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Proof Submitted",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = OldBurgundy
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your task photo and description were submitted. Scan the poster's proof QR code to verify this task.",
                        fontFamily = NunitoFontFamily,
                        fontSize = 15.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    RozkaamPrimaryButton(
                        text = "Scan Task QR Code",
                        onClick = { onNavigateToProofQrScanner(taskId) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    RozkaamSecondaryButton(
                        text = "Back",
                        onClick = onNavigateBack
                    )
                }
            }
            uiState.isAuthorized && uiState.task?.isAccepted == true -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.task?.title.orEmpty(),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = OldBurgundy
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Verify your arrival with the poster's OTP before uploading completion proof.",
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    RozkaamPrimaryButton(
                        text = "Verify Arrival OTP",
                        onClick = { onNavigateToActiveJob(taskId) }
                    )
                }
            }
            uiState.isAuthorized && uiState.task?.isStarted == true -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .imePadding()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = uiState.task?.title.orEmpty(),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = OldBurgundy
                        )
                        Text(
                            text = "Add a clear photo of the completed work and describe what you finished.",
                            fontFamily = NunitoFontFamily,
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SuccessGreen.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "Arrival verified — submit proof after completing the work.",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = SuccessGreen
                            )
                        }

                        uiState.errorMessage?.let { message ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MutedRed.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MutedRed.copy(alpha = 0.3f)
                                )
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
                                    Text(
                                        text = message,
                                        fontFamily = NunitoFontFamily,
                                        fontSize = 13.sp,
                                        color = MutedRed,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(230.dp)
                                .clip(RozkaamCardShape)
                                .background(Color.White)
                                .border(1.dp, OldBurgundy.copy(alpha = 0.18f), RozkaamCardShape)
                                .clickable(enabled = !uiState.isSubmitting) {
                                    photoPicker.launch(arrayOf("image/jpeg", "image/png"))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val selectedImage = uiState.selectedImage
                            if (selectedImage == null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Outlined.AddAPhoto,
                                        contentDescription = null,
                                        tint = OldBurgundy,
                                        modifier = Modifier.size(42.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Choose a completion photo",
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OldBurgundy
                                    )
                                    Text(
                                        text = "Tap to select a JPEG or PNG image",
                                        fontFamily = NunitoFontFamily,
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            } else {
                                AsyncImage(
                                    model = selectedImage,
                                    contentDescription = "Selected task completion photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        RozkaamSecondaryButton(
                            text = if (uiState.selectedImage == null) "Choose Photo" else "Change Photo",
                            onClick = { photoPicker.launch(arrayOf("image/jpeg", "image/png")) },
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.description,
                            onValueChange = viewModel::updateDescription,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Completion Description") },
                            placeholder = { Text("Describe the work you completed") },
                            supportingText = {
                                Text(
                                    text = "${uiState.description.length}/500 characters",
                                    fontFamily = NunitoFontFamily
                                )
                            },
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OldBurgundy,
                                unfocusedBorderColor = OldBurgundy.copy(alpha = 0.3f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    RozkaamPrimaryButton(
                        text = "Submit Proof",
                        onClick = viewModel::submitProof,
                        enabled = uiState.canSubmit,
                        isLoading = uiState.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.errorMessage ?: "This task is not awaiting proof.",
                        fontFamily = NunitoFontFamily,
                        fontSize = 15.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    RozkaamSecondaryButton(
                        text = "Back",
                        onClick = onNavigateBack
                    )
                }
            }
        }
    }
}
