package com.fibonacci.fibohealth.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibonacci.fibohealth.ui.components.*
import com.fibonacci.fibohealth.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(vm: DashboardViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val goal = state.profile.dailyCalorieGoal ?: state.profile.calculatedDailyGoal
    val remaining = (goal - (state.sessionState?.caloriesConsumed ?: 0f)).toInt()

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Good morning, ${state.profile.name}",
                style = MaterialTheme.typography.headlineMedium)
        }
        item {  // BLE status pill
            SuggestionChip(
                onClick = {},
                label   = { Text(if (state.isConnected) "Pi Connected" else "Pi Disconnected") },
                colors  = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = if (state.isConnected) StatusGreen.copy(0.15f)
                                     else MaterialTheme.colorScheme.surface
                )
            )
        }
        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CalorieRing(remaining = remaining.coerceAtLeast(0), goal = goal)
            }
        }
        item {  // Stats strip
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("${state.profile.calculatedDailyGoal}", "Goal kcal", Indigo, Modifier.weight(1f))
                StatCard("${(state.sessionState?.caloriesConsumed ?: 0f).toInt()}", "Eaten", Cyan, Modifier.weight(1f))
                StatCard("${remaining.coerceAtLeast(0)}", "Left", StatusGreen, Modifier.weight(1f))
            }
        }
        if (state.recentScans.isNotEmpty()) {
            item { Text("Recent Scans", style = MaterialTheme.typography.titleLarge) }
            items(state.recentScans) { entry ->
                ListItem(
                    headlineContent   = { Text(entry.foodName) },
                    supportingContent = { Text("${entry.weightG}g · ${entry.calories.toInt()} kcal") },
                    trailingContent   = { HealthBadge(entry.isHealthy) }
                )
                HorizontalDivider()
            }
        }
    }
}
