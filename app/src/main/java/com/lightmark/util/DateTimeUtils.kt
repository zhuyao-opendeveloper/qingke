package com.lightmark.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * 日期时间工具类
 */
object DateTimeUtils {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /**
     * 格式化日期
     */
    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

    /**
     * 格式化时间
     */
    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

    /**
     * 格式化日期时间
     */
    fun formatDateTime(timestamp: Long): String = dateTimeFormat.format(Date(timestamp))

    /**
     * 获取相对时间描述
     * 例如：刚刚、5分钟前、1小时前、昨天、3天前
     */
    fun getRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "刚刚"
            diff < 3_600_000 -> "${diff / 60_000}分钟前"
            diff < 86_400_000 -> "${diff / 3_600_000}小时前"
            diff < 172_800_000 -> "昨天"
            diff < 604_800_000 -> "${diff / 86_400_000}天前"
            else -> formatDate(timestamp)
        }
    }

    /**
     * 判断是否已过期
     */
    fun isOverdue(dueDate: Long): Boolean = dueDate < System.currentTimeMillis()

    /**
     * 截止日期倒计时（功能 #47）
     * 例如：已逾期 2 天、今天到期、明天到期、剩余 3 天
     */
    fun countdownLabel(dueDate: Long, now: Long = System.currentTimeMillis()): String {
        val startOfToday = startOfDay(now)
        val startOfDue = startOfDay(dueDate)
        val days = ((startOfDue - startOfToday) / 86_400_000L).toInt()
        return when {
            days < -1 -> "已逾期 ${-days} 天"
            days == -1 -> "昨天到期"
            days == 0 -> {
                val diff = dueDate - now
                when {
                    diff < 0 -> "已逾期"
                    diff < 3_600_000L -> "剩余 ${(diff / 60_000L).coerceAtLeast(1)} 分钟"
                    else -> "今天到期"
                }
            }
            days == 1 -> "明天到期"
            days == 2 -> "后天到期"
            days <= 30 -> "剩余 $days 天"
            else -> formatDate(dueDate)
        }
    }

    /** 当天 0 点时间戳 */
    fun startOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
