package com.lightmark.domain.ai

import com.lightmark.domain.model.AiSuggestion
import com.lightmark.domain.model.Priority
import com.lightmark.domain.model.TodoItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 轻刻本地智能服务
 *
 * 能力（全部在设备本机完成，不联网、不上传任何内容）：
 * - 智能填写：根据标题/描述推断分类、标签、优先级、建议截止天数
 * - 文本润色：规范空白与标点
 * - 智能总结：提炼首句要点
 * - 一句话生成待办：从自然语言拆分成多条待办
 * - 对话式引导
 *
 * 说明：轻刻自 v2.0.0 起为完全离线应用，已移除全部大模型在线调用。
 * 需要大模型能力（长文对话、深度改写）请使用轻刻网页版。
 */
@Singleton
class AiService @Inject constructor() {

    /**
     * 智能填写：根据标题与描述给出建议。
     */
    fun suggestForTodo(title: String, description: String): AiSuggestion =
        offlineSuggest(title, description)

    /**
     * 文本润色：规范空白与结尾标点。
     */
    fun polishText(text: String): String {
        if (text.isBlank()) return text
        return offlinePolish(text)
    }

    /**
     * 智能总结。
     */
    fun summarize(text: String): String {
        if (text.isBlank()) return ""
        return offlineSummarize(text)
    }

    /**
     * 一句话生成待办：把自然语言拆成多条待办。
     */
    fun generateTodos(prompt: String): List<TodoItem> = parseTodoLines(prompt)

    /**
     * 对话式引导（本地规则应答）。
     */
    fun chat(prompt: String): String = offlineChat(prompt)

    // ===== 本地规则实现 =====

    private fun offlineSuggest(title: String, description: String): AiSuggestion {
        val text = "$title $description".lowercase()
        val category = when {
            listOf("工作", "会议", "项目", "报告", "客户", "周报", "需求", "bug", "上线").any { text.contains(it) } -> "工作"
            listOf("学习", "考试", "读书", "课程", "复习", "论文", "单词", "作业").any { text.contains(it) } -> "学习"
            listOf("健身", "运动", "跑步", "减肥", "锻炼", "瑜伽", "体检").any { text.contains(it) } -> "健康"
            listOf("购物", "买", "超市", "囤", "下单").any { text.contains(it) } -> "生活"
            listOf("旅行", "旅游", "机票", "酒店", "行程").any { text.contains(it) } -> "旅行"
            else -> null
        }

        val tagHints = listOf(
            "紧急" to "紧急", "重要" to "重要", "电话" to "电话", "邮件" to "邮件",
            "会议" to "会议", "复习" to "复习", "购置" to "购置", "亲子" to "亲子"
        )
        val suggestedTags = tagHints.filter { text.contains(it.first) }.map { it.second }

        val suggestedPriority = when {
            listOf("紧急", "马上", "立刻", "urgent", "尽快", "必须").any { text.contains(it) } -> Priority.URGENT
            listOf("重要", "关键", "务必").any { text.contains(it) } -> Priority.HIGH
            listOf("也许", "有空", "随便", "不急").any { text.contains(it) } -> Priority.LOW
            else -> Priority.MEDIUM
        }

        val suggestedDueDate = when {
            text.contains("今天") || text.contains("今日") -> 0
            text.contains("明天") -> 1
            text.contains("后天") -> 2
            text.contains("下周") || text.contains("周内") -> 7
            text.contains("周末") -> 5
            else -> null
        }

        return AiSuggestion(
            suggestedCategory = category,
            suggestedTags = suggestedTags,
            suggestedPriority = suggestedPriority,
            suggestedDueDate = suggestedDueDate,
            confidence = 0.65f
        )
    }

    private fun offlinePolish(text: String): String {
        val collapsed = text.replace(Regex("\\s+"), " ").trim()
        return if (collapsed.isNotEmpty() && !Regex("[。！？.!?]$").containsMatchIn(collapsed)) {
            "$collapsed。"
        } else collapsed
    }

    private fun offlineSummarize(text: String): String {
        val firstSentence = text.split(Regex("(?<=[。！？.!?])"), limit = 2).first().trim()
        return if (firstSentence.length <= 60) firstSentence else firstSentence.take(57) + "..."
    }

    private fun offlineChat(prompt: String): String {
        val trimmed = prompt.trim()
        val hint = when {
            trimmed.contains("怎么") || trimmed.contains("如何") || trimmed.contains("?") || trimmed.contains("？") ->
                "试试把它拆成几个具体动作，我可以直接帮你生成待办——在输入框里描述一句话，点「生成待办」即可。"
            trimmed.length > 30 ->
                "这段内容有点长，可以点「智能总结」提炼要点，或点「生成待办」拆成可执行的条目。"
            else ->
                "我可以帮你：智能填写分类与优先级、润色描述、提炼摘要、把一句话拆成多条待办。"
        }
        return "轻刻运行在完全离线模式，以下由本机规则给出：\n" +
            "你说的是「${trimmed.take(40)}${if (trimmed.length > 40) "…" else ""}」\n" +
            hint
    }

    private fun parseTodoLines(source: String): List<TodoItem> {
        return source.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val content = line.removePrefix("-").removePrefix("•").removePrefix("*")
                    .replace(Regex("^\\d+[.、)]"), "").trim()
                if (content.isBlank()) null else content
            }
            .distinct()
            .map { TodoItem(title = it) }
    }
}
