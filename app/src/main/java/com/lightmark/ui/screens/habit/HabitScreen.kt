@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lightmark.ui.screens.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.lightmark.data.local.entity.GoalEntity
import kotlin.math.roundToInt

/**
 * 习惯打卡 + 目标追踪
 * 对应功能：习惯打卡 / 连续天数 / 完成率热力条 / 目标进度 / 里程碑
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    onNavigateBack: () -> Unit,
    viewModel: HabitViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val progress by viewModel.todayProgress.collectAsState()

    var tab by remember { mutableStateOf(0) }
    var showAddHabit by remember { mutableStateOf(false) }
    var showAddGoal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (tab == 0) "习惯打卡" else "目标追踪") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (tab == 0) showAddHabit = true else showAddGoal = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "新增")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("习惯") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("目标") })
            }

            if (tab == 0) {
                HabitList(
                    stats = stats,
                    doneToday = progress.first,
                    totalToday = progress.second,
                    onToggle = { viewModel.toggleCheck(it) },
                    onIncrement = { viewModel.incrementCheck(it) },
                    onArchive = { id, archived -> viewModel.setArchived(id, archived) },
                    onDelete = { viewModel.deleteHabit(it) }
                )
            } else {
                GoalList(
                    goals = goals,
                    onToggleDone = { viewModel.toggleGoalCompleted(it) },
                    onProgress = { goal, v -> viewModel.updateGoalProgress(goal, v) },
                    onAddMilestone = { goal, t -> viewModel.addMilestone(goal, t) },
                    onToggleMilestone = { goal, i -> viewModel.toggleMilestone(goal, i) },
                    onDelete = { viewModel.deleteGoal(it) }
                )
            }
        }
    }

    if (showAddHabit) {
        AddHabitDialog(
            onDismiss = { showAddHabit = false },
            onConfirm = { name, emoji, color, period, target, note ->
                viewModel.addHabit(name, emoji, color, period, target, note)
                showAddHabit = false
            }
        )
    }

    if (showAddGoal) {
        AddGoalDialog(
            onDismiss = { showAddGoal = false },
            onConfirm = { title, desc, target, unit ->
                viewModel.addGoal(title, desc, target, unit, null)
                showAddGoal = false
            }
        )
    }
}

@Composable
private fun HabitList(
    stats: List<HabitStat>,
    doneToday: Int,
    totalToday: Int,
    onToggle: (String) -> Unit,
    onIncrement: (String) -> Unit,
    onArchive: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "今日进度  $doneToday / $totalToday",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = if (totalToday == 0) 0f else doneToday.toFloat() / totalToday,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        if (stats.isEmpty()) {
            item {
                Text(
                    text = "还没有习惯，点击右下角 + 创建第一个吧",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 40.dp).fillMaxWidth()
                )
            }
        }

        items(stats, key = { it.habit.id }) { stat ->
            HabitCard(
                stat = stat,
                onToggle = { onToggle(stat.habit.id) },
                onIncrement = { onIncrement(stat.habit.id) },
                onArchive = { onArchive(stat.habit.id, !stat.habit.archived) },
                onDelete = { onDelete(stat.habit.id) }
            )
        }
    }
}

@Composable
private fun HabitCard(
    stat: HabitStat,
    onToggle: () -> Unit,
    onIncrement: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val accent = Color(stat.habit.color)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (stat.checkedToday) accent else accent.copy(alpha = 0.18f))
                        .clickable { onToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    if (stat.checkedToday) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "已完成",
                            tint = Color.White
                        )
                    } else {
                        Text(stat.habit.emoji, style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stat.habit.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = if (stat.streak > 0) Color(0xFFFF7043) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "连续 ${stat.streak} 天 · 最佳 ${stat.best} · 30天 ${stat.last30Rate}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (stat.habit.targetPerPeriod > 1) {
                    Text(
                        text = "${stat.todayCount}/${stat.habit.targetPerPeriod}",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        modifier = Modifier.clickable { onIncrement() }.padding(horizontal = 6.dp)
                    )
                }

                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (stat.habit.archived) "取消归档" else "归档") },
                            onClick = { menu = false; onArchive() }
                        )
                        DropdownMenuItem(
                            text = { Text("删除") },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                            onClick = { menu = false; onDelete() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HeatStrip(days = stat.last30, accent = accent)

            if (stat.habit.note.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stat.habit.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** 最近 30 天热力条 */
@Composable
private fun HeatStrip(days: List<Boolean>, accent: Color) {
    val empty = MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        days.forEach { done ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (done) accent else empty)
            )
        }
    }
}

@Composable
private fun GoalList(
    goals: List<GoalEntity>,
    onToggleDone: (GoalEntity) -> Unit,
    onProgress: (GoalEntity, Double) -> Unit,
    onAddMilestone: (GoalEntity, String) -> Unit,
    onToggleMilestone: (GoalEntity, Int) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (goals.isEmpty()) {
            item {
                Text(
                    text = "还没有目标，点击右下角 + 设定一个长期目标",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 40.dp).fillMaxWidth()
                )
            }
        }
        items(goals, key = { it.id }) { goal ->
            GoalCard(
                goal = goal,
                onToggleDone = { onToggleDone(goal) },
                onProgress = { onProgress(goal, it) },
                onAddMilestone = { onAddMilestone(goal, it) },
                onToggleMilestone = { onToggleMilestone(goal, it) },
                onDelete = { onDelete(goal.id) }
            )
        }
    }
}

@Composable
private fun GoalCard(
    goal: GoalEntity,
    onToggleDone: () -> Unit,
    onProgress: (Double) -> Unit,
    onAddMilestone: (String) -> Unit,
    onToggleMilestone: (Int) -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    var showMilestoneInput by remember { mutableStateOf(false) }
    var milestoneText by remember { mutableStateOf("") }

    val milestones = remember(goal.milestones) { HabitViewModel.parseMilestones(goal.milestones) }
    val ratio = if (goal.targetValue <= 0.0) 0f
    else (goal.currentValue / goal.targetValue).coerceIn(0.0, 1.0).toFloat()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = goal.completed, onCheckedChange = { onToggleDone() })
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (goal.description.isNotBlank()) {
                        Text(
                            text = goal.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("添加里程碑") },
                            onClick = { menu = false; showMilestoneInput = true }
                        )
                        DropdownMenuItem(
                            text = { Text("删除目标") },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                            onClick = { menu = false; onDelete() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = ratio,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${goal.currentValue.roundToInt()} / ${goal.targetValue.roundToInt()} ${goal.unit}  ·  ${(ratio * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.25, 0.5, 0.75, 1.0).forEach { p ->
                    FilterChip(
                        selected = ratio >= p.toFloat() - 0.001f,
                        onClick = { onProgress(goal.targetValue * p) },
                        label = { Text("${(p * 100).roundToInt()}%") }
                    )
                }
            }

            if (milestones.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Divider()
                milestones.forEachIndexed { index, m ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { onToggleMilestone(index) }
                    ) {
                        Checkbox(checked = m.done, onCheckedChange = { onToggleMilestone(index) })
                        Text(
                            text = m.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (m.done) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    if (showMilestoneInput) {
        AlertDialog(
            onDismissRequest = { showMilestoneInput = false; milestoneText = "" },
            title = { Text("添加里程碑") },
            text = {
                OutlinedTextField(
                    value = milestoneText,
                    onValueChange = { milestoneText = it },
                    label = { Text("里程碑名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onAddMilestone(milestoneText)
                    milestoneText = ""
                    showMilestoneInput = false
                }) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showMilestoneInput = false; milestoneText = "" }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, Int, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("\uD83D\uDCAA") }
    var note by remember { mutableStateOf("") }
    var target by remember { mutableStateOf(1) }
    var colorIndex by remember { mutableStateOf(0) }

    val palette = listOf(0xFF4CAF50L, 0xFF2196F3L, 0xFFFF9800L, 0xFFE91E63L, 0xFF9C27B0L)
    val emojis = listOf("\uD83D\uDCAA", "\uD83D\uDCDA", "\uD83C\uDFC3", "\uD83D\uDCA7", "\uD83D\uDE34", "\u270D\uFE0F")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建习惯") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("习惯名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("图标", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    emojis.forEach { e ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (emoji == e) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { emoji = e },
                            contentAlignment = Alignment.Center
                        ) { Text(e) }
                    }
                }
                Text("颜色", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    palette.forEachIndexed { i, c ->
                        Box(
                            modifier = Modifier
                                .size(if (colorIndex == i) 32.dp else 26.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .clickable { colorIndex = i }
                        )
                    }
                }
                Text("每日目标次数：$target", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3, 5, 8).forEach { t ->
                        FilterChip(
                            selected = target == t,
                            onClick = { target = t },
                            label = { Text("$t") }
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(name, emoji, palette[colorIndex], 1, target, note)
            }) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("100") }
    var unit by remember { mutableStateOf("%") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建目标") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("目标名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("描述（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("目标值") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("单位") },
                        singleLine = true,
                        modifier = Modifier.width(90.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(title, desc, targetText.toDoubleOrNull() ?: 100.0, unit)
            }) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
