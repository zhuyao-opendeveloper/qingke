package com.lightmark.domain.ai

import com.lightmark.data.remote.openclaw.OpenClawApi
import com.lightmark.data.remote.openclaw.OpenClawClientFactory
import com.lightmark.data.remote.openclaw.OpenClawChatRequest
import com.lightmark.data.remote.openclaw.OpenClawMessage
import com.lightmark.data.settings.SettingsRepository
import com.lightmark.domain.model.AiSuggestion
import com.lightmark.domain.model.Priority
import com.lightmark.domain.model.TodoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 轻刻 AI 服务
 *
 * 能力：
 * - 智能填写：根据标题/描述推断分类、标签、优先级、建议截止天数
 * - 文本润色：优化描述文案
 * - 智能总结：提炼要点
 * - 一句话生成待办：从自然语言拆分成多条待办
 * - 自由对话
 *
 * 当 OpenClaw 已配置（开关开启且填写了 Key）时走在线大模型，
 * 否则使用本地规则兜底，保证离线也可用的「越多越好」体验。
 */
@Singleton
class AiService @Inject constructor(
    private val settings: SettingsRepository
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    /** 当前是否可用在线模型（仅用于 UI 提示，实际调用内部会自行判断） */
    suspend fun isOnlineAvailable(): Boolean {
        val s = settings.currentSettings()
        return s.openClawEnabled && s.openClawApiKey.isNotBlank()
    }

    private var cachedKey: String? = null
    private var cachedClient: OpenClawApi? = null

    private suspend fun client(): OpenClawApi? {
        val s = settings.currentSettings()
        if (!s.openClawEnabled || s.openClawApiKey.isBlank()) return null
        val key = "${s.openClawBaseUrl}::${s.openClawApiKey}::${s.openClawModel}"
        if (cachedKey != key || cachedClient == null) {
            cachedClient = OpenClawClientFactory.create(s.openClawBaseUrl, s.openClawApiKey)
            cachedKey = key
        }
        return cachedClient
    }

    private suspend fun ask(system: String, user: String): String? = withContext(Dispatchers.IO) {
        val api = client() ?: return@withContext null
        runCatching {
            val resp = api.chatCompletion(
                OpenClawChatRequest(
                    model = settings.currentSettings().openClawModel,
                    messages = listOf(
                        OpenClawMessage("system", system),
                        OpenClawMessage("user", user)
                    )
                )
            )
            resp.choices.firstOrNull()?.message?.content?.trim()
        }.getOrNull()
    }

    /**
     * 智能填写：根据标题与描述给出建议。
     */
    suspend fun suggestForTodo(title: String, description: String): AiSuggestion {
        val online = ask(
            system = "你是待办管理助手。根据用户给出的标题和描述，推断合适的分类名、标签、优先级和截止天数。" +
                "只输出 JSON，格式：{\"suggestedCategory\":\"工作|学习|健康|生活|旅行|null\"," +
                "\"suggestedTags\":[\"标签1\",\"标签2\"],\"suggestedPriority\":\"LOW|MEDIUM|HIGH|URGENT\"," +
                "\"suggestedDueDate\":数字或null}。不要解释。",
            user = "标题：$title\n描述：$description"
        )
        if (!online.isNullOrBlank()) {
            runCatching {
                val cleaned = extractJsonObject(online)
                if (cleaned != null) return json.decodeFromString<AiSuggestion>(cleaned)
            }
        }
        return offlineSuggest(title, description)
    }

    /**
     * 文本润色：优化描述。
     */
    suspend fun polishText(text: String): String {
        if (text.isBlank()) return text
        val online = ask(
            system = "你是一名中文文案润色助手。只返回润色后的文本本身，不要任何解释或引号。",
            user = "请润色以下内容：\n$text"
        )
        if (!online.isNullOrBlank()) return online
        return offlinePolish(text)
    }

    /**
     * 智能总结。
     */
    suspend fun summarize(text: String): String {
        if (text.isBlank()) return ""
        val online = ask(
            system = "你是一名摘要助手。用一句简洁中文概括要点，不超过 60 字，只返回摘要本身。",
            user = "请总结：\n$text"
        )
        if (!online.isNullOrBlank()) return online
        return offlineSummarize(text)
    }

    /**
     * 一句话生成待办：把自然语言拆成多条待办。
     */
    suspend fun generateTodos(prompt: String): List<TodoItem> {
        val online = ask(
            system = "你是待办拆解助手。把用户的需求拆成可执行的待办事项。" +
                "只输出每行一条待办，用 - 开头，例如：\n- 买菜\n- 写周报。不要解释。",
            user = prompt
        )
        val source = if (!online.isNullOrBlank()) online else prompt
        return parseTodoLines(source)
    }

    /**
     * 自由对话（单轮）。无配置时返回引导文案。
     */
    suspend fun chat(prompt: String): String {
        val online = ask(
            system = "你是轻刻（LightMark）应用的智能助手，帮助用户管理待办、规划任务、回答问题。",
            user = prompt
        )
        if (!online.isNullOrBlank()) return online
        return offlineChat(prompt)
    }

    // ===== 离线兜底 =====

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
            listOf("紧急", "马上", "立刻", "立刻", "urgent", "尽快", "必须").any { text.contains(it) } -> Priority.URGENT
            listOf("重要", "关键", "务必").any { text.contains(it) } -> Priority.HIGH
            listOf("也许", "有空", "随便", "不急").any { text.contains(it) } -> Priority.LOW
            else -> Priority.MEDIUM
        }

        val suggestedDueDate = when {
            text.contains("今天") || text.contains("今日") -> 0
            text.contains("明天") -> 1
            text.contains("后天") -> 2
            text.contains("下周") || text.contains("周內") -> 7
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
        return "（当前未配置 OpenClaw，使用本地模式）\n" +
            "我理解你想说：${prompt.take(40)}${if (prompt.length > 40) "…" else ""}\n" +
            "在「设置 → 集成」中填入 OpenClaw 的 Base URL 与 API Key 后，我就能调用大模型为你生成待办、润色与对话。"
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

    private fun extractJsonObject(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return text.substring(start, end + 1)
    }
}
