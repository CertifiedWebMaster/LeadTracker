package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = MinimalPrimaryContainer,
    onPrimary = MinimalOnPrimaryContainer,
    primaryContainer = MinimalBlue,
    onPrimaryContainer = Color.White,
    secondary = MinimalTextSecondary,
    tertiary = MinimalTextTertiary,
    background = Color(0xFF121416),
    surface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFF2C2E30),
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6),
    onSurfaceVariant = Color(0xFFC4C6CF)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = MinimalBlue,
    onPrimary = Color.White,
    primaryContainer = MinimalPrimaryContainer,
    onPrimaryContainer = MinimalOnPrimaryContainer,
    secondary = MinimalTextSecondary,
    tertiary = MinimalTextTertiary,
    background = MinimalBackground,
    surface = MinimalSurface,
    surfaceVariant = MinimalSurfaceVariant,
    onBackground = MinimalText,
    onSurface = MinimalText,
    onSurfaceVariant = MinimalTextTertiary,
    outline = MinimalBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Always use our fine-tuned Clean Minimalism color system rather than device-dependent options
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
