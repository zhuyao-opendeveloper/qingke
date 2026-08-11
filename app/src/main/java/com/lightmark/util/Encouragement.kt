package com.lightmark.util

import com.lightmark.domain.model.Priority

/**
 * 完成任务时的随机鼓励语（#123）
 *
 * 高优先级任务给更强的正反馈，普通任务给轻量反馈，避免出现"每次都夸"的廉价感。
 */
object Encouragement {

    private val light = listOf(
        "又划掉一条，清爽。",
        "搞定。",
        "推进了一小步。",
        "这条不用再惦记了。",
        "干净利落。",
        "列表又短了一行。",
        "完成。继续保持。"
    )

    private val strong = listOf(
        "硬骨头啃下来了。",
        "最难的那条清了，剩下的都好办。",
        "重要的事做完了，今天不亏。",
        "这条值得记一笔。",
        "干得漂亮，这是今天的分水岭。",
        "把最重的石头搬开了。"
    )

    private val milestone = listOf(
        "今天第 %d 条了，状态在线。",
        "连着完成 %d 条，节奏很稳。",
        "已完成 %d 条，可以歇口气。"
    )

    /**
     * @param priority 任务优先级
     * @param completedToday 今天已完成的总条数（含本条）
     * @return 鼓励语；返回 null 表示这次不打扰
     */
    fun pick(priority: Priority, completedToday: Int): String? {
        // 每完成 5 条给一次里程碑反馈
        if (completedToday > 0 && completedToday % 5 == 0) {
            return milestone.random().format(completedToday)
        }
        return when (priority) {
            Priority.URGENT, Priority.HIGH -> strong.random()
            Priority.MEDIUM -> light.random()
            // 低优先级不打扰，避免频繁弹窗
            else -> null
        }
    }
}
