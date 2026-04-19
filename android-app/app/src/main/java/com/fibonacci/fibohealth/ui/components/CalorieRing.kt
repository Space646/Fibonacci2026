package com.fibonacci.fibohealth.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fibonacci.fibohealth.ui.theme.*

@Composable
fun CalorieRing(
    remaining: Int,
    goal: Int,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    strokeWidth: Dp = 16.dp
) {
    val darkTheme = isSystemInDarkTheme()
    val trackColor = if (darkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
    val subtitleColor = if (darkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    val fraction = if (goal > 0) (1f - remaining.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(fraction, tween(800), label = "ring")
    val color = when {
        fraction < 0.5f  -> StatusGreen
        fraction < 0.75f -> Indigo
        fraction < 0.9f  -> StatusAmber
        else             -> StatusRed
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset  = strokeWidth.toPx() / 2
            val rect   = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            drawArc(trackColor, -90f, 360f, false,
                topLeft = Offset(inset, inset), size = rect, style = stroke)
            drawArc(color, -90f, animated * 360f, false,
                topLeft = Offset(inset, inset), size = rect, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = remaining.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = "kcal left",
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                color = subtitleColor
            )
            Text(
                text = "of ${goal.formatK()} goal",
                fontSize = 10.sp,
                color = subtitleColor
            )
        }
    }
}

private fun Int.formatK(): String = if (this >= 1000) "${this / 1000},${(this % 1000).toString().padStart(3, '0')}" else toString()
