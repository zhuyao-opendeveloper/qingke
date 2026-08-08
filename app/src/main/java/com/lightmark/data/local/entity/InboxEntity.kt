package com.lightmark.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lightmark.domain.model.InboxItem

/**
 * 收集箱 Room 实体
 */
@Entity(tableName = "inbox")
data class InboxEntity(
    @PrimaryKey val id: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isDone: Boolean = false
) {
    fun toDomain(): InboxItem = InboxItem(
        id = id,
        content = content,
        createdAt = createdAt,
        isDone = isDone
    )

    companion object {
        fun fromDomain(item: InboxItem): InboxEntity = InboxEntity(
            id = item.id,
            content = item.content,
            createdAt = item.createdAt,
            isDone = item.isDone
        )
    }
}
