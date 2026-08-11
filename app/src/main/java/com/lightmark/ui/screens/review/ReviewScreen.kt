package com.lightmark.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lightmark.ui.theme.Dimens
import com.lightmark.util.DateTimeUtils

/**
 * 回顾与复盘（#83 / #94 / #121）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    onNavigateBack: () -> Unit,
    onOpenTodo: (String) -> Unit,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val report by viewModel.report.collectAsState()
    val period by viewModel.period.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("回顾复盘", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(report.toPlainText()))
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "复制报告")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = Dimens.lg,
                end = Dimens.lg,
                top = Dimens.sm,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.md)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                ) {
                    ReviewPeriod.entries.forEach { option ->
                        FilterChip(
                            selected = period == option,
                            onClick = { viewModel.setPeriod(option) },
                            label = { Text(option.label, fontSize = 13.sp) }
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(Dimens.lg)) {
                        Text(
                            "${report.periodLabel}完成 ${report.completed} 条",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(Dimens.xs))
                        Text(
                            "新建 ${report.created} 条 · 完成率 ${report.completionRate}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(Dimens.md))
                        LinearProgressIndicator(
                            progress = report.completionRate / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.md)) {
                    MetricCard("剩余待办", "${report.openCount}", Modifier.weight(1f))
                    MetricCard("逾期", "${report.overdueTotal}", Modifier.weight(1f))
                    MetricCard("连续天数", "${report.streakDays}", Modifier.weight(1f))
                }
            }

            item {
                SectionCard("时间画像") {
                    InfoRow("最高产的一天", report.busiestDay)
                    InfoRow(
                        "平均完成耗时",
                        if (report.avgCompleteHours > 0)
                            String.format("%.1f 小时", report.avgCompleteHours)
                        else "—"
                    )
                    InfoRow("没有日期的待办", "${report.noDateCount} 条")
                }
            }

            if (report.byCategory.isNotEmpty()) {
                item {
                    SectionCard("按分类分布") {
                        val max = report.byCategory.maxOf { it.second }.coerceAtLeast(1)
                        report.byCategory.forEach { (name, count) ->
                            BarRow(name, count, max)
                        }
                    }
                }
            }

            if (report.byPriority.isNotEmpty()) {
                item {
                    SectionCard("按优先级分布") {
                        val max = report.byPriority.maxOf { it.second }.coerceAtLeast(1)
                        report.byPriority.forEach { (name, count) ->
                            BarRow(name, count, max)
                        }
                    }
                }
            }

            item {
                SectionCard("一键整理") {
                    Text(
                        "复盘的意义在于让列表回到可执行状态。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Dimens.sm))
                    FilledTonalButton(
                        onClick = { viewModel.rescheduleOverdueToToday() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Dimens.sm))
                        Text("把逾期任务顺延到今天")
                    }
                    Spacer(Modifier.height(Dimens.sm))
                    FilledTonalButton(
                        onClick = { viewModel.archiveCompletedInPeriod() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Dimens.sm))
                        Text("归档${report.periodLabel}已完成的任务")
                    }
                }
            }

            if (report.overdue.isNotEmpty()) {
                item {
                    Text(
                        "逾期未完成（${report.overdueTotal}）",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = Dimens.sm)
                    )
                }
                items(report.overdue, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenTodo(item.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(Dimens.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                item.title,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                item.dueDate?.let { DateTimeUtils.formatDate(it) } ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(Dimens.lg)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Dimens.sm))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.xs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BarRow(name: String, count: Int, max: Int) {
    Column(modifier = Modifier.padding(vertical = Dimens.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            Text("$count", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(Dimens.xs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(3.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(count.toFloat() / max)
                    .height(6.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}
