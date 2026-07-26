package com.lightmark.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lightmark.ui.theme.Dimens

/**
 * 轻刻风格的圆角悬浮卡片
 *
 * 特点：
 * - 12dp 大圆角，柔和现代
 * - 2dp 阴影，立体悬浮感
 * - 16dp 统一内边距
 *
 * 使用方式：
 * ```kotlin
 * LightMarkCard {
 *     Text("卡片内容")
 * }
 * ```
 */
@Composable
fun LightMarkCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: Float = Dimens.cardElevation.value,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    contentPadding: PaddingValues = PaddingValues(Dimens.lg),
    content: @Composable () -> Unit
) {
    Card(
        onClick = onClick ?: {},
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.cardCornerRadius),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        if (onClick != null) {
            androidx.compose.material3.ContentAlpha.medium
        }
        BoxWithPadding(contentPadding) {
            content()
        }
    }
}

/**
 * 悬浮感更强的卡片（用于主界面的显眼区域）
 */
@Composable
fun FloatingCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = Color.Unspecified,
    content: @Composable () -> Unit
) {
    LightMarkCard(
        modifier = modifier,
        onClick = onClick,
        elevation = Dimens.cardElevationFloating.value,
        containerColor = containerColor,
        contentPadding = PaddingValues(Dimens.xl),
        content = content
    )
}

@Composable
private fun BoxWithPadding(
    contentPadding: PaddingValues,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.padding(contentPadding)
    ) {
        content()
    }
}
