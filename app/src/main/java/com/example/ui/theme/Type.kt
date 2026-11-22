package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val DisplayFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val BodyFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

fun buildAlokTypography(fontScaleMultiplier: Float = 1.0f): Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (32 * fontScaleMultiplier).sp,
        lineHeight = (38 * fontScaleMultiplier).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (24 * fontScaleMultiplier).sp,
        lineHeight = (30 * fontScaleMultiplier).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (20 * fontScaleMultiplier).sp,
        lineHeight = (26 * fontScaleMultiplier).sp
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (18 * fontScaleMultiplier).sp,
        lineHeight = (24 * fontScaleMultiplier).sp
    ),
    titleMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (16 * fontScaleMultiplier).sp,
        lineHeight = (22 * fontScaleMultiplier).sp
    ),
    titleSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (14 * fontScaleMultiplier).sp,
        lineHeight = (20 * fontScaleMultiplier).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (16 * fontScaleMultiplier).sp,
        lineHeight = (22 * fontScaleMultiplier).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (14 * fontScaleMultiplier).sp,
        lineHeight = (20 * fontScaleMultiplier).sp
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (12 * fontScaleMultiplier).sp,
        lineHeight = (16 * fontScaleMultiplier).sp
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (14 * fontScaleMultiplier).sp,
        lineHeight = (20 * fontScaleMultiplier).sp
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (12 * fontScaleMultiplier).sp,
        lineHeight = (16 * fontScaleMultiplier).sp
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (11 * fontScaleMultiplier).sp,
        lineHeight = (14 * fontScaleMultiplier).sp
    )
)

val Typography = buildAlokTypography(1.0f)
