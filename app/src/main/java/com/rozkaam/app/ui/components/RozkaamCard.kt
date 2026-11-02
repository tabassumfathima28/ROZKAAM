package com.rozkaam.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rozkaam.app.ui.theme.Buttermilk
import com.rozkaam.app.ui.theme.OutlineColor
import com.rozkaam.app.ui.theme.RozkaamCardShape

/**
 * Shared Component 3: Card
 * Per A4: Cards use a 24dp corner radius.
 * Token: Highlight Buttermilk #FFF1B5 for cards and highlights.
 */
@Composable
fun RozkaamCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Buttermilk,
    borderColor: Color? = null,
    elevation: Dp = 0.dp,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clip(RozkaamCardShape)
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }

    Surface(
        modifier = baseModifier,
        shape = RozkaamCardShape,
        color = containerColor,
        tonalElevation = elevation,
        shadowElevation = elevation,
        border = borderColor?.let { BorderStroke(1.dp, it) }
    ) {
        Column(
            modifier = Modifier.padding(padding),
            content = content
        )
    }
}
