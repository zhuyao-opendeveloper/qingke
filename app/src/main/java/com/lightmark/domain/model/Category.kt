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
    val isPrivate: Boolean = false, // 是否私密（开启后需解锁才能查看）
    val parentId: String? = null, // 父分类ID（文件夹层级，#27）
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun generateId(): String =
            "cat_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}
