package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OrangeDarkColorScheme = darkColorScheme(
    primary = NeonOrangePrimary,
    secondary = NeonOrangeSecondary,
    tertiary = NeonOrangeTertiary,
    background = DarkSlateBackground,
    surface = CharcoalCard,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val OrangeLightColorScheme = lightColorScheme(
    primary = NeonOrangePrimary,
    secondary = NeonOrangeSecondary,
    tertiary = NeonOrangeTertiary,
    background = Color(0xFFFDFBFA),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1E1C1A),
    onSurface = Color(0xFF1E1C1A)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to true for the specified athletic night usage dark mode
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        OrangeDarkColorScheme
    } else {
        OrangeLightColorScheme
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
