package com.rozkaam.app.ui.screens.poster

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rozkaam.app.data.model.Task
import com.rozkaam.app.ui.components.RozkaamCard
import com.rozkaam.app.ui.components.RozkaamChip
import com.rozkaam.app.ui.components.RozkaamEmptyState
import com.rozkaam.app.ui.components.RozkaamPrimaryButton
import com.rozkaam.app.ui.components.RozkaamTopBar
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.Buttermilk
import com.rozkaam.app.ui.theme.NunitoFontFamily
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.PastelBlue
import com.rozkaam.app.ui.theme.PoppinsFontFamily
import com.rozkaam.app.ui.theme.SuccessGreen
import com.rozkaam.app.ui.theme.TextSecondary
import com.rozkaam.app.ui.viewmodels.PosterTasksViewModel

private val posterTaskFilterKeys = listOf("Open", "Active", "Awaiting approval", "Completed")

@Composable
fun PosterTasksScreen(
    onNavigateToPostTask: () -> Unit,
    onNavigateToTaskDetails: (String) -> Unit = {},
    viewModel: PosterTasksViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            RozkaamTopBar(
                title = "My Posted Tasks",
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
            // Filters per A5: Open, Active, Awaiting approval, Completed with live counts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                posterTaskFilterKeys.forEach { filter ->
                    val count = when (filter) {
                        "Open" -> uiState.openCount
                        "Active" -> uiState.activeCount
                        "Awaiting approval" -> uiState.awaitingApprovalCount
                        "Completed" -> uiState.completedCount
                        else -> 0
                    }

                    RozkaamChip(
                        label = "$filter ($count)",
                        selected = uiState.selectedFilter == filter,
                        onClick = { viewModel.setFilter(filter) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                    uiState.filteredTasks.isEmpty() -> {
                        RozkaamEmptyState(
                            title = "No ${uiState.selectedFilter} Tasks",
                            message = if (uiState.tasks.isEmpty()) {
                                "You haven't posted any tasks yet. Create a task now to get help from nearby workers."
                            } else {
                                "You have no tasks currently in '${uiState.selectedFilter}' status."
                            },
                            icon = Icons.AutoMirrored.Outlined.Assignment,
                            actionText = "Post a Task",
                            onActionClick = onNavigateToPostTask
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.filteredTasks, key = { it.id }) { task ->
                                PosterTaskCard(
                                    task = task,
                                    onClick = { onNavigateToTaskDetails(task.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Poster task card displaying category icon, title, status pill, price, and schedule.
 */
@Composable
fun PosterTaskCard(
    task: Task,
    onClick: () -> Unit
) {
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
            // Category Icon Tile
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Buttermilk),
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
                    color = OldBurgundy,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when (task.status) {
                            Task.STATUS_OPEN -> PastelBlue.copy(alpha = 0.6f)
                            Task.STATUS_ACCEPTED, Task.STATUS_STARTED -> Buttermilk
                            Task.STATUS_COMPLETED -> SuccessGreen.copy(alpha = 0.15f)
                            else -> Color.Black.copy(alpha = 0.08f)
                        }
                    ) {
                        Text(
                            text = task.status.replaceFirstChar { it.uppercase() },
                            fontFamily = NunitoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (task.status) {
                                Task.STATUS_COMPLETED -> SuccessGreen
                                else -> OldBurgundy
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    // Schedule Tag
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = task.scheduledFor,
                            fontFamily = NunitoFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Price Tag
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = PastelBlue
            ) {
                Text(
                    text = task.formattedPrice,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = OldBurgundy,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
