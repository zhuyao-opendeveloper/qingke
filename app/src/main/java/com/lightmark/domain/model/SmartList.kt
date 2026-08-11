package com.lightmark.domain.model

/**
 * 自定义智能清单（#28）
 *
 * 把一组筛选条件（快速筛选 / 分类 / 关键词 / 排序）保存为一个命名清单，
 * 在首页一键套用。纯本地，不涉及任何网络能力。
 */
data class SmartList(
    val id: String = "sl_${System.currentTimeMillis()}_${(0..9999).random()}",
    val name: String,
    val emoji: String = "📋",
    /** QuickFilter 枚举名，null 表示不过滤 */
    val quickFilter: String? = null,
    val categoryId: String? = null,
    val query: String? = null,
    /** SortOrder 枚举名，null 表示默认排序 */
    val sortOrder: String? = null,
    val order: Int = 0
)
