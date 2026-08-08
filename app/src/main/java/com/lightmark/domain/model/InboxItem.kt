package com.lightmark.domain.model

import kotlinx.serialization.Serializable

/**
 * 收集箱条目：用于快速捕获灵感 / 待办，稍后再整理
 */
@Serializable
data class InboxItem(
    val id: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isDone: Boolean = false
)
