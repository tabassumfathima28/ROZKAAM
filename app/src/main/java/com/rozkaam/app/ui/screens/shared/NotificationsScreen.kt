package com.rozkaam.app.ui.screens.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rozkaam.app.ui.components.RozkaamEmptyState
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight

@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            RozkaamTopBar(
                title = "Notifications",
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
                title = "No Notifications",
                message = "You have no unread alerts. You will receive notifications here whenever nearby tasks match your radius or a task status updates.",
                icon = Icons.Outlined.NotificationsNone,
                actionText = "Back",
                onActionClick = onNavigateBack
            )
        }
    }
}
