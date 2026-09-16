package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.ThemeAccent

@Composable
fun AuraTheme(
    themeAccent: ThemeAccent = ThemeAccent.CYAN,
    content: @Composable () -> Unit
) {
    val primary = Color(themeAccent.primaryHex)
    val secondary = Color(themeAccent.secondaryHex)

    val colorScheme = darkColorScheme(
        primary = primary,
        secondary = secondary,
        tertiary = CyberNeonEmerald,
        background = CyberBlack,
        surface = CyberDarkSurface,
        surfaceVariant = CyberCardBg,
        onPrimary = Color.Black,
        onSecondary = Color.White,
        onTertiary = Color.Black,
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        outline = CyberBorder
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
