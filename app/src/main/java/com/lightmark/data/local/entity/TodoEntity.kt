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
    val startDate: Long? = null,
    val isPinned: Boolean = false, // 是否置顶
    val isBlocked: Boolean = false, // 是否被外部阻塞
    val status: String = "ACTIVE", // 生命周期状态
    val isArchived: Boolean = false, // 是否已归档
    val isDeleted: Boolean = false, // 是否已软删除（回收站）
    val deletedAt: Long? = null, // 移入回收站时间
    val parentId: String? = null, // 父任务ID（子任务）
    val isPrivate: Boolean = false, // 是否私密（需解锁才能查看）
    val recurrenceRule: String? = null, // 重复规则
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val manualOrder: Int = 0 // 手动排序序号（#32）
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
        startDate = startDate,
        isPinned = isPinned,
        isBlocked = isBlocked,
        status = status,
        isArchived = isArchived,
        isDeleted = isDeleted,
        deletedAt = deletedAt,
            parentId = parentId,
            isPrivate = isPrivate,
            recurrenceRule = recurrenceRule,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt,
        manualOrder = manualOrder
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
            startDate = item.startDate,
            isPinned = item.isPinned,
            isBlocked = item.isBlocked,
            status = item.status,
            isArchived = item.isArchived,
            isDeleted = item.isDeleted,
            deletedAt = item.deletedAt,
            parentId = item.parentId,
            isPrivate = item.isPrivate,
            recurrenceRule = item.recurrenceRule,
            createdAt = item.createdAt,
            updatedAt = item.updatedAt,
            completedAt = item.completedAt,
            manualOrder = item.manualOrder
        )
    }
}
