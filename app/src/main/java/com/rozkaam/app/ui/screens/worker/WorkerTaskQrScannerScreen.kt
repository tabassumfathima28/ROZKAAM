package com.rozkaam.app.ui.screens.worker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rozkaam.app.ui.components.RozkaamCard
import com.rozkaam.app.ui.components.RozkaamPrimaryButton
import com.rozkaam.app.ui.components.RozkaamSecondaryButton
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.MutedRed
import com.rozkaam.app.ui.theme.NunitoFontFamily
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.PoppinsFontFamily
import com.rozkaam.app.ui.theme.SuccessGreen
import com.rozkaam.app.ui.viewmodels.WorkerProofQrViewModel
import com.rozkaam.app.utils.QrVerificationHelper
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun WorkerTaskQrScannerScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    onNavigateToTasks: () -> Unit,
    viewModel: WorkerProofQrViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val scannerHandled = remember { AtomicBoolean(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraController = remember(context) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
    }

    LaunchedEffect(uiState.isVerifying, uiState.isVerified, uiState.errorMessage) {
        if (!uiState.isVerifying && !uiState.isVerified) {
            if (uiState.errorMessage != null) kotlinx.coroutines.delay(1200)
            scannerHandled.set(false)
        }
    }

    DisposableEffect(cameraController, lifecycleOwner, cameraPermissionGranted, uiState.isVerified) {
        if (cameraPermissionGranted && !uiState.isVerified) {
            cameraController.setImageAnalysisAnalyzer(cameraExecutor) { image ->
                try {
                    val payload = QrVerificationHelper.decodeImageProxy(image)
                    if (payload != null && scannerHandled.compareAndSet(false, true)) {
                        mainHandler.post { viewModel.verifyScannedQr(taskId, payload) }
                    }
                } finally {
                    image.close()
                }
            }
            cameraController.bindToLifecycle(lifecycleOwner)
        }
        onDispose {
            cameraController.clearImageAnalysisAnalyzer()
            cameraController.unbind()
        }
    }

    DisposableEffect(cameraExecutor) {
        onDispose { cameraExecutor.shutdown() }
    }

    Scaffold(
        topBar = {
            RozkaamTopBar(
                title = "Verify Task QR",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (uiState.isVerified) {
                RozkaamCard(
                    containerColor = SuccessGreen.copy(alpha = 0.12f),
                    borderColor = SuccessGreen.copy(alpha = 0.3f),
                    padding = 18.dp
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = "Proof Submitted and QR Verified",
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = SuccessGreen
                    )
                    Text(
                        text = "The backend verified this QR code for task $taskId. The Poster can now review your submitted photo and description.",
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        color = OldBurgundy
                    )
                }
                RozkaamPrimaryButton(
                    text = "Back to Tasks",
                    onClick = onNavigateToTasks
                )
            } else {
                RozkaamCard(
                    containerColor = SuccessGreen.copy(alpha = 0.1f),
                    padding = 14.dp
                ) {
                    Text(
                        text = "Proof submitted successfully",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SuccessGreen
                    )
                    Text(
                        text = "Scan the proof QR code displayed by the Poster for this task. Verification is checked securely by Firebase.",
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        color = OldBurgundy
                    )
                }

                uiState.errorMessage?.let { message ->
                    RozkaamCard(containerColor = MutedRed.copy(alpha = 0.1f), padding = 12.dp) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MutedRed
                        )
                        Text(
                            text = message,
                            fontFamily = NunitoFontFamily,
                            fontSize = 14.sp,
                            color = MutedRed
                        )
                    }
                }

                if (cameraPermissionGranted) {
                    AndroidView(
                        factory = { viewContext ->
                            PreviewView(viewContext).apply {
                                controller = cameraController
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                    )
                } else {
                    RozkaamCard(containerColor = Color.White, padding = 16.dp) {
                        Text(
                            text = "Camera permission is needed to scan the Poster QR code.",
                            fontFamily = NunitoFontFamily,
                            fontSize = 14.sp,
                            color = OldBurgundy
                        )
                        RozkaamPrimaryButton(
                            text = "Allow Camera",
                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                        )
                    }
                }

                if (uiState.isVerifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = OldBurgundy
                    )
                    Text(
                        text = "Validating QR code for this task...",
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        color = OldBurgundy
                    )
                } else {
                    RozkaamSecondaryButton(
                        text = "Back",
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
