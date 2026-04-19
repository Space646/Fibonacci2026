package com.fibonacci.fibohealth.ui.activity

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibonacci.fibohealth.ui.theme.*

private const val BURNED_GOAL = 600

@Composable
fun ActivityScreen(vm: ActivityViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val permLauncher = rememberLauncherForActivityResult(
        contract = androidx.health.connect.client.PermissionController
            .createRequestPermissionResultContract()
    ) { vm.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 36.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Activity", style = MaterialTheme.typography.headlineMedium)

        if (!state.hasPermission) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(
                    Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Health Connect access required",
                        style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { permLauncher.launch(vm.permissions) }) {
                        Text("Grant Access")
                    }
                }
            }
        } else {
            // Weekly calories burned chart
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Weekly Calories Burned", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground)
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            LegendDot(StatusGreen, "Goal met")
                            LegendDot(StatusRed, "Below goal")
                        }
                    }
                    WeeklyBurnedChart(
                        weeklyBurned = state.weeklyBurned.ifEmpty { List(7) { 0 } },
                        burnedGoal   = BURNED_GOAL
                    )
                }
            }

            // Metric cards
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ActivityMetricCard(
                    icon  = Icons.Rounded.DirectionsWalk,
                    label = "Today's Steps",
                    value = fmtK(state.snapshot.steps),
                    unit  = "/ 10,000 goal",
                    color = Cyan,
                    modifier = Modifier.weight(1f)
                )
                ActivityMetricCard(
                    icon  = Icons.Rounded.LocalFireDepartment,
                    label = "Calories Burned",
                    value = fmtK(state.snapshot.caloriesBurned.toInt()),
                    unit  = "/ $BURNED_GOAL goal",
                    color = StatusRed,
                    modifier = Modifier.weight(1f)
                )
                ActivityMetricCard(
                    icon  = Icons.Rounded.Bolt,
                    label = "Active Minutes",
                    value = "${state.snapshot.activeMinutes}",
                    unit  = "/ 60 goal",
                    color = StatusAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WeeklyBurnedChart(weeklyBurned: List<Int>, burnedGoal: Int) {
    val days   = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val maxVal = maxOf(weeklyBurned.maxOrNull() ?: 0, burnedGoal).toFloat().coerceAtLeast(1f)
    val barHeight = 120.dp

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(barHeight + 28.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            weeklyBurned.forEachIndexed { i, v ->
                val isToday  = i == weeklyBurned.size - 1
                val met      = v >= burnedGoal
                val barColor = if (met) StatusGreen else StatusRed
                val fraction = (v.toFloat() / maxVal).coerceAtLeast(if (v > 0) 0.02f else 0f)

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (v > 0) {
                        Text(
                            text = "$v",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    val barMod = Modifier
                        .fillMaxWidth()
                        .height(barHeight * fraction)
                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                        .background(barColor.copy(alpha = if (isToday) 1f else 0.6f))
                        .then(
                            if (isToday) Modifier.border(
                                2.dp,
                                barColor,
                                RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)
                            ) else Modifier
                        )
                    Box(barMod)
                }
            }
        }
        // Day labels
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEachIndexed { i, day ->
                val isToday  = i == days.size - 1
                val v        = weeklyBurned.getOrNull(i) ?: 0
                val met      = v >= burnedGoal
                val barColor = if (met) StatusGreen else StatusRed
                Text(
                    text      = day,
                    modifier  = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize  = 11.sp,
                    fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                    color     = if (isToday) barColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActivityMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Text(unit, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun fmtK(n: Int): String =
    if (n >= 1000) "${n / 1000},${(n % 1000).toString().padStart(3, '0')}" else n.toString()
