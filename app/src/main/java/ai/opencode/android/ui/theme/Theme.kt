package ai.opencode.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TuiDarkColorScheme = darkColorScheme(
    primary = TuiColors.TerminalGreen,
    onPrimary = TuiColors.Background,
    primaryContainer = TuiColors.TerminalGreen.copy(alpha = 0.15f),
    onPrimaryContainer = TuiColors.TerminalGreen,

    secondary = TuiColors.TerminalCyan,
    onSecondary = TuiColors.Background,
    secondaryContainer = TuiColors.TerminalCyan.copy(alpha = 0.15f),
    onSecondaryContainer = TuiColors.TerminalCyan,

    tertiary = TuiColors.TerminalMagenta,
    onTertiary = TuiColors.Background,

    background = TuiColors.Background,
    onBackground = TuiColors.OnBackground,

    surface = TuiColors.Surface,
    onSurface = TuiColors.OnSurface,

    surfaceVariant = TuiColors.SurfaceVariant,
    onSurfaceVariant = TuiColors.OnSurfaceVariant,

    error = TuiColors.TerminalRed,
    onError = TuiColors.OnBackground,
    errorContainer = TuiColors.TerminalRed.copy(alpha = 0.15f),
    onErrorContainer = TuiColors.TerminalRed,

    outline = TuiColors.CodeBorder,
    outlineVariant = TuiColors.Divider,
)

@Composable
fun OpenCodeTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = TuiDarkColorScheme,
        typography = TuiTypography,
        content = content,
    )
}
