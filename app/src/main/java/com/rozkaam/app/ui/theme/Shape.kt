package com.rozkaam.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val RozkaamCardShape = RoundedCornerShape(24.dp)
val RozkaamButtonShape = CircleShape
val RozkaamChipShape = CircleShape
val RozkaamBottomSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

val Shapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RozkaamCardShape,
    extraLarge = CircleShape
)
