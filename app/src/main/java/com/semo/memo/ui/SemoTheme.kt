package com.semo.memo.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val AppBackground = Color(0xFFFCFCFD)
val AppSurface = Color(0xFFFFFFFF)
val AppSurfaceVariant = Color(0xFFEAECEF)
val AppSelected = Color(0xFFDDE0E4)
val AppBorder = Color(0xFFDFE2E6)
val PrimaryText = Color(0xFF17191C)
val StrongText = Color(0xFF111317)
val SecondaryText = Color(0xFF6F747C)
val TimeText = Color(0xFF747981)
val AppAccent = Color(0xFF25282D)
val InactiveIcon = Color(0xFF92979F)
val HighlightText = Color(0xFFC45C26)

private val colors = lightColorScheme(
    primary = AppAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EAED),
    onPrimaryContainer = StrongText,
    background = AppBackground,
    onBackground = PrimaryText,
    surface = AppSurface,
    onSurface = PrimaryText,
    surfaceVariant = AppSurfaceVariant,
    onSurfaceVariant = SecondaryText,
    outline = AppBorder,
    error = Color(0xFFB3261E),
)

@Composable
fun SemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
        ),
        content = content,
    )
}
