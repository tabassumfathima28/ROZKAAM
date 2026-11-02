package com.rozkaam.app.ui.screens.worker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rozkaam.app.ui.components.RozkaamChip
import com.rozkaam.app.ui.components.RozkaamEmptyState
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight

@Composable
fun WorkerTasksScreen(
    onNavigateToTaskDetails: () -> Unit,
    onNavigateToActiveJob: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Active") }

    Scaffold(
        topBar = {
            RozkaamTopBar(
                title = "My Work Tasks",
                canNavigateBack = false
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            // Active / Completed tabs per A5
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                RozkaamChip(
                    label = "Active",
                    selected = selectedTab == "Active",
                    onClick = { selectedTab = "Active" }
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                RozkaamChip(
                    label = "Completed",
                    selected = selectedTab == "Completed",
                    onClick = { selectedTab = "Completed" }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (selectedTab == "Active") {
                    RozkaamEmptyState(
                        title = "No Active Tasks",
                        message = "You have not accepted any tasks yet. Browse Nearby Tasks on Home to accept work.",
                        icon = Icons.AutoMirrored.Outlined.Assignment,
                        actionText = "Inspect Active Job Skeleton",
                        onActionClick = onNavigateToActiveJob
                    )
                } else {
                    RozkaamEmptyState(
                        title = "No Completed Tasks",
                        message = "Tasks you successfully finish and receive poster approval for will be archived here.",
                        icon = Icons.AutoMirrored.Outlined.Assignment
                    )
                }
            }
        }
    }
}
