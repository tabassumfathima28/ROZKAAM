package com.rozkaam.app.ui.screens.worker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rozkaam.app.ui.components.RozkaamEmptyState
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight

@Composable
fun WorkerJobCompletedScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            RozkaamTopBar(
                title = "Job Completed",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            RozkaamEmptyState(
                title = "No Completed Jobs Yet",
                message = "Once the poster approves your photo proof, escrowed payment will be credited immediately to your available wallet balance and shown here.",
                icon = Icons.Outlined.CheckCircleOutline,
                actionText = "Back",
                onActionClick = onNavigateBack
            )
        }
    }
}
