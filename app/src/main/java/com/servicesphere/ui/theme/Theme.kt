package com.servicesphere.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val lightColors = lightColorScheme(
    primary = ServiceSpherePrimary,
    secondary = ServiceSphereSecondary,
    background = ServiceSphereBackground,
    surface = ServiceSphereSurface,
    surfaceVariant = ServiceSphereSurfaceVariant,
    primaryContainer = ServiceSphereSecondaryContainer,
    secondaryContainer = Color(0xFFE3DFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = ServiceSphereTextPrimary,
    onSurface = ServiceSphereTextPrimary,
    onSurfaceVariant = ServiceSphereTextSecondary,
    outline = ServiceSphereOutline,
    outlineVariant = ServiceSphereOutlineVariant,
    error = ServiceSphereDanger
)

private val darkColors = darkColorScheme(
    primary = Color(0xFFA78BFA),
    secondary = Color(0xFF818CF8),
    background = ServiceSphereDarkSurface,
    surface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFF374151),
    onPrimary = ServiceSphereDarkSurface,
    onSecondary = ServiceSphereDarkSurface,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFD1D5DB),
    error = Color(0xFFFCA5A5)
)

@Composable
fun ServiceSphereTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColors else lightColors,
        typography = ServiceSphereTypography,
        shapes = ServiceSphereShapes,
        content = content
    )
}
