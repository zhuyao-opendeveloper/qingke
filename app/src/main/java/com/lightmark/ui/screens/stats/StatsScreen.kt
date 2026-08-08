package com.lightmark.ui.screens.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lightmark.domain.model.Priority
import com.lightmark.ui.theme.Dimens
import kotlin.math.roundToInt

/**
 * 统计页
 *
 * 展示完成率环形进度、关键计数、按优先级/分类的分布条形图，
 * 进度与条形宽度均带过渡动画。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()

    val animatedRate by animateFloatAsState(
        targetValue = stats.rate,
        animationSpec = tween(800),
        label = "rate"
    )
    val maxPriority = (stats.byPriority.values.maxOrNull() ?: 0).coerceAtLeast(1)
    val maxCategory = (stats.byCategory.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 完成率环形
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                CircularProgressIndicator(
                    progress = animatedRate,
                    modifier = Modifier.size(160.dp),
                    strokeWidth = 14.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(animatedRate * 100).roundToInt()}%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("完成率", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(Dimens.xl))

            // 关键计数卡片
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                StatChip("总计", stats.total, MaterialTheme.colorScheme.primaryContainer)
                StatChip("已完成", stats.done, MaterialTheme.colorScheme.tertiaryContainer)
                StatChip("待办", stats.pending, MaterialTheme.colorScheme.secondaryContainer)
                StatChip("逾期", stats.overdue, MaterialTheme.colorScheme.errorContainer)
            }

            Spacer(modifier = Modifier.height(Dimens.xl))

            // 按优先级分布
            SectionTitle("按优先级")
            Priority.entries.forEach { p ->
                val count = stats.byPriority[p] ?: 0
                val ratio = count.toFloat() / maxPriority
                val animatedWidth by animateFloatAsState(ratio, tween(600), label = "pw_$p")
                BarRow(
                    label = priorityLabel(p),
                    count = count,
                    color = priorityColor(p),
                    ratio = animatedWidth
                )
                Spacer(modifier = Modifier.height(Dimens.sm))
            }

            Spacer(modifier = Modifier.height(Dimens.lg))

            // 按分类分布
            if (stats.byCategory.isNotEmpty()) {
                SectionTitle("按分类")
                stats.byCategory.forEach { (cat, count) ->
                    val ratio = count.toFloat() / maxCategory
                    val animatedWidth by animateFloatAsState(ratio, tween(600), label = "cw_${cat.id}")
                    BarRow(
                        label = cat.name,
                        count = count,
                        color = Color(cat.color),
                        ratio = animatedWidth,
                        leadingDot = Color(cat.color)
                    )
                    Spacer(modifier = Modifier.height(Dimens.sm))
                }
            }
        }
    }
}

@Composable
private fun RowScope.StatChip(label: String, value: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color,
        modifier = Modifier.weight(1f)
    ) {
        Column(
            modifier = Modifier.padding(Dimens.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$value", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BarRow(
    label: String,
    count: Int,
    color: Color,
    ratio: Float,
    leadingDot: Color? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingDot != null) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(leadingDot)
                    )
                    Spacer(modifier = Modifier.width(Dimens.sm))
                }
                Text(text = label, fontSize = 14.sp)
            }
            Text(text = "$count", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Dimens.sm)
    )
}

private fun priorityLabel(p: Priority): String = when (p) {
    Priority.LOW -> "低"
    Priority.MEDIUM -> "中"
    Priority.HIGH -> "高"
    Priority.URGENT -> "紧急"
    Priority.IDLE -> "空闲"
}

private fun priorityColor(p: Priority): Color = when (p) {
    Priority.LOW -> Color(0xFF4CAF50)
    Priority.MEDIUM -> Color(0xFFFF9800)
    Priority.HIGH -> Color(0xFFF44336)
    Priority.URGENT -> Color(0xFF9C27B0)
    Priority.IDLE -> Color(0xFF9E9E9E)
}
