package com.lightmark.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lightmark.domain.model.Priority
import com.lightmark.domain.model.TodoItem
import com.lightmark.icons.IconProvider
import com.lightmark.icons.LightMarkIcon
import com.lightmark.ui.theme.Dimens

/**
 * 待办事项卡片
 *
 * 圆角悬浮卡片设计，展示单个待办事项
 * 支持：
 * - 完成状态切换（打勾动画）
 * - 优先级色标
 * - 截止日期
 * - 标签
 * - 左滑删除（待实现）
 * - 长按进入编辑
 *
 * @param item 待办事项数据
 * @param onToggle 切换完成状态
 * @param onClick 点击卡片
 * @param onDelete 删除事项
 * @param iconProvider 当前图标库
 */
@Composable
fun TodoItemCard(
    item: TodoItem,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    iconProvider: IconProvider,
    onPin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.lg, vertical = Dimens.sm),
        shape = RoundedCornerShape(Dimens.cardCornerRadius),
        elevation = CardDefaults.cardElevation(
            defaultElevation = Dimens.cardElevation
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isPinned)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 优先级色标
            PriorityIndicator(priority = item.priority)

            Spacer(modifier = Modifier.width(Dimens.md))

            // 完成状态切换按钮（圆形勾选）
            CompleteToggle(
                isCompleted = item.isCompleted,
                onToggle = onToggle,
                iconProvider = iconProvider
            )

            Spacer(modifier = Modifier.width(Dimens.md))

            // 中间内容区域
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 标题
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (item.isPinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "已置顶",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = item.title,
                        fontSize = 16.sp,
                        fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.Medium,
                        color = if (item.isCompleted)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 描述
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // 底部信息（标签、截止日期）
                if (item.tags.isNotEmpty() || item.dueDate != null) {
                    Row(
                        modifier = Modifier.padding(top = Dimens.sm),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item.tags.take(3).forEach { tag ->
                            TagChip(text = tag)
                        }
                        if (item.dueDate != null) {
                            DueDateBadge(timestamp = item.dueDate)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(Dimens.sm))

            // 置顶按钮
            IconButton(
                onClick = onPin,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = "置顶",
                    tint = if (item.isPinned)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // 删除按钮
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                LightMarkIcon(
                    provider = iconProvider,
                    icon = { delete },
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * 优先级色标
 */
@Composable
fun PriorityIndicator(priority: Priority) {
    val color = when (priority) {
        Priority.LOW -> Color(0xFF4CAF50)     // 绿色
        Priority.MEDIUM -> Color(0xFFFF9800)  // 橙色
        Priority.HIGH -> Color(0xFFF44336)    // 红色
        Priority.URGENT -> Color(0xFF9C27B0)  // 紫色
    }
    Box(
        modifier = Modifier
            .width(3.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}

/**
 * 完成状态切换圆形按钮
 */
@Composable
fun CompleteToggle(
    isCompleted: Boolean,
    onToggle: () -> Unit,
    iconProvider: IconProvider
) {
    IconButton(
        onClick = onToggle,
        modifier = Modifier.size(28.dp)
    ) {
        AnimatedContent(
            targetState = isCompleted,
            transitionSpec = {
                fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
            },
            label = "toggle_complete"
        ) { completed ->
            if (completed) {
                Icon(
                    imageVector = iconProvider.checkCircle,
                    contentDescription = "已完成",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    imageVector = iconProvider.circleOutline,
                    contentDescription = "未完成",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * 标签 Chip
 */
@Composable
fun TagChip(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * 截止日期标签
 */
@Composable
fun DueDateBadge(timestamp: Long) {
    val dateText = remember(timestamp) {
        val sdf = java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault())
        sdf.format(java.util.Date(timestamp))
    }
    val isOverdue = remember(timestamp) {
        timestamp < System.currentTimeMillis()
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isOverdue)
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
        else
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
    ) {
        Text(
            text = dateText,
            fontSize = 11.sp,
            color = if (isOverdue)
                MaterialTheme.colorScheme.onErrorContainer
            else
                MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
