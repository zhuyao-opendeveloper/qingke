package com.lightmark.ui.screens.table

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lightmark.domain.model.TodoItem
import com.lightmark.ui.components.priorityColorOf
import com.lightmark.ui.components.priorityLabelOf
import com.lightmark.ui.theme.Dimens
import com.lightmark.util.DateTimeUtils

/**
 * 表格视图（#56）
 *
 * 一屏横向对比全部字段，表头可点击排序，行内可直接勾选完成。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableScreen(
    onNavigateBack: () -> Unit,
    onOpenTodo: (String) -> Unit,
    viewModel: TableViewModel = hiltViewModel()
) {
    val rows by viewModel.rows.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val ascending by viewModel.ascending.collectAsState()
    val categoryNames by viewModel.categoryNames.collectAsState()
    val hScroll = rememberScrollState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("表格视图", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 过滤器
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.lg, vertical = Dimens.sm),
                horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                TableFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { viewModel.setFilter(option) },
                        label = { Text(option.label, fontSize = 13.sp) }
                    )
                }
            }

            Text(
                "共 ${rows.size} 条 · 左右滑动查看更多列",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Dimens.lg, vertical = Dimens.xs)
            )

            Column(modifier = Modifier.horizontalScroll(hScroll)) {
                // 表头
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(vertical = Dimens.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(COL_CHECK))
                    HeaderCell("标题", COL_TITLE, sort == TableSort.TITLE, ascending) {
                        viewModel.toggleSort(TableSort.TITLE)
                    }
                    HeaderCell("优先级", COL_PRIORITY, sort == TableSort.PRIORITY, ascending) {
                        viewModel.toggleSort(TableSort.PRIORITY)
                    }
                    HeaderCell("分类", COL_CATEGORY, sort == TableSort.CATEGORY, ascending) {
                        viewModel.toggleSort(TableSort.CATEGORY)
                    }
                    HeaderCell("到期", COL_DUE, sort == TableSort.DUE, ascending) {
                        viewModel.toggleSort(TableSort.DUE)
                    }
                    HeaderCell("状态", COL_STATUS, false, ascending) { }
                    HeaderCell("标签", COL_TAGS, false, ascending) { }
                    HeaderCell("更新", COL_UPDATED, sort == TableSort.UPDATED, ascending) {
                        viewModel.toggleSort(TableSort.UPDATED)
                    }
                }
                Divider()

                if (rows.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .width(TOTAL_WIDTH)
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "这个筛选下没有任务",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.width(TOTAL_WIDTH),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(rows, key = { it.id }) { item ->
                            TableRow(
                                item = item,
                                categoryName = item.categoryId?.let { categoryNames[it] },
                                onToggle = { viewModel.toggleComplete(item) },
                                onOpen = { onOpenTodo(item.id) }
                            )
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(
    label: String,
    width: androidx.compose.ui.unit.Dp,
    active: Boolean,
    ascending: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = if (active) "$label ${if (ascending) "\u2191" else "\u2193"}" else label,
        fontSize = 12.sp,
        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
        color = if (active) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .width(width)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.sm)
    )
}

@Composable
private fun TableRow(
    item: TodoItem,
    categoryName: String?,
    onToggle: () -> Unit,
    onOpen: () -> Unit
) {
    val overdue = !item.isCompleted &&
        (item.dueDate ?: Long.MAX_VALUE) < System.currentTimeMillis()

    Row(
        modifier = Modifier
            .width(TOTAL_WIDTH)
            .clickable(onClick = onOpen)
            .padding(vertical = Dimens.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(COL_CHECK),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onToggle, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (item.isCompleted) Icons.Filled.CheckCircle
                    else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (item.isCompleted) "标记未完成" else "标记完成",
                    tint = if (item.isCompleted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Text(
            text = item.title,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .width(COL_TITLE)
                .padding(horizontal = Dimens.sm)
        )

        Text(
            text = priorityLabelOf(item.priority),
            fontSize = 12.sp,
            color = priorityColorOf(item.priority),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier
                .width(COL_PRIORITY)
                .padding(horizontal = Dimens.sm)
        )

        Text(
            text = categoryName ?: "—",
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(COL_CATEGORY)
                .padding(horizontal = Dimens.sm)
        )

        Text(
            text = item.dueDate?.let { DateTimeUtils.formatDate(it) } ?: "—",
            fontSize = 12.sp,
            maxLines = 1,
            color = if (overdue) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(COL_DUE)
                .padding(horizontal = Dimens.sm)
        )

        Text(
            text = statusLabel(item),
            fontSize = 12.sp,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(COL_STATUS)
                .padding(horizontal = Dimens.sm)
        )

        Text(
            text = if (item.tags.isEmpty()) "—" else item.tags.joinToString(" ") { "#$it" },
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(COL_TAGS)
                .padding(horizontal = Dimens.sm)
        )

        Text(
            text = DateTimeUtils.formatDate(item.updatedAt),
            fontSize = 12.sp,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(COL_UPDATED)
                .padding(horizontal = Dimens.sm)
        )
    }
}

private fun statusLabel(item: TodoItem): String = when {
    item.isCompleted -> "已完成"
    item.isBlocked -> "被阻塞"
    item.status == "PAUSED" -> "已暂停"
    item.status == "CANCELLED" -> "已取消"
    (item.startDate ?: Long.MAX_VALUE) <= System.currentTimeMillis() -> "进行中"
    else -> "待办"
}

private val COL_CHECK = 44.dp
private val COL_TITLE = 180.dp
private val COL_PRIORITY = 72.dp
private val COL_CATEGORY = 100.dp
private val COL_DUE = 92.dp
private val COL_STATUS = 72.dp
private val COL_TAGS = 140.dp
private val COL_UPDATED = 92.dp
private val TOTAL_WIDTH = 792.dp
