package com.lightmark.ui.theme

import android.app.Activity
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.lightmark.domain.model.AppSettings
import com.lightmark.domain.model.ThemeMode

@Composable
fun LightMarkTheme(
    appSettings: AppSettings = AppSettings(),
    content: @Composable () -> Unit
) {
    val darkTheme = when (appSettings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val colorScheme = rememberColorScheme(appSettings, darkTheme, context)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景层：自定义图片优先，否则用主题色淡渐变（内置多套主题背景）
        if (appSettings.backgroundImageUri.isNotBlank()) {
            CustomBackgroundImage(appSettings.backgroundImageUri)
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.primaryContainer.copy(alpha = 0.22f),
                            colorScheme.background
                        )
                    )
                )
            )
        }
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = LightMarkShapes,
            content = content
        )
    }
}

@Composable
private fun rememberColorScheme(appSettings: AppSettings, dark: Boolean, context: android.content.Context): androidx.compose.material3.ColorScheme {
    val custom = appSettings.customPrimary
    val preset = PRESET_SEEDS[appSettings.themeId]
    return androidx.compose.runtime.remember(appSettings.themeId, appSettings.customPrimary, dark, appSettings.useDynamicColor) {
        when {
            custom != null -> buildSchemeFromSeed(Color(custom), dark)
            preset != null -> buildSchemeFromSeed(preset, dark)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && appSettings.useDynamicColor ->
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            else -> if (dark) DarkColorScheme else LightColorScheme
        }
    }
}

@Composable
private fun CustomBackgroundImage(uri: String) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        try {
            val u = Uri.parse(uri)
            context.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.45f
        )
    } else {
        Box(Modifier.fillMaxSize().background(Color.Transparent))
    }
}
