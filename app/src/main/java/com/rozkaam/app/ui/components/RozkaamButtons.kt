package com.rozkaam.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.OnOldBurgundy
import com.rozkaam.app.ui.theme.PastelBlue
import com.rozkaam.app.ui.theme.PoppinsFontFamily
import com.rozkaam.app.ui.theme.RozkaamButtonShape

/**
 * Shared Component 1: Primary Pill Button
 * Token: Old Burgundy #43302E background, OnOldBurgundy light cream text (#FFFFFAE6),
 * pill shape, subtle elevation, and centered typography.
 * Adheres to SPEC.md Section A4 Design System.
 */
@Composable
fun RozkaamPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    isLoading: Boolean = false
) {
    val textColor = if (enabled && !isLoading) OnOldBurgundy else OnOldBurgundy.copy(alpha = 0.6f)

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
        enabled = enabled && !isLoading,
        shape = RozkaamButtonShape,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp,
            focusedElevation = 3.dp,
            hoveredElevation = 3.dp,
            disabledElevation = 0.dp
        ),
        border = BorderStroke(
            1.dp,
            if (enabled && !isLoading) OldBurgundy.copy(alpha = 0.25f) else Color.Transparent
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = OldBurgundy,
            contentColor = OnOldBurgundy,
            disabledContainerColor = OldBurgundy.copy(alpha = 0.35f),
            disabledContentColor = OnOldBurgundy.copy(alpha = 0.55f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
    ) {
        if (isLoading) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = OnOldBurgundy,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Please wait...",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = OnOldBurgundy,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = textColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.3.sp,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Shared Component 2: Secondary Button
 * Token: Pastel Blue #C1DBE8 background, Old Burgundy #43302E text,
 * pill shape, subtle elevation, and crisp border.
 */
@Composable
fun RozkaamSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val textColor = if (enabled) OldBurgundy else OldBurgundy.copy(alpha = 0.5f)

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
        enabled = enabled,
        shape = RozkaamButtonShape,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 1.dp,
            pressedElevation = 3.dp,
            hoveredElevation = 2.dp,
            disabledElevation = 0.dp
        ),
        border = BorderStroke(
            1.dp,
            if (enabled) OldBurgundy.copy(alpha = 0.18f) else Color.Transparent
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = PastelBlue,
            contentColor = OldBurgundy,
            disabledContainerColor = PastelBlue.copy(alpha = 0.38f),
            disabledContentColor = OldBurgundy.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = textColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.2.sp,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Secondary Outlined Pill Button variant for neutral actions
 */
@Composable
fun RozkaamOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val textColor = if (enabled) OldBurgundy else OldBurgundy.copy(alpha = 0.45f)

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
        enabled = enabled,
        shape = RozkaamButtonShape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = OldBurgundy,
            disabledContentColor = OldBurgundy.copy(alpha = 0.45f)
        ),
        border = BorderStroke(
            1.5.dp,
            if (enabled) OldBurgundy.copy(alpha = 0.85f) else OldBurgundy.copy(alpha = 0.25f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = textColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.2.sp,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
