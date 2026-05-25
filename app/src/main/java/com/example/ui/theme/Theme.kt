package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NeonTeal,
    secondary = SoftCyan,
    tertiary = WarmGold,
    background = SpaceBlack,
    surface = CardNavy,
    onPrimary = SpaceBlack,
    onSecondary = SpaceBlack,
    onBackground = TextWhite,
    onSurface = TextWhite,
    error = AlertRed
)

private val LightColorScheme = lightColorScheme(
    primary = CardNavy,
    secondary = MutedSlate,
    tertiary = WarmGold,
    background = LightBg,
    surface = LightCard,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onBackground = SpaceBlack,
    onSurface = SpaceBlack,
    error = AlertRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamic color disabled to preserve our custom luxury branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
