package com.lightmark.ui.screens.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.local.dao.HabitDao
import com.lightmark.data.local.entity.GoalEntity
import com.lightmark.data.local.entity.HabitCheckEntity
import com.lightmark.data.local.entity.HabitEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * 习惯 + 目标 统计视图模型
 * 覆盖：习惯打卡 / 连续天数 / 完成率 / 目标进度 / 里程碑
 */
@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitDao: HabitDao
) : ViewModel() {

    private val habitsFlow = habitDao.getAllHabits()
    private val checksFlow = habitDao.getAllChecks()

    val stats: StateFlow<List<HabitStat>> = combine(habitsFlow, checksFlow) { habits, checks ->
        val today = LocalDate.now()
        val grouped = checks.groupBy { it.habitId }
        habits.map { habit ->
            buildStat(habit, grouped[habit.id].orEmpty(), today)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<GoalEntity>> = habitDao.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 今日整体完成度：已打卡习惯数 / 未归档习惯数 */
    val todayProgress: StateFlow<Pair<Int, Int>> = stats.map { list ->
        val active = list.filter { !it.habit.archived }
        active.count { it.checkedToday } to active.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0 to 0)

    // ---------------- 习惯操作 ----------------

    fun addHabit(name: String, emoji: String, color: Long, periodDays: Int, target: Int, note: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            habitDao.upsertHabit(
                HabitEntity(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    emoji = emoji.ifBlank { "\u2705" },
                    color = color,
                    periodDays = periodDays.coerceAtLeast(1),
                    targetPerPeriod = target.coerceAtLeast(1),
                    note = note.trim(),
                    sortOrder = System.currentTimeMillis().toInt()
                )
            )
        }
    }

    fun updateHabit(habit: HabitEntity) {
        viewModelScope.launch { habitDao.upsertHabit(habit) }
    }

    fun setArchived(habitId: String, archived: Boolean) {
        viewModelScope.launch { habitDao.setHabitArchived(habitId, archived) }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            habitDao.deleteChecksForHabit(habitId)
            habitDao.deleteHabitById(habitId)
        }
    }

    /** 打卡 / 取消打卡（同一天再次点击则撤销） */
    fun toggleCheck(habitId: String, date: LocalDate = LocalDate.now()) {
        val key = dayKeyOf(date)
        viewModelScope.launch {
            val existing = habitDao.getCheck(habitId, key)
            if (existing != null) {
                habitDao.deleteCheck(habitId, key)
            } else {
                habitDao.upsertCheck(
                    HabitCheckEntity(id = "${habitId}_$key", habitId = habitId, dayKey = key, count = 1)
                )
            }
        }
    }

    /** 多次打卡型习惯：累加一次 */
    fun incrementCheck(habitId: String, date: LocalDate = LocalDate.now()) {
        val key = dayKeyOf(date)
        viewModelScope.launch {
            val existing = habitDao.getCheck(habitId, key)
            val next = (existing?.count ?: 0) + 1
            habitDao.upsertCheck(
                HabitCheckEntity(id = "${habitId}_$key", habitId = habitId, dayKey = key, count = next)
            )
        }
    }

    // ---------------- 目标操作 ----------------

    fun addGoal(title: String, description: String, target: Double, unit: String, dueDate: Long?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            habitDao.upsertGoal(
                GoalEntity(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    description = description.trim(),
                    targetValue = if (target <= 0.0) 100.0 else target,
                    unit = unit.ifBlank { "%" },
                    dueDate = dueDate
                )
            )
        }
    }

    fun updateGoalProgress(goal: GoalEntity, value: Double) {
        viewModelScope.launch {
            val clamped = value.coerceIn(0.0, goal.targetValue)
            habitDao.upsertGoal(
                goal.copy(currentValue = clamped, completed = clamped >= goal.targetValue)
            )
        }
    }

    fun toggleGoalCompleted(goal: GoalEntity) {
        viewModelScope.launch {
            val done = !goal.completed
            habitDao.upsertGoal(
                goal.copy(
                    completed = done,
                    currentValue = if (done) goal.targetValue else goal.currentValue
                )
            )
        }
    }

    fun addMilestone(goal: GoalEntity, title: String) {
        if (title.isBlank()) return
        val line = "0|${title.trim().replace("\n", " ")}"
        val next = if (goal.milestones.isBlank()) line else goal.milestones + "\n" + line
        viewModelScope.launch { habitDao.upsertGoal(goal.copy(milestones = next)) }
    }

    fun toggleMilestone(goal: GoalEntity, index: Int) {
        val items = parseMilestones(goal.milestones).toMutableList()
        if (index !in items.indices) return
        items[index] = items[index].copy(done = !items[index].done)
        val encoded = items.joinToString("\n") { (if (it.done) "1" else "0") + "|" + it.title }
        viewModelScope.launch { habitDao.upsertGoal(goal.copy(milestones = encoded)) }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch { habitDao.deleteGoalById(goalId) }
    }

    // ---------------- 统计 ----------------

    private fun buildStat(habit: HabitEntity, checks: List<HabitCheckEntity>, today: LocalDate): HabitStat {
        val map = checks.associateBy { it.dayKey }
        val todayKey = dayKeyOf(today)
        val todayCount = map[todayKey]?.count ?: 0
        val required = habit.targetPerPeriod.coerceAtLeast(1)
        val checkedToday = todayCount >= required

        // 最近 30 天（旧 -> 新）
        val recent = (29 downTo 0).map { offset ->
            val d = today.minusDays(offset.toLong())
            (map[dayKeyOf(d)]?.count ?: 0) >= required
        }
        val rate = if (recent.isEmpty()) 0 else recent.count { it } * 100 / recent.size

        // 当前连续：今天未打卡时不算断，从昨天往前找
        var streak = 0
        var cursor = if (checkedToday) today else today.minusDays(1)
        while (true) {
            val ok = (map[dayKeyOf(cursor)]?.count ?: 0) >= required
            if (!ok) break
            streak++
            cursor = cursor.minusDays(1)
            if (streak > 3650) break
        }

        // 历史最佳连续
        val sortedKeys = map.filterValues { it.count >= required }.keys.sorted()
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        for (k in sortedKeys) {
            val d = dateOfKey(k)
            run = if (prev != null && prev.plusDays(1) == d) run + 1 else 1
            if (run > best) best = run
            prev = d
        }
        if (streak > best) best = streak

        return HabitStat(
            habit = habit,
            checkedToday = checkedToday,
            todayCount = todayCount,
            streak = streak,
            best = best,
            last30Rate = rate,
            last30 = recent,
            totalChecks = map.values.count { it.count >= required }
        )
    }

    companion object {
        fun dayKeyOf(date: LocalDate): Int =
            date.year * 10000 + date.monthValue * 100 + date.dayOfMonth

        fun dateOfKey(key: Int): LocalDate =
            LocalDate.of(key / 10000, (key / 100) % 100, key % 100)

        fun parseMilestones(raw: String): List<Milestone> =
            raw.lineSequence()
                .filter { it.isNotBlank() }
                .map { line ->
                    val idx = line.indexOf('|')
                    if (idx <= 0) Milestone(line, false)
                    else Milestone(line.substring(idx + 1), line.substring(0, idx) == "1")
                }
                .toList()
    }
}

data class Milestone(val title: String, val done: Boolean)

data class HabitStat(
    val habit: HabitEntity,
    val checkedToday: Boolean,
    val todayCount: Int,
    val streak: Int,
    val best: Int,
    val last30Rate: Int,
    val last30: List<Boolean>,
    val totalChecks: Int
)
