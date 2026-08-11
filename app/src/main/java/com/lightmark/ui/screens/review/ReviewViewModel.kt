package com.lightmark.ui.screens.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.local.dao.CategoryDao
import com.lightmark.data.local.dao.TodoDao
import com.lightmark.domain.model.TodoItem
import com.lightmark.ui.components.priorityLabelOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * 回顾与复盘（#83 / #94 / #121）
 *
 * 自动汇总本周 / 本月的完成情况，并给出可直接执行的清理建议。
 */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val todoDao: TodoDao,
    categoryDao: CategoryDao
) : ViewModel() {

    private val _period = MutableStateFlow(ReviewPeriod.WEEK)
    val period: StateFlow<ReviewPeriod> = _period.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val categoryNames = categoryDao.getAllCategories()
        .map { list -> list.associate { it.id to it.name } }

    val report: StateFlow<ReviewReport> = combine(
        todoDao.getAllTodos(),
        _period,
        categoryNames
    ) { entities, period, names ->
        buildReport(entities.map { it.toDomain() }.filter { !it.isDeleted }, period, names)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReviewReport())

    fun setPeriod(value: ReviewPeriod) {
        _period.value = value
    }

    fun consumeMessage() {
        _message.value = null
    }

    /** 把逾期未完成的任务统一顺延到今天 23:59 */
    fun rescheduleOverdueToToday() {
        viewModelScope.launch {
            val endOfToday = LocalDate.now()
                .atTime(23, 59)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            val now = System.currentTimeMillis()
            var count = 0
            todoDao.getAllTodosList().forEach { entity ->
                val due = entity.dueDate
                if (!entity.isDeleted && !entity.isCompleted && due != null && due < now) {
                    todoDao.updateTodo(entity.copy(dueDate = endOfToday, updatedAt = now))
                    count++
                }
            }
            _message.value = if (count > 0) "已把 $count 条逾期任务顺延到今天" else "没有逾期任务"
        }
    }

    /** 归档周期内已完成的任务，让列表回到干净状态 */
    fun archiveCompletedInPeriod() {
        viewModelScope.launch {
            val start = _period.value.startMillis()
            val now = System.currentTimeMillis()
            var count = 0
            todoDao.getAllTodosList().forEach { entity ->
                val done = entity.completedAt
                if (!entity.isDeleted && !entity.isArchived && entity.isCompleted &&
                    done != null && done >= start
                ) {
                    todoDao.updateTodo(entity.copy(isArchived = true, updatedAt = now))
                    count++
                }
            }
            _message.value = if (count > 0) "已归档 $count 条完成任务" else "没有可归档的任务"
        }
    }

    private fun buildReport(
        todos: List<TodoItem>,
        period: ReviewPeriod,
        names: Map<String, String>
    ): ReviewReport {
        val start = period.startMillis()
        val now = System.currentTimeMillis()

        val created = todos.count { it.createdAt >= start }
        val completedList = todos.filter { it.isCompleted && (it.completedAt ?: 0L) >= start }
        val completed = completedList.size
        val openList = todos.filter { !it.isCompleted && !it.isArchived }
        val overdue = openList
            .filter { (it.dueDate ?: Long.MAX_VALUE) < now }
            .sortedBy { it.dueDate ?: 0L }

        val rate = if (created + completed == 0) 0
        else (completed * 100.0 / maxOf(created, completed)).toInt()

        val byCategory = completedList
            .groupBy { it.categoryId }
            .map { (id, list) -> (id?.let { names[it] } ?: "未分类") to list.size }
            .sortedByDescending { it.second }
            .take(6)

        val byPriority = completedList
            .groupBy { it.priority }
            .map { (p, list) -> priorityLabelOf(p) to list.size }
            .sortedByDescending { it.second }

        val dayCounts = completedList.mapNotNull { it.completedAt }
            .groupingBy { millisToDate(it) }
            .eachCount()
        val busiest = dayCounts.maxByOrNull { it.value }
        val busiestDay = busiest?.let { "${it.key.monthValue}/${it.key.dayOfMonth}（${it.value} 条）" } ?: "—"

        val durations = completedList.mapNotNull { item ->
            val done = item.completedAt ?: return@mapNotNull null
            val span = done - item.createdAt
            if (span in 0..(365L * 86_400_000L)) span else null
        }
        val avgHours = if (durations.isEmpty()) 0.0
        else durations.sum() / durations.size / 3_600_000.0

        // 连续有完成记录的天数（从今天往前推）
        var streak = 0
        var cursor = LocalDate.now()
        val doneDays = completedList.mapNotNull { it.completedAt }.map { millisToDate(it) }.toSet()
        while (doneDays.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }

        val noDate = openList.count { it.dueDate == null }

        return ReviewReport(
            periodLabel = period.label,
            created = created,
            completed = completed,
            completionRate = rate.coerceIn(0, 100),
            openCount = openList.size,
            noDateCount = noDate,
            overdue = overdue.take(20),
            overdueTotal = overdue.size,
            byCategory = byCategory,
            byPriority = byPriority,
            busiestDay = busiestDay,
            avgCompleteHours = avgHours,
            streakDays = streak
        )
    }

    private fun millisToDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
}

enum class ReviewPeriod(val label: String) {
    WEEK("本周"),
    MONTH("本月"),
    QUARTER("近 90 天");

    fun startMillis(): Long {
        val date = when (this) {
            WEEK -> LocalDate.now().minusDays(6)
            MONTH -> LocalDate.now().withDayOfMonth(1)
            QUARTER -> LocalDate.now().minusDays(89)
        }
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}

data class ReviewReport(
    val periodLabel: String = "本周",
    val created: Int = 0,
    val completed: Int = 0,
    val completionRate: Int = 0,
    val openCount: Int = 0,
    val noDateCount: Int = 0,
    val overdue: List<TodoItem> = emptyList(),
    val overdueTotal: Int = 0,
    val byCategory: List<Pair<String, Int>> = emptyList(),
    val byPriority: List<Pair<String, Int>> = emptyList(),
    val busiestDay: String = "—",
    val avgCompleteHours: Double = 0.0,
    val streakDays: Int = 0
) {
    /** 生成可复制的纯文本周报 */
    fun toPlainText(): String = buildString {
        append("【轻刻 · $periodLabel 复盘】\n\n")
        append("完成 $completed 条，新建 $created 条，完成率 $completionRate%\n")
        append("待办剩余 $openCount 条，其中 $noDateCount 条没有安排日期\n")
        append("逾期未完成 $overdueTotal 条\n")
        append("连续完成天数 $streakDays 天\n")
        append("最高产的一天：$busiestDay\n")
        if (avgCompleteHours > 0) {
            append("平均从创建到完成 ${String.format("%.1f", avgCompleteHours)} 小时\n")
        }
        if (byCategory.isNotEmpty()) {
            append("\n按分类：\n")
            byCategory.forEach { append("  · ${it.first} ${it.second} 条\n") }
        }
        if (byPriority.isNotEmpty()) {
            append("\n按优先级：\n")
            byPriority.forEach { append("  · ${it.first} ${it.second} 条\n") }
        }
        if (overdue.isNotEmpty()) {
            append("\n需要处理的逾期任务：\n")
            overdue.take(10).forEach { append("  · ${it.title}\n") }
        }
    }
}
