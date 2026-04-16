package com.fibonacci.fibohealth.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val FiboTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 22.sp),
    titleLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyMedium     = TextStyle(fontSize = 14.sp),
    labelSmall     = TextStyle(fontSize = 11.sp, letterSpacing = 0.5.sp)
)
