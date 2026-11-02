package com.rozkaam.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rozkaam.app.ui.theme.BackgroundLight
import com.rozkaam.app.ui.theme.Buttermilk
import com.rozkaam.app.ui.theme.OldBurgundy
import com.rozkaam.app.ui.theme.OutlineColor
import com.rozkaam.app.ui.theme.PastelBlue
import com.rozkaam.app.ui.theme.PoppinsFontFamily

/**
 * 6-digit OTP verification boxes per SPEC.md Section A5:
 * "Firebase sends a 6-digit code, so use 6 boxes (the mockup shows 4), with a 30 second resend timer."
 */
@Composable
fun RozkaamOtpInput(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    focusRequester: FocusRequester? = null,
    onComplete: (() -> Unit)? = null
) {
    BasicTextField(
        value = otpValue,
        onValueChange = { newValue ->
            val digitsOnly = newValue.filter { it.isDigit() }.take(length)
            onOtpChange(digitsOnly)
            if (digitsOnly.length == length && onComplete != null) {
                onComplete()
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        cursorBrush = SolidColor(Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
            ),
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                for (i in 0 until length) {
                    val char = otpValue.getOrNull(i)?.toString() ?: ""
                    val isCurrentFocus = i == otpValue.length
                    val isFilled = i < otpValue.length

                    val boxBorderColor = when {
                        isCurrentFocus -> OldBurgundy
                        isFilled -> OldBurgundy.copy(alpha = 0.7f)
                        else -> OutlineColor
                    }

                    val boxBgColor = when {
                        isFilled -> Buttermilk
                        isCurrentFocus -> BackgroundLight
                        else -> BackgroundLight
                    }

                    Box(
                        modifier = Modifier
                            .size(width = 46.dp, height = 54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(boxBgColor)
                            .border(
                                width = if (isCurrentFocus) 2.dp else 1.dp,
                                color = boxBorderColor,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = OldBurgundy,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    )
}
