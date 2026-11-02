package com.rozkaam.app.ui.screens.poster

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rozkaam.app.ui.components.RozkaamEmptyState
import com.rozkaam.app.ui.components.RozkaamSecondaryButton
import com.rozkaam.app.ui.components.RozkaamStepTracker
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight

val postTaskSteps = listOf(
    "What",
    "Where & When",
    "Price & Pay"
)

@Composable
fun PosterPostTaskScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddMoney: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            RozkaamTopBar(
                title = "Post a Task",
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
            // 3-step tracker per A5
            RozkaamStepTracker(
                steps = postTaskSteps,
                currentStepIndex = currentStep
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                RozkaamEmptyState(
                    title = "Post Task Wizard",
                    message = "In Milestone 4, this flow will load real categories from Firestore config/categories, geocode addresses via Google Maps SDK, and check your wallet balance before placing escrow hold.",
                    icon = Icons.Outlined.PostAdd,
                    actionText = "Check Wallet Balance",
                    onActionClick = onNavigateToAddMoney
                )
            }

            // Step progression controls for skeleton inspection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                if (currentStep > 0) {
                    RozkaamSecondaryButton(
                        text = "Previous Step",
                        onClick = { currentStep-- },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                }
                if (currentStep < postTaskSteps.size - 1) {
                    RozkaamSecondaryButton(
                        text = "Next Step (${postTaskSteps[currentStep + 1]})",
                        onClick = { currentStep++ },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
