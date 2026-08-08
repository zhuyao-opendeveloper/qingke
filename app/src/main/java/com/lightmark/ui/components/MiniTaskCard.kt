package com.lightmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.lightmark.ui.theme.Dimens
import com.lightmark.util.DateTimeUtils

/** 优先级对应色（与列表卡片保持一致） */
fun priorityColorOf(priority: Priority): Color = when (priority) {
    Priority.IDLE -> Color(0xFF9E9E9E)
    Priority.LOW -> Color(0xFF4CAF50)
    Priority.MEDIUM -> Color(0xFF2196F3)
    Priority.HIGH -> Color(0xFFFF9800)
    Priority.URGENT -> Color(0xFFF44336)
}

fun priorityLabelOf(priority: Priority): String = when (priority) {
    Priority.IDLE -> "空闲"
    Priority.LOW -> "低"
    Priority.MEDIUM -> "中"
    Priority.HIGH -> "高"
    Priority.URGENT -> "紧急"
}

/**
 * 看板 / 四象限使用的紧凑任务卡片（#52 / #53）
 */
@Composable
fun MiniTaskCard(
    item: TodoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.md),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(priorityColorOf(item.priority))
            )
            Spacer(modifier = Modifier.width(Dimens.sm))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.isCompleted)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurface
                )
                val due = item.dueDate
                if (due != null) {
                    Spacer(modifier = Modifier.height(Dimens.xs))
                    val overdue = !item.isCompleted && due < System.currentTimeMillis()
                    Text(
                        text = DateTimeUtils.formatDateTime(due),
                        fontSize = 11.sp,
                        color = if (overdue) Color(0xFFF44336)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Dimens.xs))
                    Text(
                        text = item.tags.joinToString(" ") { "#$it" },
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/** 小圆点，用于列头计数等 */
@Composable
fun Dot(color: Color, size: Int = 8) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
    )
}
