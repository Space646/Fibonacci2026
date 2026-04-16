package com.fibonacci.fibohealth.ui.activity

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibonacci.fibohealth.ui.components.StatCard
import com.fibonacci.fibohealth.ui.theme.*

@Composable
fun ActivityScreen(vm: ActivityViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val permLauncher = rememberLauncherForActivityResult(
        contract = androidx.health.connect.client.PermissionController
            .createRequestPermissionResultContract()
    ) { vm.refresh() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Activity", style = MaterialTheme.typography.headlineMedium)
        if (!state.hasPermission) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Health Connect access required",
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        permLauncher.launch(vm.permissions)
                    }) { Text("Grant Access") }
                }
            }
        } else {
            LazyVerticalGrid(GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { StatCard("${state.snapshot.steps}", "Steps", Cyan, Modifier.fillMaxWidth()) }
                item { StatCard("${state.snapshot.activeMinutes}m", "Active", StatusGreen, Modifier.fillMaxWidth()) }
                item { StatCard("${state.snapshot.caloriesBurned.toInt()}", "Burned kcal", StatusAmber, Modifier.fillMaxWidth()) }
                item { StatCard("${state.snapshot.workouts}", "Workouts", Indigo, Modifier.fillMaxWidth()) }
            }
        }
    }
}
