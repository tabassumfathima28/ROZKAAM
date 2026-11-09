package com.rozkaam.app.ui.screens.worker

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rozkaam.app.data.model.Task
import com.rozkaam.app.ui.components.RozkaamCard
import com.rozkaam.app.ui.components.RozkaamChip
import com.rozkaam.app.ui.components.RozkaamEmptyState
import com.rozkaam.app.ui.components.RozkaamPrimaryButton
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.screens.poster.getCategoryIcon
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.Buttermilk
import com.rozkaam.app.ui.theme.NunitoFontFamily
import com.rozkaam.app.ui.theme.MutedRed
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.PoppinsFontFamily
import com.rozkaam.app.ui.theme.SuccessGreen
import com.rozkaam.app.ui.theme.TextSecondary
import com.rozkaam.app.ui.viewmodels.WorkerTasksUiState
import com.rozkaam.app.ui.viewmodels.WorkerTasksViewModel

@Composable
fun WorkerTasksScreen(
    onNavigateToTaskDetails: (String) -> Unit,
    onNavigateToActiveJob: (String) -> Unit,
    onNavigateToUploadProof: (String) -> Unit,
    viewModel: WorkerTasksViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RozkaamChip(
                    label = "${WorkerTasksUiState.TAB_ACTIVE} (${uiState.activeTasks.size})",
                    selected = uiState.selectedTab == WorkerTasksUiState.TAB_ACTIVE,
                    onClick = { viewModel.setSelectedTab(WorkerTasksUiState.TAB_ACTIVE) }
                )
                RozkaamChip(
                    label = "${WorkerTasksUiState.TAB_COMPLETED} (${uiState.completedTasks.size})",
                    selected = uiState.selectedTab == WorkerTasksUiState.TAB_COMPLETED,
                    onClick = { viewModel.setSelectedTab(WorkerTasksUiState.TAB_COMPLETED) }
                )
                RozkaamChip(
                    label = "${WorkerTasksUiState.TAB_CANCELLED} (${uiState.cancelledTasks.size})",
                    selected = uiState.selectedTab == WorkerTasksUiState.TAB_CANCELLED,
                    onClick = { viewModel.setSelectedTab(WorkerTasksUiState.TAB_CANCELLED) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = OldBurgundy, strokeWidth = 3.dp)
                        }
                    }
                    uiState.errorMessage != null -> {
                        RozkaamEmptyState(
                            title = "Unable to Load Tasks",
                            message = uiState.errorMessage.orEmpty(),
                            icon = Icons.AutoMirrored.Outlined.Assignment,
                            actionText = "Retry",
                            onActionClick = viewModel::refresh
                        )
                    }
                    uiState.filteredTasks.isEmpty() -> {
                        val titleAndMessage = when (uiState.selectedTab) {
                            WorkerTasksUiState.TAB_COMPLETED -> "No Completed Tasks" to
                                "Tasks you finish and receive poster approval for will appear here."
                            WorkerTasksUiState.TAB_CANCELLED -> "No Cancelled Tasks" to
                                "Tasks cancelled after assignment will appear here."
                            else -> "No Active Tasks" to
                                "Accepted tasks and tasks awaiting poster approval will appear here."
                        }
                        RozkaamEmptyState(
                            title = titleAndMessage.first,
                            message = titleAndMessage.second,
                            icon = Icons.AutoMirrored.Outlined.Assignment
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.filteredTasks, key = { it.id }) { task ->
                                WorkerTaskCard(
                                    task = task,
                                    onClick = { onNavigateToTaskDetails(task.id) },
                                    onNavigateToActiveJob = { onNavigateToActiveJob(task.id) },
                                    onNavigateToUploadProof = { onNavigateToUploadProof(task.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerTaskCard(
    task: Task,
    onClick: () -> Unit,
    onNavigateToActiveJob: () -> Unit,
    onNavigateToUploadProof: () -> Unit
) {
    val statusText = when (task.status) {
        Task.STATUS_ACCEPTED -> "Accepted"
        Task.STATUS_STARTED -> "In Progress"
        Task.STATUS_SUBMITTED -> "Awaiting Poster Approval"
        Task.STATUS_COMPLETED -> "Completed"
        Task.STATUS_CANCELLED -> "Cancelled"
        else -> task.status.replaceFirstChar { it.uppercase() }
    }
    val statusColor = when (task.status) {
        Task.STATUS_SUBMITTED, Task.STATUS_COMPLETED -> SuccessGreen
        Task.STATUS_CANCELLED -> MutedRed
        else -> OldBurgundy
    }

    RozkaamCard(
        containerColor = Color.White,
        borderColor = OldBurgundy.copy(alpha = 0.15f),
        padding = 14.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Buttermilk, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(task.category, task.category),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = OldBurgundy
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = OldBurgundy
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusText,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = statusColor
                )
                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = task.description,
                        fontFamily = NunitoFontFamily,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = task.formattedPrice,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = OldBurgundy
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = TextSecondary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = task.scheduledFor,
                fontFamily = NunitoFontFamily,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        if (task.status == Task.STATUS_ACCEPTED) {
            Spacer(modifier = Modifier.height(12.dp))
            RozkaamPrimaryButton(
                text = "Verify Arrival / Continue",
                onClick = onNavigateToActiveJob,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (task.status == Task.STATUS_STARTED) {
            Spacer(modifier = Modifier.height(12.dp))
            RozkaamPrimaryButton(
                text = "Upload Photo Proof",
                onClick = onNavigateToUploadProof,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
