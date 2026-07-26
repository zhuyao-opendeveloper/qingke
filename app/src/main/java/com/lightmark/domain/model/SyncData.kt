package com.lightmark.domain.model

import kotlinx.serialization.Serializable

/**
 * GitHub 数据同步的顶层数据结构
 * 整个待办数据以一个 JSON 文件的形式存储在 GitHub 私有仓库中
 *
 * @property version 数据版本（用于迁移）
 * @property lastSync 上次同步时间
 * @property todos 待办列表
 * @property categories 分类列表
 */
@Serializable
data class SyncData(
    val version: Int = CURRENT_VERSION,
    val lastSync: Long = System.currentTimeMillis(),
    val todos: List<TodoItem> = emptyList(),
    val categories: List<Category> = emptyList()
) {
    companion object {
        const val CURRENT_VERSION = 1
        const val FILE_PATH = "lightmark-data.json"
    }
}
