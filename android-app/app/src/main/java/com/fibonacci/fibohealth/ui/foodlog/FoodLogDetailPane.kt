package com.fibonacci.fibohealth.ui.foodlog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fibonacci.fibohealth.data.model.FoodLogEntry
import com.fibonacci.fibohealth.ui.components.*
import com.fibonacci.fibohealth.ui.theme.*

@Composable
fun FoodLogDetailPane(entry: FoodLogEntry, modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(entry.foodName, style = MaterialTheme.typography.headlineMedium)
        Text("${entry.weightG}g · ${entry.timestamp.take(10)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("${entry.calories.toInt()}", "Calories", Indigo, Modifier.weight(1f))
            HealthBadge(entry.isHealthy)
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Macros", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                MacroBar("Protein", entry.proteinG, color = Indigo)
                MacroBar("Fat",     entry.fatG,     color = StatusAmber)
                MacroBar("Sugar",   entry.sugarG,   color = Cyan)
                MacroBar("Fiber",   entry.fiberG,   color = StatusGreen)
            }
        }
    }
}
