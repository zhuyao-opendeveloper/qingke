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
}
