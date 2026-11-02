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
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rozkaam.app.ui.components.RozkaamEmptyState
import com.rozkaam.app.ui.components.RozkaamSecondaryButton
import com.rozkaam.app.ui.components.RozkaamStepTracker
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight

val activeJobSteps = listOf(
    "Accepted",
    "Arrived & OTP",
    "Working",
    "Photo Proof",
    "Paid"
)

@Composable
fun WorkerActiveJobScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUploadProof: () -> Unit,
    onNavigateToJobCompleted: () -> Unit
) {
    Scaffold(
        topBar = {
            RozkaamTopBar(
                title = "Active Job",
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
                .padding(horizontal = 20.dp)
        ) {
            // 5-step tracker as defined in A5
            RozkaamStepTracker(
                steps = activeJobSteps,
                currentStepIndex = 0
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                RozkaamEmptyState(
                    title = "No Active Job",
                    message = "You do not have any job currently underway. Accept a task from the Home screen to enter arrival OTP, track progress, and submit photo proof.",
                    icon = Icons.Outlined.WorkOutline,
                    actionText = "Find Open Tasks",
                    onActionClick = onNavigateBack
                )
            }

            // Navigation helpers to inspect sub-screens
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                RozkaamSecondaryButton(
                    text = "Upload Proof Screen",
                    onClick = onNavigateToUploadProof,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                RozkaamSecondaryButton(
                    text = "Job Completed Screen",
                    onClick = onNavigateToJobCompleted,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
