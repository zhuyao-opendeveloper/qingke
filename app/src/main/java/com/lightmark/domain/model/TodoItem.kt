package com.lightmark.domain.model

import kotlinx.serialization.Serializable

/**
 * 待办事项优先级（5 级：空闲 < 低 < 中 < 高 < 紧急）
 */
enum class Priority {
    IDLE,       // 空闲（P4，不做也行）
    LOW,        // 低优先级
    MEDIUM,     // 中优先级
    HIGH,       // 高优先级
    URGENT;     // 紧急

    companion object {
        fun fromString(value: String): Priority =
            runCatching { valueOf(value) }.getOrDefault(MEDIUM)
    }
}

/**
 * 任务生命周期状态
 */
enum class TodoStatus {
    ACTIVE,     // 进行中（默认）
    PAUSED,     // 暂停（保留记录但不活跃）
    CANCELLED;  // 已取消（保留记录但不活跃）

    companion object {
        fun fromString(value: String): TodoStatus =
            runCatching { valueOf(value) }.getOrDefault(ACTIVE)
    }
}

/**
 * 重复规则常量（存储为字符串）
 */
object Recurrence {
    const val NONE = "NONE"
    const val DAILY = "DAILY"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"
    const val INTERVAL_PREFIX = "INTERVAL:" // 后接天数，如 INTERVAL:3 表示每 3 天

    /** 人类可读标签 */
    fun label(rule: String?): String = when {
        rule == null || rule == NONE -> "不重复"
        rule == DAILY -> "每天"
        rule == WEEKLY -> "每周"
        rule == MONTHLY -> "每月"
        rule.startsWith(INTERVAL_PREFIX) -> "每 ${rule.removePrefix(INTERVAL_PREFIX)} 天"
        else -> "自定义"
    }

    /** 根据规则计算下一次触发的时间戳（毫秒）；无法计算时返回 null */
    fun nextOccurrence(rule: String?, from: Long): Long? {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = from }
        return when {
            rule == null || rule == NONE -> null
            rule == DAILY -> { cal.add(java.util.Calendar.DAY_OF_MONTH, 1); cal.timeInMillis }
            rule == WEEKLY -> { cal.add(java.util.Calendar.WEEK_OF_YEAR, 1); cal.timeInMillis }
            rule == MONTHLY -> { cal.add(java.util.Calendar.MONTH, 1); cal.timeInMillis }
            rule.startsWith(INTERVAL_PREFIX) -> {
                val days = rule.removePrefix(INTERVAL_PREFIX).toIntOrNull() ?: return null
                if (days <= 0) return null
                cal.add(java.util.Calendar.DAY_OF_MONTH, days); cal.timeInMillis
            }
            else -> null
        }
    }
}

/**
 * 待办事项数据模型
 *
 * @property id 唯一标识
 * @property title 标题
 * @property description 描述（支持 Markdown 轻量语法）
 * @property isCompleted 是否已完成
 * @property priority 优先级（5 级）
 * @property categoryId 分类ID
 * @property tags 标签列表
 * @property dueDate 截止日期（时间戳毫秒）
 * @property startDate 开始日期（计划执行时间）
 * @property isPinned 是否置顶
 * @property isBlocked 是否被外部阻塞（暂停计时）
 * @property status 生命周期状态（ACTIVE/PAUSED/CANCELLED）
 * @property isArchived 是否已归档
 * @property isDeleted 是否已移入回收站（软删除）
 * @property deletedAt 移入回收站的时间
 * @property parentId 父任务ID（用于多级子任务；null 表示顶层任务）
 * @property recurrenceRule 重复规则（见 Recurrence 常量）
 * @property createdAt 创建时间
 * @property updatedAt 更新时间
 * @property completedAt 完成时间
 */
@Serializable
data class TodoItem(
    val id: String = generateId(),
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val categoryId: String? = null,
    val tags: List<String> = emptyList(),
    val dueDate: Long? = null,
    val startDate: Long? = null,
    val isPinned: Boolean = false,
    val isBlocked: Boolean = false,
    val status: String = TodoStatus.ACTIVE.name,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val parentId: String? = null,
    val isPrivate: Boolean = false, // 是否私密（开启后需解锁才能查看）
    val recurrenceRule: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val manualOrder: Int = 0 // 手动排序序号（#32）
) {
    companion object {
        fun generateId(): String =
            "todo_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}
