package com.rozkaam.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.Buttermilk
import com.rozkaam.app.ui.theme.MutedGreen
import com.rozkaam.app.ui.theme.NunitoFontFamily
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.OutlineColor
import com.rozkaam.app.ui.theme.PastelBlue
import com.rozkaam.app.ui.theme.PoppinsFontFamily

/**
 * Shared Component 10: Step Tracker
 * Used in:
 * - Poster Post a Task (3 steps: What, Where & When, Price & Pay)
 * - Worker Active Job (5 steps: Accepted, Arrived & OTP, Working, Photo Proof, Paid)
 */
@Composable
fun RozkaamStepTracker(
    steps: List<String>,
    currentStepIndex: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { index, title ->
                val isCompleted = index < currentStepIndex
                val isCurrent = index == currentStepIndex
                val isUpcoming = index > currentStepIndex

                val circleBg = when {
                    isCompleted -> MutedGreen
                    isCurrent -> OldBurgundy
                    else -> PastelBlue.copy(alpha = 0.5f)
                }

                val circleContentColor = when {
                    isCompleted || isCurrent -> BackgroundLight
                    else -> OldBurgundy
                }

                // Step Circle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(circleBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            modifier = Modifier.size(18.dp),
                            tint = circleContentColor
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = circleContentColor
                        )
                    }
                }

                // Connector line between steps
                if (index < steps.size - 1) {
                    val lineColor = if (index < currentStepIndex) MutedGreen else OutlineColor
                    HorizontalDivider(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        thickness = 2.dp,
                        color = lineColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Current Step Label
        if (currentStepIndex in steps.indices) {
            Text(
                text = "Step ${currentStepIndex + 1}: ${steps[currentStepIndex]}",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = OldBurgundy,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
