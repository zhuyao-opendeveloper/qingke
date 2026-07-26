package com.lightmark.domain.model

import kotlinx.serialization.Serializable

/**
 * 待办分类
 */
@Serializable
data class Category(
    val id: String = generateId(),
    val name: String,
    val color: Long = 0xFF6200EE, // 颜色值
    val icon: String = "folder",  // 图标名称
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun generateId(): String =
            "cat_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}
