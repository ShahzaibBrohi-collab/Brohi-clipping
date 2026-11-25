package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val BrohiDarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = BgMain,
    primaryContainer = GoldDark,
    onPrimaryContainer = BgMain,
    secondary = ActionBlue,
    onSecondary = BgMain,
    secondaryContainer = CardSecondary,
    onSecondaryContainer = TextPrimary,
    tertiary = GoldLight,
    onTertiary = BgMain,
    background = BgMain,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSecondary,
    onSurfaceVariant = TextSecondary,
    outline = GoldEdge,
    outlineVariant = ObsidianRimSubtle,
    error = StatusError,
    onError = BgMain
)

val BrohiShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BrohiClippingTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BrohiDarkColorScheme,
        typography = Typography,
        shapes = BrohiShapes,
        content = content
    )
}
