package com.lightmark.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

/**
 * 内置主题：每个主题只是一个「种子色」，真正的配色由 buildSchemeFromSeed 统一推导，
 * 保证浅色/深色都和谐。用户也可以完全自定义主题色（customPrimary）。
 */
val PRESET_SEEDS = mapOf(
    "DEFAULT" to Color(0xFF6750A4),   // 默认紫
    "MINT" to Color(0xFF2E9E7B),       // 薄荷绿
    "OCEAN" to Color(0xFF2D6FE0),      // 海洋蓝
    "SUNSET" to Color(0xFFE1609A),     // 晚霞粉
    "FOREST" to Color(0xFF4B7A2E),     // 森林绿
    "MIDNIGHT" to Color(0xFF5B6BB5)    // 午夜蓝
)

val PRESET_NAMES = mapOf(
    "DEFAULT" to "默认紫",
    "MINT" to "薄荷绿",
    "OCEAN" to "海洋蓝",
    "SUNSET" to "晚霞粉",
    "FOREST" to "森林绿",
    "MIDNIGHT" to "午夜蓝"
)

val PRESET_ORDER = listOf("DEFAULT", "MINT", "OCEAN", "SUNSET", "FOREST", "MIDNIGHT")

/**
 * 由单一种子色推导一套完整的 Material 3 配色方案（浅色/深色）。
 * 通过 HSL 生成主色、辅色、第三色及其容器色，并按明暗调整明度。
 */
fun buildSchemeFromSeed(seed: Color, dark: Boolean): ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed.toArgb(), hsl)
    val hue = hsl[0]
    val sat = hsl[1].coerceIn(0.35f, 0.9f)

    fun hslc(hh: Float, ss: Float, ll: Float): Color =
        Color(
            ColorUtils.HSLToColor(
                floatArrayOf(((hh % 360) + 360) % 360, ss.coerceIn(0f, 1f), ll.coerceIn(0f, 1f))
            )
        )

    val primary = hslc(hue, sat, if (dark) 0.8f else 0.5f)
    val onPrimary = if (dark) Color(0xFF1C1B1F) else Color.White
    val primaryContainer = hslc(hue, (sat * 0.5f).coerceAtLeast(0.2f), if (dark) 0.3f else 0.88f)
    val onPrimaryContainer = hslc(hue, sat.coerceAtLeast(0.3f), if (dark) 0.9f else 0.2f)

    val secondary = hslc(hue + 40f, (sat * 0.7f).coerceIn(0f, 1f), if (dark) 0.75f else 0.55f)
    val onSecondary = if (dark) Color(0xFF1C1B1F) else Color.White
    val secondaryContainer = hslc(hue + 40f, (sat * 0.4f).coerceAtLeast(0.15f), if (dark) 0.28f else 0.9f)
    val onSecondaryContainer = hslc(hue + 40f, sat.coerceAtLeast(0.3f), if (dark) 0.88f else 0.2f)

    val tertiary = hslc(hue - 40f, (sat * 0.7f).coerceIn(0f, 1f), if (dark) 0.75f else 0.55f)
    val onTertiary = if (dark) Color(0xFF1C1B1F) else Color.White
    val tertiaryContainer = hslc(hue - 40f, (sat * 0.4f).coerceAtLeast(0.15f), if (dark) 0.28f else 0.9f)
    val onTertiaryContainer = hslc(hue - 40f, sat.coerceAtLeast(0.3f), if (dark) 0.88f else 0.2f)

    val surface = if (dark) Color(0xFF1C1B1F) else Color(0xFFFFFBFE)
    val onSurface = if (dark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)
    val surfaceVariant = hslc(hue, 0.12f, if (dark) 0.3f else 0.9f)
    val onSurfaceVariant = hslc(hue, 0.1f, if (dark) 0.8f else 0.35f)
    val outline = hslc(hue, 0.12f, if (dark) 0.55f else 0.55f)

    val error = Color(0xFFB3261E)
    val onError = Color.White
    val errorContainer = Color(0xFFF9DEDC)
    val onErrorContainer = Color(0xFF410E0B)

    val params = arrayOf(
        primary, onPrimary, primaryContainer, onPrimaryContainer,
        secondary, onSecondary, secondaryContainer, onSecondaryContainer,
        tertiary, onTertiary, tertiaryContainer, onTertiaryContainer,
        error, onError, errorContainer, onErrorContainer,
        surface, onSurface, surfaceVariant, onSurfaceVariant, outline
    )

    return if (dark) darkColorScheme(
        primary = params[0], onPrimary = params[1], primaryContainer = params[2], onPrimaryContainer = params[3],
        secondary = params[4], onSecondary = params[5], secondaryContainer = params[6], onSecondaryContainer = params[7],
        tertiary = params[8], onTertiary = params[9], tertiaryContainer = params[10], onTertiaryContainer = params[11],
        error = params[12], onError = params[13], errorContainer = params[14], onErrorContainer = params[15],
        background = params[16], onBackground = params[17], surface = params[16], onSurface = params[17],
        surfaceVariant = params[18], onSurfaceVariant = params[19], outline = params[20], outlineVariant = params[18]
    ) else lightColorScheme(
        primary = params[0], onPrimary = params[1], primaryContainer = params[2], onPrimaryContainer = params[3],
        secondary = params[4], onSecondary = params[5], secondaryContainer = params[6], onSecondaryContainer = params[7],
        tertiary = params[8], onTertiary = params[9], tertiaryContainer = params[10], onTertiaryContainer = params[11],
        error = params[12], onError = params[13], errorContainer = params[14], onErrorContainer = params[15],
        background = params[16], onBackground = params[17], surface = params[16], onSurface = params[17],
        surfaceVariant = params[18], onSurfaceVariant = params[19], outline = params[20], outlineVariant = params[18]
    )
}
