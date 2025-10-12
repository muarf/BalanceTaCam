package com.osmcamera.mapper.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF446732),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC5EFAB),
    onPrimaryContainer = Color(0xFF062100),
    secondary = Color(0xFF55624C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E7CA),
    onSecondaryContainer = Color(0xFF131F0D),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    background = Color(0xFFFCFDF6),
    onBackground = Color(0xFF1A1C18),
    surface = Color(0xFFFCFDF6),
    onSurface = Color(0xFF1A1C18)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFAAD291),
    onPrimary = Color(0xFF173807),
    primaryContainer = Color(0xFF2E4F1C),
    onPrimaryContainer = Color(0xFFC5EFAB),
    secondary = Color(0xFFBDCBAF),
    onSecondary = Color(0xFF283420),
    secondaryContainer = Color(0xFF3E4A35),
    onSecondaryContainer = Color(0xFFD9E7CA),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = Color(0xFF1A1C18),
    onBackground = Color(0xFFE2E3DD),
    surface = Color(0xFF1A1C18),
    onSurface = Color(0xFFE2E3DD)
)

@Composable
fun OSMCameraMapperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


