package com.fibonacci.fibohealth.ui.foodlog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.*
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibonacci.fibohealth.data.model.FoodLogEntry
import com.fibonacci.fibohealth.ui.components.HealthBadge
import com.fibonacci.fibohealth.ui.theme.Indigo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal fun formatFoodTime(ts: String): String = try {
    val dt = LocalDateTime.parse(ts.take(19), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    dt.format(DateTimeFormatter.ofPattern("h:mm a"))
} catch (_: Exception) {
    ts.take(10)
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun FoodLogScreen(vm: FoodLogViewModel = hiltViewModel()) {
    val log by vm.foodLog.collectAsStateWithLifecycle()
    val navigator = rememberListDetailPaneScaffoldNavigator<FoodLogEntry>()
    val selected = navigator.currentDestination?.content

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                FoodLogListPane(log, selected) {
                    navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, it)
                }
            }
        },
        detailPane = {
            AnimatedPane {
                selected?.let { FoodLogDetailPane(it, Modifier.fillMaxSize()) }
            }
        }
    )
}

@Composable
private fun FoodLogListPane(
    log: List<FoodLogEntry>,
    selected: FoodLogEntry?,
    onSelect: (FoodLogEntry) -> Unit
) {
    val totalKcal = log.sumOf { it.calories.toInt() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Food Log",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Today · $totalKcal kcal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (log.isEmpty()) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No food scans yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column {
                    Text(
                        "ENTRIES",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.04.sp
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    log.forEachIndexed { i, entry ->
                        val isSelected = entry.id == selected?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(entry) }
                                .background(
                                    if (isSelected) Indigo.copy(alpha = 0.07f)
                                    else Color.Transparent
                                )
                                .drawBehind {
                                    drawRect(
                                        color = if (isSelected) Indigo else Color.Transparent,
                                        size = Size(3.dp.toPx(), size.height)
                                    )
                                }
                                .padding(horizontal = 18.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) Indigo.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.QrCode,
                                    contentDescription = null,
                                    tint = if (isSelected) Indigo
                                           else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    entry.foodName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "${entry.calories.toInt()} kcal",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "·",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        formatFoodTime(entry.timestamp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            HealthBadge(entry.isHealthy)
                        }

                        if (i < log.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 66.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
