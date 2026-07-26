package com.lightmark.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 轻刻形状系统
 *
 * 强调圆角设计，营造柔和、现代、卡片化的视觉感受
 * 悬浮卡片使用较大的圆角 + 阴影营造立体感
 */
val LightMarkShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // 标签、小图标
    small = RoundedCornerShape(8.dp),        // 按钮、输入框
    medium = RoundedCornerShape(12.dp),      // 卡片、对话框
    large = RoundedCornerShape(16.dp),       // 底部菜单、大卡片
    extraLarge = RoundedCornerShape(24.dp)   // 全屏弹窗
)
