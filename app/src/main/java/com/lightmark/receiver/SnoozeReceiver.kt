package com.lightmark.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lightmark.data.reminder.ReminderScheduler

/**
 * 提醒「推迟」接收器（#46）
 *
 * 由通知上的「推迟 10 分钟 / 1 小时 / 明天」按钮触发，重新向 AlarmManager
 * 注册一条到点提醒（复用 [ReminderScheduler] 的意图），下次到点再由
 * [ReminderReceiver] 弹出通知。
 */
class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderReceiver.ACTION_SNOOZE) return
        val todoId = intent.getStringExtra(ReminderScheduler.EXTRA_TODO_ID) ?: return
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "轻刻提醒"
        val minutes = intent.getIntExtra(ReminderReceiver.EXTRA_SNOOZE_MINUTES, 10)
            .coerceAtLeast(1)

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val fireAt = System.currentTimeMillis() + minutes * 60_000L

        val reminderIntent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderScheduler.EXTRA_TODO_ID, todoId)
            putExtra(ReminderScheduler.EXTRA_TITLE, title)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            todoId.hashCode(),
            reminderIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
    }
}
