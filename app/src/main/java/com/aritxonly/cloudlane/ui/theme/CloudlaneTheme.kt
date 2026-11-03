package com.aritxonly.cloudlane.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/** MIUIX owns every color. Follow system light/dark mode without Monet palettes. */
@Composable
fun CloudlaneTheme(content: @Composable () -> Unit) {
    val controller = remember { ThemeController(ColorSchemeMode.System) }
    MiuixTheme(controller = controller) {
        // The unmodified Deadliner glass reads Material locals. Bridge all roles from
        // MIUIX instead of introducing Material defaults or a second palette generator.
        MaterialTheme(colorScheme = MiuixTheme.colorScheme.glassCompatibilityColors(), content = content)
    }
}

private fun Colors.glassCompatibilityColors() = ColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    inversePrimary = primaryVariant,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = primary,
    onTertiary = onPrimary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariantSummary,
    surfaceTint = primary,
    inverseSurface = onSurface,
    inverseOnSurface = surface,
    error = error,
    onError = onError,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer,
    outline = outline,
    outlineVariant = dividerLine,
    scrim = windowDimming,
    surfaceBright = surfaceVariant,
    surfaceDim = surface,
    surfaceContainer = surfaceContainer,
    surfaceContainerHigh = surfaceContainerHigh,
    surfaceContainerHighest = surfaceContainerHighest,
    surfaceContainerLow = surface,
    surfaceContainerLowest = background,
    primaryFixed = primaryContainer,
    primaryFixedDim = primaryVariant,
    onPrimaryFixed = onPrimaryContainer,
    onPrimaryFixedVariant = onPrimaryVariant,
    secondaryFixed = secondaryContainer,
    secondaryFixedDim = secondaryVariant,
    onSecondaryFixed = onSecondaryContainer,
    onSecondaryFixedVariant = onSecondaryVariant,
    tertiaryFixed = tertiaryContainer,
    tertiaryFixedDim = tertiaryContainerVariant,
    onTertiaryFixed = onTertiaryContainer,
    onTertiaryFixedVariant = onTertiaryContainer,
)
