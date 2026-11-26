package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MsnLightColorScheme = lightColorScheme(
    primary = MsnAzurePrimary,
    onPrimary = Color.White,
    primaryContainer = MsnIceBlue,
    onPrimaryContainer = Color(0xFF072B59),
    secondary = MsnGreenOnline,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8F5DC),
    onSecondaryContainer = Color(0xFF0C4014),
    tertiary = MsnAmberAccent,
    onTertiary = Color.White,
    background = Color(0xFFEAF3FD),
    onBackground = Color(0xFF102033),
    surface = Color.White,
    onSurface = Color(0xFF102033),
    surfaceVariant = MsnFrostSurface,
    onSurfaceVariant = Color(0xFF445973),
    outline = MsnWindowBorder,
    error = MsnRedBusy,
    onError = Color.White
)

private val MsnDarkColorScheme = darkColorScheme(
    primary = MsnSkyLight,
    onPrimary = Color(0xFF062348),
    primaryContainer = Color(0xFF17447D),
    onPrimaryContainer = MsnIceBlue,
    secondary = Color(0xFF4ADE64),
    onSecondary = Color(0xFF052B0C),
    secondaryContainer = Color(0xFF164A21),
    onSecondaryContainer = Color(0xFFCFF7D6),
    tertiary = MsnOrangeAway,
    onTertiary = Color(0xFF311F00),
    background = MsnDarkBackground,
    onBackground = Color(0xFFE7EFFA),
    surface = MsnDarkSurface,
    onSurface = Color(0xFFE7EFFA),
    surfaceVariant = MsnDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA7BBD4),
    outline = MsnDarkBorder,
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3E0606)
)

val MsnShapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MsnDarkColorScheme else MsnLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = MsnShapes,
        content = content
    )
}
