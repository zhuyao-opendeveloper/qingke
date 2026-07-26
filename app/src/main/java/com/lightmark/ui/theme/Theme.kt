package com.lightmark.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.lightmark.domain.model.AppSettings
import com.lightmark.domain.model.ThemeMode
import com.lightmark.ui.theme.Color.LightSeed
import com.lightmark.ui.theme.Color.DarkColorScheme
import com.lightmark.ui.theme.Color.LightColorScheme

/**
 * 轻刻主题入口
 *
 * 支持：
 * - Material 3 动态取色（Android 12+）
 * - 浅色/深色/跟随系统
 * - 自定义种子色
 *
 * @param appSettings 当前应用设置
 * @param content 内容
 */
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

    // 动态取色优先（Android 12+）
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && appSettings.useDynamicColor -> {
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
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = LightMarkShapes,
        content = content
    )
}
