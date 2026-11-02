package com.rozkaam.app.ui.screens.worker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rozkaam.app.ui.components.RozkaamEmptyState
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight

@Composable
fun WorkerWithdrawScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            RozkaamTopBar(
                title = "Withdraw Earnings",
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
                title = "Zero Available Balance",
                message = "Your current available balance is ₹0. Complete nearby micro-tasks to earn money before requesting a UPI withdrawal.",
                icon = Icons.Outlined.AccountBalance,
                actionText = "Back to Earnings",
                onActionClick = onNavigateBack
            )
        }
    }
}
