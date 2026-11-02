package com.rozkaam.app.ui.screens.poster

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rozkaam.app.ui.components.RozkaamEmptyState
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight

@Composable
fun PosterTaskDetailsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            RozkaamTopBar(
                title = "Posted Task Details",
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
                message = "Select one of your posted tasks to view worker status, reveal your 4-digit arrival OTP card, review photo proof, and approve escrow release.",
                icon = Icons.Outlined.TaskAlt,
                actionText = "Back to Home",
                onActionClick = onNavigateBack
            )
        }
    }
}
