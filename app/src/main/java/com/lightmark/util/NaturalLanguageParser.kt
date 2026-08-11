package com.lightmark.util

import com.lightmark.domain.model.Priority
import com.lightmark.domain.model.Recurrence
import java.util.Calendar

/**
 * 自然语言解析结果（功能 #2）
 */
data class ParsedTask(
    val title: String,
    val dueDate: Long? = null,
    val tags: List<String> = emptyList(),
    val categoryName: String? = null,
    val priority: Priority? = null,
    val recurrenceRule: String? = null
) {
    /** 是否解析出了标题以外的信息 */
    val hasExtras: Boolean
        get() = dueDate != null || tags.isNotEmpty() || categoryName != null ||
            priority != null || recurrenceRule != null
}

/**
 * 中文自然语言任务解析器（功能 #2）
 *
 * 支持：
 * - 标签：`#紧急`、`#工作`
 * - 清单/分类：`@工作`、`@个人`
 * - 优先级：紧急 / 重要 / 高 / 中 / 低 / 空闲 / p0~p4 / !!! 
 * - 重复：每天、每日、每周、每月、每工作日、每 3 天
 * - 日期：今天、明天、后天、大后天、N 天后、周一~周日、下周三、3月5日、3/5
 * - 时间：上午/下午/晚上/中午 + 3点半 / 15:30 / 15点20分
 *
 * 例："明天下午3点交报告 @工作 #紧急 每周"
 */
object NaturalLanguageParser {

    private val WEEKDAY_MAP = mapOf(
        "一" to Calendar.MONDAY, "二" to Calendar.TUESDAY, "三" to Calendar.WEDNESDAY,
        "四" to Calendar.THURSDAY, "五" to Calendar.FRIDAY, "六" to Calendar.SATURDAY,
        "日" to Calendar.SUNDAY, "天" to Calendar.SUNDAY, "7" to Calendar.SUNDAY,
        "1" to Calendar.MONDAY, "2" to Calendar.TUESDAY, "3" to Calendar.WEDNESDAY,
        "4" to Calendar.THURSDAY, "5" to Calendar.FRIDAY, "6" to Calendar.SATURDAY
    )

    fun parse(raw: String, now: Long = System.currentTimeMillis()): ParsedTask {
        var text = raw

        // ---- 标签 #xxx ----
        val tags = mutableListOf<String>()
        Regex("#([^\\s#@]{1,20})").findAll(text).forEach { tags.add(it.groupValues[1]) }
        text = text.replace(Regex("#([^\\s#@]{1,20})"), " ")

        // ---- 分类 @xxx ----
        var categoryName: String? = null
        Regex("@([^\\s#@]{1,20})").find(text)?.let { categoryName = it.groupValues[1] }
        text = text.replace(Regex("@([^\\s#@]{1,20})"), " ")

        // ---- 重复规则 ----
        var recurrence: String? = null
        val intervalMatch = Regex("每\\s*(\\d{1,3})\\s*天").find(text)
        when {
            intervalMatch != null -> {
                recurrence = Recurrence.INTERVAL_PREFIX + intervalMatch.groupValues[1]
                text = text.replace(intervalMatch.value, " ")
            }
            Regex("每工作日").containsMatchIn(text) -> {
                recurrence = Recurrence.DAILY
                text = text.replace("每工作日", " ")
            }
            Regex("每[天日]").containsMatchIn(text) -> {
                recurrence = Recurrence.DAILY
                text = text.replace(Regex("每[天日]"), " ")
            }
            Regex("每[周星]期?").containsMatchIn(text) && !Regex("每周[一二三四五六日]").containsMatchIn(text) -> {
                recurrence = Recurrence.WEEKLY
                text = text.replace(Regex("每[周星]期?"), " ")
            }
            Regex("每周[一二三四五六日]").containsMatchIn(text) -> {
                recurrence = Recurrence.WEEKLY
                // 保留星期信息给日期解析，仅去掉"每"
                text = text.replaceFirst("每", " ")
            }
            Regex("每月").containsMatchIn(text) -> {
                recurrence = Recurrence.MONTHLY
                text = text.replace("每月", " ")
            }
        }

        // ---- 优先级 ----
        var priority: Priority? = null
        val priorityRules = listOf(
            Triple(Regex("(紧急|马上|立刻|p0|P0|!!!)"), Priority.URGENT, true),
            Triple(Regex("(重要|高优|高优先级|p1|P1|!!)"), Priority.HIGH, true),
            Triple(Regex("(p2|P2)"), Priority.MEDIUM, true),
            Triple(Regex("(不急|低优|低优先级|p3|P3)"), Priority.LOW, true),
            Triple(Regex("(空闲|有空再说|p4|P4)"), Priority.IDLE, true)
        )
        for ((regex, p, strip) in priorityRules) {
            val m = regex.find(text) ?: continue
            priority = p
            if (strip) text = text.replace(m.value, " ")
            break
        }

        // ---- 日期 ----
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        var dateFound = false

        val nDaysLater = Regex("(\\d{1,3})\\s*天后").find(text)
        val absDate = Regex("(\\d{1,2})\\s*[月/\\-]\\s*(\\d{1,2})\\s*[日号]?").find(text)
        val nextWeekday = Regex("下\\s*[周星]期?\\s*([一二三四五六日天1-7])").find(text)
        val thisWeekday = Regex("(本周|这周|[周星]期?)\\s*([一二三四五六日天1-7])").find(text)

        when {
            text.contains("今天") || text.contains("今日") -> {
                dateFound = true
                text = text.replace(Regex("今[天日]"), " ")
            }
            text.contains("大后天") -> {
                cal.add(Calendar.DAY_OF_MONTH, 3); dateFound = true
                text = text.replace("大后天", " ")
            }
            text.contains("后天") -> {
                cal.add(Calendar.DAY_OF_MONTH, 2); dateFound = true
                text = text.replace("后天", " ")
            }
            text.contains("明天") || text.contains("明日") -> {
                cal.add(Calendar.DAY_OF_MONTH, 1); dateFound = true
                text = text.replace(Regex("明[天日]"), " ")
            }
            nDaysLater != null -> {
                cal.add(Calendar.DAY_OF_MONTH, nDaysLater.groupValues[1].toInt()); dateFound = true
                text = text.replace(nDaysLater.value, " ")
            }
            absDate != null -> {
                val month = absDate.groupValues[1].toInt()
                val day = absDate.groupValues[2].toInt()
                if (month in 1..12 && day in 1..31) {
                    val candidate = Calendar.getInstance().apply {
                        timeInMillis = now
                        set(Calendar.MONTH, month - 1)
                        set(Calendar.DAY_OF_MONTH, day)
                    }
                    if (candidate.timeInMillis < now - 12 * 3600_000L) {
                        candidate.add(Calendar.YEAR, 1)
                    }
                    cal.timeInMillis = candidate.timeInMillis
                    dateFound = true
                    text = text.replace(absDate.value, " ")
                }
            }
            nextWeekday != null -> {
                val target = WEEKDAY_MAP[nextWeekday.groupValues[1]]
                if (target != null) {
                    advanceToWeekday(cal, target, forceNextWeek = true)
                    dateFound = true
                    text = text.replace(nextWeekday.value, " ")
                }
            }
            thisWeekday != null -> {
                val target = WEEKDAY_MAP[thisWeekday.groupValues[2]]
                if (target != null) {
                    advanceToWeekday(cal, target, forceNextWeek = false)
                    dateFound = true
                    text = text.replace(thisWeekday.value, " ")
                }
            }
        }

        // ---- 时间 ----
        var timeFound = false
        var hour = -1
        var minute = 0
        var meridiem = 0 // 0=未知, 1=上午, 2=下午

        if (Regex("(下午|傍晚|晚上|夜里)").containsMatchIn(text)) {
            meridiem = 2
            text = text.replace(Regex("(下午|傍晚|晚上|夜里)"), " ")
        } else if (Regex("(上午|早上|早晨|凌晨)").containsMatchIn(text)) {
            meridiem = 1
            text = text.replace(Regex("(上午|早上|早晨|凌晨)"), " ")
        } else if (text.contains("中午")) {
            meridiem = 2; hour = 12; timeFound = true
            text = text.replace("中午", " ")
        }

        val colonTime = Regex("(\\d{1,2})\\s*[:：]\\s*(\\d{1,2})").find(text)
        val cnTime = Regex("(\\d{1,2})\\s*[点時时]\\s*(半|\\d{1,2}\\s*分?)?").find(text)
        when {
            colonTime != null -> {
                hour = colonTime.groupValues[1].toIntOrNull() ?: -1
                minute = colonTime.groupValues[2].toIntOrNull() ?: 0
                timeFound = hour in 0..23 && minute in 0..59
                if (timeFound) text = text.replace(colonTime.value, " ")
            }
            cnTime != null -> {
                hour = cnTime.groupValues[1].toIntOrNull() ?: -1
                val minPart = cnTime.groupValues[2].trim()
                minute = when {
                    minPart == "半" -> 30
                    minPart.isNotEmpty() -> minPart.removeSuffix("分").trim().toIntOrNull() ?: 0
                    else -> 0
                }
                timeFound = hour in 0..23
                if (timeFound) text = text.replace(cnTime.value, " ")
            }
        }

        if (timeFound && hour >= 0) {
            if (meridiem == 2 && hour < 12) hour += 12
            if (meridiem == 1 && hour == 12) hour = 0
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            // 只说时间没说日期，且时间已过 → 顺延到明天
            if (!dateFound && cal.timeInMillis < now) {
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
            dateFound = true
        } else if (dateFound) {
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }

        // ---- 清理标题 ----
        val title = text
            .replace(Regex("[，,]{2,}"), "，")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
            .trim('，', ',', '的', '·')
            .trim()

        return ParsedTask(
            title = title.ifBlank { raw.trim() },
            dueDate = if (dateFound) cal.timeInMillis else null,
            tags = tags.distinct(),
            categoryName = categoryName,
            priority = priority,
            recurrenceRule = recurrence
        )
    }

    private fun advanceToWeekday(cal: Calendar, targetDow: Int, forceNextWeek: Boolean) {
        var delta = targetDow - cal.get(Calendar.DAY_OF_WEEK)
        if (delta < 0) delta += 7
        if (delta == 0 && forceNextWeek) delta = 7
        if (forceNextWeek && delta < 7) delta += 7
        cal.add(Calendar.DAY_OF_MONTH, delta)
    }

    /** 生成解析摘要，供输入框下方预览 */
    fun summary(parsed: ParsedTask): String {
        val parts = mutableListOf<String>()
        parsed.dueDate?.let { parts.add("⏰ " + DateTimeUtils.formatDateTime(it)) }
        parsed.priority?.let {
            parts.add(
                "⚑ " + when (it) {
                    Priority.URGENT -> "紧急"
                    Priority.HIGH -> "高"
                    Priority.MEDIUM -> "中"
                    Priority.LOW -> "低"
                    Priority.IDLE -> "空闲"
                }
            )
        }
        parsed.categoryName?.let { parts.add("📁 $it") }
        if (parsed.tags.isNotEmpty()) parts.add("🏷 " + parsed.tags.joinToString(" "))
        parsed.recurrenceRule?.let { parts.add("🔁 " + Recurrence.label(it)) }
        return parts.joinToString("   ")
    }
}
