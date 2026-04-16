package com.fibonacci.fibohealth.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
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
    val fraction = if (goal > 0) (1f - remaining.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(fraction, tween(800), label = "ring")
    val color = when {
        fraction < 0.5f -> StatusGreen
        fraction < 0.75f -> Indigo
        fraction < 0.9f -> StatusAmber
        else -> StatusRed
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2
            val rect = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            drawArc(Color.Gray.copy(alpha = 0.2f), -90f, 360f, false,
                topLeft = Offset(inset, inset), size = rect, style = stroke)
            drawArc(color, -90f, animated * 360f, false,
                topLeft = Offset(inset, inset), size = rect, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$remaining", fontSize = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            Text("kcal left", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
