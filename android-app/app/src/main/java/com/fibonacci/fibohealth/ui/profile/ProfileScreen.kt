package com.fibonacci.fibohealth.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibonacci.fibohealth.data.model.UserProfile
import com.fibonacci.fibohealth.ui.theme.StatusRed

@Composable
fun ProfileScreen(vm: ProfileViewModel = hiltViewModel()) {
    val profile     by vm.profile.collectAsStateWithLifecycle()
    val hcEnabled   by vm.hcLoggingEnabled.collectAsStateWithLifecycle()
    var draft       by remember(profile) { mutableStateOf(profile) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(draft.name, { draft = draft.copy(name = it) },
            label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(draft.age.toString(), { draft = draft.copy(age = it.toIntOrNull() ?: draft.age) },
            label = { Text("Age") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(draft.weightKg.toString(), { draft = draft.copy(weightKg = it.toFloatOrNull() ?: draft.weightKg) },
            label = { Text("Weight (kg)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(draft.heightCm.toString(), { draft = draft.copy(heightCm = it.toFloatOrNull() ?: draft.heightCm) },
            label = { Text("Height (cm)") }, modifier = Modifier.fillMaxWidth())

        // Sex picker
        var sexExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(sexExpanded, { sexExpanded = it }) {
            OutlinedTextField(draft.sex, {}, readOnly = true, label = { Text("Sex") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sexExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(sexExpanded, { sexExpanded = false }) {
                listOf("male", "female").forEach { opt ->
                    DropdownMenuItem({ Text(opt) }, { draft = draft.copy(sex = opt); sexExpanded = false })
                }
            }
        }

        // Activity level picker
        var actExpanded by remember { mutableStateOf(false) }
        val actLevels = listOf("sedentary","light","moderate","active","very_active")
        ExposedDropdownMenuBox(actExpanded, { actExpanded = it }) {
            OutlinedTextField(draft.activityLevel, {}, readOnly = true, label = { Text("Activity Level") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(actExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(actExpanded, { actExpanded = false }) {
                actLevels.forEach { opt ->
                    DropdownMenuItem({ Text(opt) }, { draft = draft.copy(activityLevel = opt); actExpanded = false })
                }
            }
        }

        Text("Calculated daily goal: ${draft.calculatedDailyGoal} kcal",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        // HC logging toggle
        ListItem(
            headlineContent   = { Text("Log Food to Health Connect") },
            trailingContent   = { Switch(hcEnabled, { vm.setHcLogging(it) }) }
        )
        HorizontalDivider()

        Button({ vm.save(draft) }, Modifier.fillMaxWidth()) { Text("Save & Sync to Pi") }

        TextButton({ showRemoveDialog = true }, Modifier.fillMaxWidth()) {
            Text("Remove FiboHealth Food Entries from Health", color = StatusRed)
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove food entries?") },
            text  = { Text("This will delete all FiboHealth food entries from Health Connect.") },
            confirmButton = {
                TextButton({ vm.removeAllHcEntries(); showRemoveDialog = false }) {
                    Text("Remove", color = StatusRed)
                }
            },
            dismissButton = { TextButton({ showRemoveDialog = false }) { Text("Cancel") } }
        )
    }
}
