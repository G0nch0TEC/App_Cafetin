package com.proyecto.cafetin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


private val LightColorScheme = lightColorScheme(
    primary = CafetinPrimary,
    onPrimary = CafetinOnPrimaryContainer,
    primaryContainer = CafetinPrimaryContainer,

    background = Color(0xFFF4F2F8),
    surface = Color.White,

    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),

    error = CafetinDebtRed,
    errorContainer = CafetinDebtRedBg,

    outline = Color(0xFFCAC4D0),
    surfaceVariant = CafetinPrimaryContainer
)

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */


@Composable
fun CafetinTheme(
    darkTheme: Boolean = false,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}