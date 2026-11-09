package com.rozkaam.app.ui.screens.poster

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Sos
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rozkaam.app.ui.components.RozkaamCard
import com.rozkaam.app.ui.components.RozkaamEmptyState
import com.rozkaam.app.ui.components.RozkaamPrimaryButton
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.Buttermilk
import com.rozkaam.app.ui.theme.MutedRed
import com.rozkaam.app.ui.theme.NunitoFontFamily
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.PastelBlue
import com.rozkaam.app.ui.theme.PoppinsFontFamily
import com.rozkaam.app.ui.theme.TextSecondary
import com.rozkaam.app.ui.viewmodels.PosterTasksViewModel

@Composable
fun PosterHomeScreen(
    onNavigateToPostTask: () -> Unit,
    onNavigateToTaskDetails: (String) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSos: () -> Unit,
    viewModel: PosterTasksViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundLight)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Namaste, Poster",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = OldBurgundy
                    )
                    Text(
                        text = "Need something done today?",
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateToSos,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MutedRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Sos,
                                contentDescription = "SOS",
                                tint = MutedRed
                            )
                        }
                    }

                    IconButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier.size(48.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = MutedRed) {
                                    Text("0", color = BackgroundLight)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = OldBurgundy
                            )
                        }
                    }
                }
            }
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            // Live Summary Cards per SPEC.md Section A5: Active, Awaiting Approval, Completed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Active Card
                RozkaamCard(
                    containerColor = Buttermilk,
                    modifier = Modifier.weight(1f),
                    padding = 12.dp
                ) {
                    Text(
                        text = "Active",
                        fontFamily = NunitoFontFamily,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = uiState.activeCount.toString(),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = OldBurgundy
                    )
                }

                // Awaiting Approval Card
                RozkaamCard(
                    containerColor = PastelBlue,
                    modifier = Modifier.weight(1.2f),
                    padding = 12.dp
                ) {
                    Text(
                        text = "Awaiting",
                        fontFamily = NunitoFontFamily,
                        fontSize = 13.sp,
                        color = OldBurgundy
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = uiState.awaitingApprovalCount.toString(),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = OldBurgundy
                    )
                }

                // Completed Card
                RozkaamCard(
                    containerColor = Buttermilk,
                    modifier = Modifier.weight(1f),
                    padding = 12.dp
                ) {
                    Text(
                        text = "Done",
                        fontFamily = NunitoFontFamily,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = uiState.completedCount.toString(),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = OldBurgundy
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Pill Button: Post a Task
            RozkaamPrimaryButton(
                text = "Post a Task",
                icon = Icons.Outlined.Add,
                onClick = onNavigateToPostTask
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Recent Tasks",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = OldBurgundy
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.tasks.isEmpty()) {
                    RozkaamEmptyState(
                        title = "No Tasks Posted Yet",
                        message = "Tap 'Post a Task' above to define a micro-task, choose your location and price, and get matched with nearby verified workers.",
                        icon = Icons.AutoMirrored.Outlined.Assignment,
                        actionText = "Post Your First Task",
                        onActionClick = onNavigateToPostTask
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.tasks.take(5), key = { it.id }) { task ->
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
