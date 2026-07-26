package com.lightmark.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 轻刻统一间距与尺寸规范
 *
 * 保持 UI 元素间距一致，营造整齐、通透的视觉节奏
 */
object Dimens {
    // 基础间距
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 48.dp

    // 卡片相关
    val cardElevation = 2.dp      // 普通卡片阴影
    val cardElevationFloating = 4.dp  // 悬浮卡片阴影（强调立体感）
    val cardCornerRadius = 12.dp  // 卡片圆角

    // 列表项
    val listItemHeight = 64.dp

    // 按钮
    val buttonCornerRadius = 12.dp
    val fabSize = 56.dp   // FloatingActionButton 尺寸

    // 头像
    val avatarSize = 40.dp
    val avatarSmall = 24.dp
}
