package com.rozkaam.app.ui.screens.worker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rozkaam.app.ui.components.RozkaamEmptyState
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight

@Composable
fun WorkerTaskDetailsScreen(
    onNavigateBack: () -> Unit,
    onAcceptTask: () -> Unit
) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            RozkaamEmptyState(
                title = "No Task Selected",
                message = "Select an open task from Nearby Tasks or the Map view to inspect details, location distance, poster rating, and to accept.",
                icon = Icons.AutoMirrored.Outlined.Assignment,
                actionText = "Back to Tasks",
                onActionClick = onNavigateBack
            )
        }
    }
}
