package com.lightmark.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lightmark.domain.model.Priority
import com.lightmark.domain.model.TodoItem

/**
 * Room 待办事项实体
 */
@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: String = Priority.MEDIUM.name,
    val categoryId: String? = null,
    val tags: String = "",     // 用逗号分隔存储
    val dueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    /** 转换为领域模型 */
    fun toDomain(): TodoItem = TodoItem(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted,
        priority = Priority.fromString(priority),
        categoryId = categoryId,
        tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() },
        dueDate = dueDate,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt
    )

    companion object {
        /** 从领域模型转换 */
        fun fromDomain(item: TodoItem): TodoEntity = TodoEntity(
            id = item.id,
            title = item.title,
            description = item.description,
            isCompleted = item.isCompleted,
            priority = item.priority.name,
            categoryId = item.categoryId,
            tags = item.tags.joinToString(","),
            dueDate = item.dueDate,
            createdAt = item.createdAt,
            updatedAt = item.updatedAt,
            completedAt = item.completedAt
        )
    }
}
