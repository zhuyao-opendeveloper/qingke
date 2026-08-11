package com.lightmark.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lightmark.domain.model.SmartList

/**
 * 自定义智能清单实体（#28）
 */
@Entity(tableName = "smart_lists")
data class SmartListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String = "📋",
    val quickFilter: String? = null,
    val categoryId: String? = null,
    val query: String? = null,
    val sortOrder: String? = null,
    val order: Int = 0
) {
    fun toDomain(): SmartList = SmartList(
        id = id,
        name = name,
        emoji = emoji,
        quickFilter = quickFilter,
        categoryId = categoryId,
        query = query,
        sortOrder = sortOrder,
        order = order
    )

    companion object {
        fun fromDomain(s: SmartList): SmartListEntity = SmartListEntity(
            id = s.id,
            name = s.name,
            emoji = s.emoji,
            quickFilter = s.quickFilter,
            categoryId = s.categoryId,
            query = s.query,
            sortOrder = s.sortOrder,
            order = s.order
        )
    }
}
