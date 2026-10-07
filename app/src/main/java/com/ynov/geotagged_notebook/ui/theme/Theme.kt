package com.ynov.geotagged_notebook.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

private val YouNotesColorScheme = darkColorScheme(
    primary = YouNotesPurple,
    onPrimary = Color.White,
    primaryContainer = YouNotesSurfaceHigh,
    onPrimaryContainer = YouNotesText,
    secondary = YouNotesBlue,
    onSecondary = Color.White,
    secondaryContainer = YouNotesSurfaceHigh,
    onSecondaryContainer = YouNotesText,
    tertiary = YouNotesGreen,
    background = YouNotesBackground,
    onBackground = YouNotesText,
    surface = YouNotesSurface,
    onSurface = YouNotesText,
    surfaceVariant = YouNotesSurfaceHigh,
    onSurfaceVariant = YouNotesTextMuted,
    outline = YouNotesOutline,
    outlineVariant = YouNotesOutline,
    error = YouNotesError,
    onError = Color.Black
)

@Composable
fun GEOTAGGED_NOTEBOOKTheme(content: @Composable () -> Unit) {
    val activity = LocalContext.current as? Activity
    SideEffect {
        activity?.window?.let { window ->
            window.statusBarColor = YouNotesBackground.toArgb()
            window.navigationBarColor = YouNotesBackground.toArgb()
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = YouNotesColorScheme,
        typography = Typography,
        content = content
    )
}
