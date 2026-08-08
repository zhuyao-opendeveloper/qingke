package com.lightmark.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lightmark.domain.model.TodoItem
import com.lightmark.ui.components.LightMarkCard
import com.lightmark.ui.theme.Dimens
import java.util.Calendar

/**
 * 铁架日历
 *
 * 三种视图：
 * - 日视图：单日待办明细
 * - 月视图：6×7 网格月历，格内显示待办数量点
 * - 年视图：12 个月缩略格，显示每月待办数量
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    onOpenTodo: (String) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val todosByDay by viewModel.todosByDay.collectAsState()
    val undated by viewModel.undatedCount.collectAsState()

    val today = remember { Calendar.getInstance() }
    var mode by remember { mutableStateOf(CalendarViewMode.MONTH) }
    var year by remember { mutableStateOf(today.get(Calendar.YEAR)) }
    var month0 by remember { mutableStateOf(today.get(Calendar.MONTH)) }
    var day by remember { mutableStateOf(today.get(Calendar.DAY_OF_MONTH)) }

    fun shift(delta: Int) {
        when (mode) {
            CalendarViewMode.DAY -> {
                val c = Calendar.getInstance().apply {
                    set(year, month0, day, 0, 0, 0)
                    add(Calendar.DAY_OF_YEAR, delta)
                }
                year = c.get(Calendar.YEAR); month0 = c.get(Calendar.MONTH); day = c.get(Calendar.DAY_OF_MONTH)
            }
            CalendarViewMode.MONTH -> {
                val c = Calendar.getInstance().apply {
                    set(year, month0, 1, 0, 0, 0)
                    add(Calendar.MONTH, delta)
                }
                year = c.get(Calendar.YEAR); month0 = c.get(Calendar.MONTH)
                val maxDay = c.getActualMaximum(Calendar.DAY_OF_MONTH)
                if (day > maxDay) day = maxDay
            }
            CalendarViewMode.YEAR -> year += delta
        }
    }

    val title = when (mode) {
        CalendarViewMode.DAY -> "%d年%d月%d日".format(year, month0 + 1, day)
        CalendarViewMode.MONTH -> "%d年%d月".format(year, month0 + 1)
        CalendarViewMode.YEAR -> "%d年".format(year)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val c = Calendar.getInstance()
                        year = c.get(Calendar.YEAR)
                        month0 = c.get(Calendar.MONTH)
                        day = c.get(Calendar.DAY_OF_MONTH)
                    }) { Text("今天") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.md)
        ) {
            // 视图切换 + 翻页
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.sm)
            ) {
                IconButton(onClick = { shift(-1) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "上一页")
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
                    modifier = Modifier.weight(1f),
                ) {
                    CalendarViewMode.values().forEach { m ->
                        FilterChip(
                            selected = mode == m,
                            onClick = { mode = m },
                            label = { Text(m.label, fontSize = 13.sp) }
                        )
                    }
                }
                IconButton(onClick = { shift(1) }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "下一页")
                }
            }

            when (mode) {
                CalendarViewMode.DAY -> DayView(
                    year = year, month0 = month0, day = day,
                    todosByDay = todosByDay,
                    undatedCount = undated,
                    onOpenTodo = onOpenTodo
                )
                CalendarViewMode.MONTH -> MonthView(
                    year = year, month0 = month0, selectedDay = day,
                    todosByDay = todosByDay,
                    onSelectDay = { day = it },
                    onOpenDay = { day = it; mode = CalendarViewMode.DAY },
                    onOpenTodo = onOpenTodo
                )
                CalendarViewMode.YEAR -> YearView(
                    year = year,
                    todosByDay = todosByDay,
                    onOpenMonth = { m -> month0 = m; mode = CalendarViewMode.MONTH }
                )
            }
        }
    }
}

/* ---------------- 日视图 ---------------- */

@Composable
private fun DayView(
    year: Int,
    month0: Int,
    day: Int,
    todosByDay: Map<Int, List<TodoItem>>,
    undatedCount: Int,
    onOpenTodo: (String) -> Unit
) {
    val key = dayKeyOf(year, month0, day)
    val list = todosByDay[key].orEmpty()
    val weekLabel = remember(key) {
        val c = Calendar.getInstance().apply { set(year, month0, day) }
        when (c.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "星期一"; Calendar.TUESDAY -> "星期二"; Calendar.WEDNESDAY -> "星期三"
            Calendar.THURSDAY -> "星期四"; Calendar.FRIDAY -> "星期五"; Calendar.SATURDAY -> "星期六"
            else -> "星期日"
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(bottom = Dimens.sm)
        ) {
            Text("$day", fontSize = 46.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(Dimens.sm))
            Column {
                Text(weekLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "${list.size} 项待办 · ${list.count { it.isCompleted }} 已完成",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (list.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("这一天没有安排", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (undatedCount > 0) {
                        Spacer(Modifier.height(Dimens.xs))
                        Text(
                            "另有 $undatedCount 项待办未设置日期",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                items(list, key = { it.id }) { todo ->
                    TodoBrief(todo) { onOpenTodo(todo.id) }
                }
            }
        }
    }
}

@Composable
private fun TodoBrief(todo: TodoItem, onClick: () -> Unit) {
    LightMarkCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (todo.isCompleted) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.primary
                    )
            )
            Spacer(Modifier.width(Dimens.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    todo.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                if (todo.description.isNotBlank()) {
                    Text(
                        todo.description,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/* ---------------- 月视图 ---------------- */

@Composable
private fun MonthView(
    year: Int,
    month0: Int,
    selectedDay: Int,
    todosByDay: Map<Int, List<TodoItem>>,
    onSelectDay: (Int) -> Unit,
    onOpenDay: (Int) -> Unit,
    onOpenTodo: (String) -> Unit
) {
    val cal = remember(year, month0) {
        Calendar.getInstance().apply { set(year, month0, 1, 0, 0, 0) }
    }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // 周一为一周起点：Calendar.MONDAY=2 → offset 0
    val firstOffset = remember(year, month0) {
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        if (dow == Calendar.SUNDAY) 6 else dow - 2
    }
    val todayKey = remember { dayKeyOf(System.currentTimeMillis()) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 星期表头
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEachIndexed { i, w ->
                Text(
                    w,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (i >= 5) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(Dimens.xs))

        // 6 行 × 7 列铁架网格
        val totalCells = 42
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(Dimens.cardCornerRadius)
                )
                .clip(RoundedCornerShape(Dimens.cardCornerRadius))
        ) {
            for (row in 0 until 6) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val idx = row * 7 + col
                        val d = idx - firstOffset + 1
                        val inMonth = d in 1..daysInMonth
                        val key = if (inMonth) dayKeyOf(year, month0, d) else -1
                        val count = todosByDay[key]?.size ?: 0
                        val doneAll = inMonth && count > 0 &&
                            todosByDay[key]!!.all { it.isCompleted }
                        DayCell(
                            day = if (inMonth) d else null,
                            count = count,
                            allDone = doneAll,
                            isToday = inMonth && key == todayKey,
                            isSelected = inMonth && d == selectedDay,
                            modifier = Modifier.weight(1f),
                            onClick = { if (inMonth) onSelectDay(d) }
                        )
                    }
                }
                if (row < 5) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
                if (idxRowEmpty(row, firstOffset, daysInMonth, totalCells)) break
            }
        }

        Spacer(Modifier.height(Dimens.md))

        // 选中日的待办
        val selKey = dayKeyOf(year, month0, selectedDay)
        val list = todosByDay[selKey].orEmpty()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.xs)
        ) {
            Text(
                "%d月%d日".format(month0 + 1, selectedDay),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(Dimens.sm))
            Text(
                "${list.size} 项",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onOpenDay(selectedDay) }) { Text("查看日视图", fontSize = 12.sp) }
        }
        if (list.isEmpty()) {
            Text(
                "这一天没有安排",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                items(list, key = { it.id }) { todo ->
                    TodoBrief(todo) { onOpenTodo(todo.id) }
                }
            }
        }
    }
}

/** 该行之后是否已无有效日期，可提前收尾（避免出现整行空白） */
private fun idxRowEmpty(row: Int, firstOffset: Int, daysInMonth: Int, total: Int): Boolean {
    val nextRowFirstDay = (row + 1) * 7 - firstOffset + 1
    return nextRowFirstDay > daysInMonth
}

@Composable
private fun DayCell(
    day: Int?,
    count: Int,
    allDone: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    Box(
        modifier = modifier
            .height(52.dp)
            .background(bg)
            .clickable(enabled = day != null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (day == null) return@Box
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$day",
                fontSize = 14.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(Modifier.height(3.dp))
            if (count > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val dots = if (count > 3) 3 else count
                    repeat(dots) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (allDone) MaterialTheme.colorScheme.outline
                                    else MaterialTheme.colorScheme.primary
                                )
                        )
                    }
                    if (count > 3) {
                        Text(
                            "+",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(5.dp))
            }
        }
    }
}

/* ---------------- 年视图 ---------------- */

@Composable
private fun YearView(
    year: Int,
    todosByDay: Map<Int, List<TodoItem>>,
    onOpenMonth: (Int) -> Unit
) {
    val counts = remember(year, todosByDay) {
        IntArray(12) { m ->
            todosByDay.entries.sumOf { (k, v) ->
                if (k / 10000 == year && (k / 100) % 100 == m + 1) v.size else 0
            }
        }
    }
    val maxCount = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    val todayCal = remember { Calendar.getInstance() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimens.sm)
    ) {
        Text(
            "全年 ${counts.sum()} 项带日期的待办",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        for (row in 0 until 4) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                for (col in 0 until 3) {
                    val m = row * 3 + col
                    MonthTile(
                        month0 = m,
                        count = counts[m],
                        intensity = counts[m].toFloat() / maxCount,
                        isCurrent = year == todayCal.get(Calendar.YEAR) &&
                            m == todayCal.get(Calendar.MONTH),
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenMonth(m) }
                    )
                }
            }
        }
        Spacer(Modifier.height(Dimens.lg))
    }
}

@Composable
private fun MonthTile(
    month0: Int,
    count: Int,
    intensity: Float,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val base = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(Dimens.cardCornerRadius))
            .background(base.copy(alpha = 0.06f + 0.34f * intensity.coerceIn(0f, 1f)))
            .border(
                width = if (isCurrent) 2.dp else 1.dp,
                color = if (isCurrent) base else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(Dimens.cardCornerRadius)
            )
            .clickable { onClick() }
            .padding(Dimens.sm),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "${month0 + 1}月",
            fontSize = 16.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold
        )
        Spacer(Modifier.height(Dimens.xs))
        Text(
            if (count > 0) "$count 项" else "空",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
