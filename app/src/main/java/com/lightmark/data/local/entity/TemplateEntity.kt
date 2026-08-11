package com.lightmark.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 任务模板
 *
 * subtasks: 每行一个子任务标题
 * dueInDays: 生成任务时的相对到期天数（null 表示不设置截止日期）
 */
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val emoji: String = "\uD83D\uDCCB",
    val categoryId: String? = null,
    val tags: String = "",
    val priority: String = "MEDIUM",
    val subtasks: String = "",
    val dueInDays: Int? = null,
    val recurrenceRule: String? = null,
    val builtIn: Boolean = false,
    val usageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** 子任务标题列表 */
    val subtaskList: List<String>
        get() = subtasks.split("\n").map { it.trim() }.filter { it.isNotBlank() }

    /** 标签列表 */
    val tagList: List<String>
        get() = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
}
