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
 * 杞诲埢椋庢牸鐨勫渾瑙掓偓娴崱鐗? *
 * 鐗圭偣锛? * - 12dp 澶у渾瑙掞紝鏌斿拰鐜颁唬
 * - 2dp 闃村奖锛岀珛浣撴偓娴劅
 * - 16dp 缁熶竴鍐呰竟璺? *
 * 浣跨敤鏂瑰紡锛? * ```kotlin
 * LightMarkCard {
 *     Text("鍗＄墖鍐呭")
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

        BoxWithPadding(contentPadding) {
            content()
        }
    }
}

/**
 * 鎮诞鎰熸洿寮虹殑鍗＄墖锛堢敤浜庝富鐣岄潰鐨勬樉鐪煎尯鍩燂級
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

