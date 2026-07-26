package com.lightmark.domain.model

import kotlinx.serialization.Serializable

/**
 * 待办事项优先级
 */
enum class Priority {
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
 * 待办事项数据模型
 *
 * @property id 唯一标识
 * @property title 标题
 * @property description 描述
 * @property isCompleted 是否已完成
 * @property priority 优先级
 * @property categoryId 分类ID
 * @property tags 标签列表
 * @property dueDate 截止日期（时间戳毫秒）
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
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    companion object {
        fun generateId(): String =
            "todo_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}
