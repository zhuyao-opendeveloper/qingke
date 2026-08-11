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
    val useDynamicColor: Boolean = true,
    /** 内置主题 id（见 PRESET_SEEDS），默认 DEFAULT */
    val themeId: String = "DEFAULT",
    /** 用户自定义主题色（覆盖内置主题），为 null 时使用 themeId */
    val customPrimary: Long? = null,
    /** 用户自定义背景图片 Uri，为空表示使用主题色淡渐变背景 */
    val backgroundImageUri: String = "",
    /** 全局字号缩放系数（#61），同时影响行距（sp 相关尺寸） */
    val fontScale: Float = 1.0f
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
