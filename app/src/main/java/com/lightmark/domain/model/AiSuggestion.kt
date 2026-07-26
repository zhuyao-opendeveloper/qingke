package com.lightmark.domain.model

import kotlinx.serialization.Serializable

/**
 * AI 智能推荐结果
 *
 * @property suggestedCategory 建议的分类
 * @property suggestedTags 建议的标签
 * @property suggestedPriority 建议的优先级
 * @property suggestedDueDate 建议的截止日期（天）
 * @property confidence 推荐置信度 0-1
 */
@Serializable
data class AiSuggestion(
    val suggestedCategory: String? = null,
    val suggestedTags: List<String> = emptyList(),
    val suggestedPriority: Priority? = null,
    val suggestedDueDate: Int? = null, // 建议几天后截止
    val confidence: Float = 0f
)
