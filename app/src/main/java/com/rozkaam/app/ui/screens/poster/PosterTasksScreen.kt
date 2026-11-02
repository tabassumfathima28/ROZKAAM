package com.rozkaam.app.ui.screens.poster

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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

private val posterTaskFilters = listOf("Open", "Active", "Awaiting approval", "Completed")

@Composable
fun PosterTasksScreen(
    onNavigateToPostTask: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("Open") }

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
            // Filters per A5: Open, Active, Awaiting approval, Completed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                posterTaskFilters.forEach { filter ->
                    RozkaamChip(
                        label = filter,
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter }
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                RozkaamEmptyState(
                    title = "No $selectedFilter Tasks",
                    message = "You currently have no tasks in '$selectedFilter' status. Create a new task to get started.",
                    icon = Icons.AutoMirrored.Outlined.Assignment,
                    actionText = "Post a Task",
                    onActionClick = onNavigateToPostTask
                )
            }
        }
    }
}
