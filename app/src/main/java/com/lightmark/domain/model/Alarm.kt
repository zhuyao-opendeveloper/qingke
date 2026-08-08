package com.lightmark.domain.model

/**
 * 闹钟数据模型
 *
 * @param id            唯一 ID
 * @param label         标签，如「起床」「吃药」
 * @param hour          小时 0-23
 * @param minute        分钟 0-59
 * @param enabled       是否启用
 * @param repeatDays    重复的星期集合，1=周一 … 7=周日；为空表示只响一次
 * @param soundId       音效标识，见 AlarmSounds
 * @param vibrate       是否震动
 */
data class Alarm(
    val id: String,
    val label: String = "",
    val hour: Int = 7,
    val minute: Int = 0,
    val enabled: Boolean = true,
    val repeatDays: Set<Int> = emptySet(),
    val soundId: String = AlarmSounds.DEFAULT_ID,
    val vibrate: Boolean = true
) {
    /** 展示用时间字符串，如 07:05 */
    val timeText: String
        get() = "%02d:%02d".format(hour, minute)

    /** 展示用重复描述 */
    val repeatText: String
        get() = when {
            repeatDays.isEmpty() -> "仅一次"
            repeatDays.size == 7 -> "每天"
            repeatDays == setOf(1, 2, 3, 4, 5) -> "工作日"
            repeatDays == setOf(6, 7) -> "周末"
            else -> repeatDays.sorted().joinToString(" ") { WEEK_LABELS[it - 1] }
        }

    companion object {
        val WEEK_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")
    }
}

/** 内置闹钟音效表（Mixkit 免费音效，Mixkit License） */
object AlarmSounds {
    const val DEFAULT_ID = "alarm_classic"

    /** id -> 显示名称 */
    val ALL: List<Pair<String, String>> = listOf(
        "alarm_classic" to "经典闹铃",
        "alarm_morning" to "清晨时钟",
        "alarm_beep" to "电子哔哔",
        "alarm_tone" to "轻柔提示",
        "alarm_digital" to "数字时钟",
        "alarm_rooster" to "雄鸡报晓"
    )

    fun nameOf(id: String): String = ALL.firstOrNull { it.first == id }?.second ?: "经典闹铃"
}
