package com.fibonacci.fibohealth.ui.foodlog

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.*
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibonacci.fibohealth.data.model.FoodLogEntry

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun FoodLogScreen(vm: FoodLogViewModel = hiltViewModel()) {
    val log by vm.foodLog.collectAsStateWithLifecycle()
    val navigator = rememberListDetailPaneScaffoldNavigator<FoodLogEntry>()

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value     = navigator.scaffoldValue,
        listPane  = {
            AnimatedPane {
                if (log.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("No food scans yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn {
                        items(log) { entry ->
                            ListItem(
                                headlineContent   = { Text(entry.foodName) },
                                supportingContent = { Text("${entry.weightG}g · ${entry.calories.toInt()} kcal") },
                                modifier          = Modifier.clickable {
                                    navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, entry)
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane {
                navigator.currentDestination?.content?.let { entry ->
                    FoodLogDetailPane(entry, Modifier.fillMaxSize())
                }
            }
        }
    )
}
