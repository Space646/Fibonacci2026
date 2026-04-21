package com.fibonacci.fibohealth.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibonacci.fibohealth.data.model.UserProfile
import com.fibonacci.fibohealth.ui.theme.Indigo
import com.fibonacci.fibohealth.ui.theme.StatusRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: ProfileViewModel = hiltViewModel()) {
    val profile   by vm.profile.collectAsStateWithLifecycle()
    val hcEnabled by vm.hcLoggingEnabled.collectAsStateWithLifecycle()
    var draft     by remember(profile) { mutableStateOf(profile) }
    var manualGoalText by remember(profile) {
        mutableStateOf(profile.dailyCalorieGoal?.toString() ?: "")
    }
    var showRemoveDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 36.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(draft.name,
            { draft = draft.copy(name = it) },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth())

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(draft.age.toString(),
                { draft = draft.copy(age = it.toIntOrNull() ?: draft.age) },
                label = { Text("Age") },
                modifier = Modifier.weight(1f))
            OutlinedTextField(draft.weightKg.toString(),
                { draft = draft.copy(weightKg = it.toFloatOrNull() ?: draft.weightKg) },
                label = { Text("Weight (kg)") },
                modifier = Modifier.weight(1f))
        }

        OutlinedTextField(draft.heightCm.toString(),
            { draft = draft.copy(heightCm = it.toFloatOrNull() ?: draft.heightCm) },
            label = { Text("Height (cm)") },
            modifier = Modifier.fillMaxWidth())

        // Sex picker
        var sexExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(sexExpanded, { sexExpanded = it }) {
            OutlinedTextField(
                draft.sex.replaceFirstChar { it.uppercase() }, {},
                readOnly = true, label = { Text("Sex") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sexExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(sexExpanded, { sexExpanded = false }) {
                listOf("male", "female", "other").forEach { opt ->
                    DropdownMenuItem(
                        { Text(opt.replaceFirstChar { it.uppercase() }) },
                        { draft = draft.copy(sex = opt); sexExpanded = false }
                    )
                }
            }
        }

        // Activity level picker
        var actExpanded by remember { mutableStateOf(false) }
        val actLevels     = listOf("sedentary", "light", "moderate", "active", "very_active")
        val actLabels     = listOf("Sedentary", "Lightly Active", "Moderately Active", "Very Active", "Extra Active")
        ExposedDropdownMenuBox(actExpanded, { actExpanded = it }) {
            OutlinedTextField(
                actLabels.getOrElse(actLevels.indexOf(draft.activityLevel)) { draft.activityLevel }, {},
                readOnly = true, label = { Text("Activity Level") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(actExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(actExpanded, { actExpanded = false }) {
                actLevels.forEachIndexed { i, opt ->
                    DropdownMenuItem(
                        { Text(actLabels[i]) },
                        { draft = draft.copy(activityLevel = opt); actExpanded = false }
                    )
                }
            }
        }

        // Daily calorie goal card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Indigo.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Indigo.copy(alpha = 0.2f))
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "DAILY CALORIE GOAL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.7.sp,
                    color = Indigo
                )
                Text(
                    "Calculated: ${draft.calculatedDailyGoal.toLocaleString()} kcal",
                    fontSize = 11.sp,
                    color = Indigo.copy(alpha = 0.75f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualGoalText,
                        onValueChange = { text ->
                            manualGoalText = text
                            draft = draft.copy(dailyCalorieGoal = text.toIntOrNull())
                        },
                        placeholder = { Text(draft.calculatedDailyGoal.toLocaleString()) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Indigo,
                            unfocusedBorderColor = Indigo.copy(alpha = 0.35f),
                            focusedTextColor     = Indigo,
                            unfocusedTextColor   = Indigo,
                            cursorColor          = Indigo
                        ),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold, color = Indigo
                        )
                    )
                    Text("kcal / day", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Indigo)
                }
                Text(
                    "Leave blank to use calculated value",
                    fontSize = 11.sp,
                    color = Indigo.copy(alpha = 0.6f)
                )
            }
        }

        // HC logging toggle
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            ListItem(
                headlineContent   = { Text("Log Food to Health Connect", fontWeight = FontWeight.Medium) },
                supportingContent = { Text("Sync nutrition data automatically", fontSize = 11.sp) },
                trailingContent   = { Switch(hcEnabled, { vm.setHcLogging(it) }) }
            )
        }

        Button(
            onClick = { vm.save(draft) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Save & Sync to Pi", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        TextButton(
            onClick = { showRemoveDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Remove AntiDonut Food Entries", color = StatusRed, fontSize = 13.sp)
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title            = { Text("Remove food entries?") },
            text             = { Text("This will delete all AntiDonut food entries from Health Connect.") },
            confirmButton    = {
                TextButton({ vm.removeAllHcEntries(); showRemoveDialog = false }) {
                    Text("Remove", color = StatusRed)
                }
            },
            dismissButton = { TextButton({ showRemoveDialog = false }) { Text("Cancel") } }
        )
    }
}

private fun Int.toLocaleString(): String =
    if (this >= 1000) "${this / 1000},${(this % 1000).toString().padStart(3, '0')}" else toString()
