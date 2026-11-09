package com.rozkaam.app.ui.screens.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rozkaam.app.data.model.AppNotification
import com.rozkaam.app.ui.components.RozkaamCard
import com.rozkaam.app.ui.components.RozkaamEmptyState
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.Buttermilk
import com.rozkaam.app.ui.theme.NunitoFontFamily
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.PoppinsFontFamily
import com.rozkaam.app.ui.theme.TextSecondary
import com.rozkaam.app.ui.viewmodels.NotificationsViewModel

@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OldBurgundy)
            }
            uiState.errorMessage != null -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) {
                RozkaamEmptyState(
                    title = "Unable to Load Notifications",
                    message = uiState.errorMessage.orEmpty(),
                    icon = Icons.Outlined.NotificationsNone,
                    actionText = "Back",
                    onActionClick = onNavigateBack
                )
            }
            uiState.notifications.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) {
                RozkaamEmptyState(
                    title = "No Notifications",
                    message = "Task updates will appear here.",
                    icon = Icons.Outlined.NotificationsNone,
                    actionText = "Back",
                    onActionClick = onNavigateBack
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.notifications, key = AppNotification::id) { notification ->
                    NotificationCard(
                        notification = notification,
                        onClick = { viewModel.markAsRead(notification.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: AppNotification, onClick: () -> Unit) {
    RozkaamCard(
        containerColor = if (notification.read) BackgroundLight else Buttermilk,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = OldBurgundy
                )
                Text(
                    text = notification.message,
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                if (notification.formattedDate.isNotBlank()) {
                    Text(
                        text = notification.formattedDate,
                        fontFamily = NunitoFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
            if (!notification.read) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = OldBurgundy
                ) {
                    Text(
                        text = "NEW",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BackgroundLight
                    )
                }
            }
        }
    }
}
