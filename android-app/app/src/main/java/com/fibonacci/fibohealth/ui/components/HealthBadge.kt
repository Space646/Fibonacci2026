package com.fibonacci.fibohealth.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.fibonacci.fibohealth.ui.theme.StatusGreen
import com.fibonacci.fibohealth.ui.theme.StatusRed

@Composable
fun HealthBadge(isHealthy: Boolean) {
    SuggestionChip(
        onClick = {},
        label   = { Text(if (isHealthy) "Healthy" else "Unhealthy") },
        icon    = { Icon(if (isHealthy) Icons.Rounded.Check else Icons.Rounded.Close, null) },
        colors  = SuggestionChipDefaults.suggestionChipColors(
            containerColor = if (isHealthy) StatusGreen.copy(0.15f) else StatusRed.copy(0.15f),
            labelColor     = if (isHealthy) StatusGreen else StatusRed,
            iconContentColor = if (isHealthy) StatusGreen else StatusRed
        )
    )
}
