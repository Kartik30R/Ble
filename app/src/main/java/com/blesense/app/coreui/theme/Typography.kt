package com.blesense.app.coreui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.blesense.app.R

val Helvetica = FontFamily(
    Font(R.font.helvetica),
    Font(R.font.helvetica_bold, weight = FontWeight.Bold)
)

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = Helvetica,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Helvetica,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Helvetica,
        fontSize = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Helvetica,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
    )
)
