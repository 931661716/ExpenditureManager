// ui.theme/Theme.kt
// This file defines the custom Material Design 3 theme for the application.
package com.example.expendituremanager.ui.theme

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

// Define the color scheme for dark mode
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00B140), // Green for primary actions and elements (updated)
    secondary = Color(0xFF9C27B0), // Purple for secondary elements
    tertiary = Color(0xFF3B89FD), // Changed to blue (3B89FD)
    background = Color(0xFF1C1B1F), // Dark background color
    surface = Color(0xFF1C1B1F), // Dark surface color
    onPrimary = Color.White, // Text/icons on primary color
    onSecondary = Color.White, // Text/icons on secondary color
    onTertiary = Color.White, // Text/icons on tertiary color
    onBackground = Color(0xFFE6E1E5), // Text/icons on background
    onSurface = Color(0xFFE6E1E5), // Text/icons on surface
    primaryContainer = Color(0xFF00802E), // Darker green for primary containers (updated)
    onPrimaryContainer = Color.White, // Text/icons on primary containers
    surfaceVariant = Color(0xFF3B89FD), // Changed to blue (3B89FD) for NavigationBar background
    error = Color(0xFFCF6679) // Error color for dark theme
)

// Define the color scheme for light mode
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00B140), // Green for primary actions and elements (updated)
    secondary = Color(0xFF9C27B0), // Purple for secondary elements
    tertiary = Color(0xFF3B89FD), // Blue for tertiary elements
    background = Color(0xFFFBFDF6), // Light background color
    surface = Color(0xFFFBFDF6), // Light surface color
    onPrimary = Color.White, // Text/icons on primary color
    onSecondary = Color.White, // Text/icons on secondary color
    onTertiary = Color.White, // Text/icons on tertiary color
    onBackground = Color(0xFF1C1B1F), // Text/icons on background
    onSurface = Color(0xFF1C1B1F), // Text/icons on surface
    primaryContainer = Color(0xFF00802E), // Darker green for primary containers (updated)
    onPrimaryContainer = Color.White, // Text/icons on primary containers
    surfaceVariant = Color(0xFF3B89FD), // Changed to blue (3B89FD) for NavigationBar background
    error = Color(0xFFB00020) // Error color for light theme
)

// Composable function to apply the application theme
@Composable
fun ExpenditureManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Check if system is in dark mode
    // Dynamic color is available on Android 12+ (API 31+)
    dynamicColor: Boolean = true, // Enable/disable dynamic color
    content: @Composable () -> Unit // Content to apply the theme to
) {
    // Determine the color scheme to use based on dark theme and dynamic color settings
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
//            window.statusBarColor = colorScheme.primary.toArgb() // Set status bar color to primary
            // Set status bar color to the desired blue (0xFF3B89FD)
            window.statusBarColor = Color(0xFF3B89FD).toArgb()
            // Adjust status bar icons for light/dark content
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    // Apply the MaterialTheme with the chosen color scheme and typography
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Custom typography defined in Type.kt
        content = content // Render the content provided
    )
}