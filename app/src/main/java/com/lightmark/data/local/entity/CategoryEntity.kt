package com.lightmark.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lightmark.domain.model.Category

/**
 * Room 分类实体
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: Long = 0xFF6200EE,
    val icon: String = "folder",
    val isPrivate: Boolean = false, // 是否私密
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Category = Category(
        id = id,
        name = name,
        color = color,
        icon = icon,
        isPrivate = isPrivate,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(cat: Category): CategoryEntity = CategoryEntity(
            id = cat.id,
            name = cat.name,
            color = cat.color,
            icon = cat.icon,
            isPrivate = cat.isPrivate,
            createdAt = cat.createdAt
        )
    }
}
