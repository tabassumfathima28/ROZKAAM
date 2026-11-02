package com.rozkaam.app.ui.screens.worker

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Sos
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rozkaam.app.ui.components.RozkaamCard
import com.rozkaam.app.ui.components.RozkaamChip
import com.rozkaam.app.ui.components.RozkaamEmptyState
import com.rozkaam.app.ui.components.RozkaamSecondaryButton
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.Buttermilk
import com.rozkaam.app.ui.theme.MutedRed
import com.rozkaam.app.ui.theme.NunitoFontFamily
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.PastelBlue
import com.rozkaam.app.ui.theme.PoppinsFontFamily
import com.rozkaam.app.ui.theme.TextSecondary

@Composable
fun WorkerHomeScreen(
    onNavigateToTaskDetails: () -> Unit,
    onNavigateToActiveJob: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSos: () -> Unit
) {
    var isFreeNow by remember { mutableStateOf(false) }
    var isMapView by remember { mutableStateOf(false) }
    var selectedRadiusKm by remember { mutableStateOf(5) }

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
                        text = "Namaste, Worker",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = OldBurgundy
                    )
                    Text(
                        text = "Looking for micro-tasks today?",
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
            // Free Now Toggle Tile
            RozkaamCard(
                containerColor = Buttermilk
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "I'm Free Now",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = OldBurgundy
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isFreeNow) "Ready to receive nearby tasks ($selectedRadiusKm km)" else "Turn on to get notified of nearby gigs",
                            fontFamily = NunitoFontFamily,
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = isFreeNow,
                        onCheckedChange = { isFreeNow = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BackgroundLight,
                            checkedTrackColor = OldBurgundy,
                            uncheckedThumbColor = OldBurgundy,
                            uncheckedTrackColor = PastelBlue
                        )
                    )
                }

                if (isFreeNow) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Radius:",
                            fontFamily = PoppinsFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = OldBurgundy
                        )
                        listOf(3, 5, 10).forEach { radius ->
                            RozkaamChip(
                                label = "$radius km",
                                selected = selectedRadiusKm == radius,
                                onClick = { selectedRadiusKm = radius }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Daily Goal & List/Map Toggle Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nearby Tasks",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OldBurgundy
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RozkaamChip(
                        label = "List",
                        icon = Icons.AutoMirrored.Outlined.ListAlt,
                        selected = !isMapView,
                        onClick = { isMapView = false }
                    )
                    RozkaamChip(
                        label = "Map",
                        icon = Icons.Outlined.Map,
                        selected = isMapView,
                        onClick = { isMapView = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Real Empty State per A2
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                RozkaamEmptyState(
                    title = "No Tasks Nearby",
                    message = if (isFreeNow) {
                        "No tasks currently open within $selectedRadiusKm km. We will notify you immediately when a new task is posted."
                    } else {
                        "Turn on 'I'm Free Now' to start seeing tasks posted in your neighborhood."
                    },
                    icon = Icons.Outlined.Explore,
                    actionText = if (!isFreeNow) "Turn On Availability" else "Inspect Active Job",
                    onActionClick = {
                        if (!isFreeNow) isFreeNow = true else onNavigateToActiveJob()
                    }
                )
            }

            // Quick navigation helpers for Milestone 1 evaluation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RozkaamSecondaryButton(
                    text = "Task Details",
                    onClick = onNavigateToTaskDetails,
                    modifier = Modifier.weight(1f)
                )
                RozkaamSecondaryButton(
                    text = "Active Job",
                    onClick = onNavigateToActiveJob,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
