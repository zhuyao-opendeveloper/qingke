package com.lightmark.domain.model

import kotlinx.serialization.Serializable

/**
 * 应用设置（主题、图标库等）
 *
 * @property themeMode 主题模式
 * @property seedColor 主题种子色（自定义主题色）
 * @property iconPack 图标库类型
 * @property useDynamicColor 是否使用动态取色（Android 12+）
 */
@Serializable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val seedColor: Long = 0xFF6750A4, // Material 3 默认种子色
    val iconPack: IconPack = IconPack.MATERIAL,
    val useDynamicColor: Boolean = true
)

/**
 * 主题模式
 */
enum class ThemeMode {
    LIGHT,   // 浅色
    DARK,    // 深色
    SYSTEM;  // 跟随系统

    companion object {
        fun fromString(value: String): ThemeMode =
            runCatching { valueOf(value) }.getOrDefault(SYSTEM)
    }
}

/**
 * 图标库类型
 */
enum class IconPack {
    MATERIAL,       // Material Symbols
    FLUENT,         // Fluent UI Icons
    LUCIDE;         // Lucide Icons

    companion object {
        fun fromString(value: String): IconPack =
            runCatching { valueOf(value) }.getOrDefault(MATERIAL)
    }
}
