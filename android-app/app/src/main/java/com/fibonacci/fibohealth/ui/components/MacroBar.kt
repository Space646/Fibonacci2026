package com.fibonacci.fibohealth.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fibonacci.fibohealth.ui.theme.*

@Composable
fun MacroBar(label: String, grams: Float?, maxGrams: Float = 50f, color: Color) {
    grams ?: return
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text("${grams}g", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress    = { (grams / maxGrams).coerceIn(0f, 1f) },
            modifier    = Modifier.fillMaxWidth().height(4.dp),
            color       = color,
            trackColor  = MaterialTheme.colorScheme.outline
        )
    }
}
