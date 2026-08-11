package com.lightmark.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.lightmark.data.reminder.ReminderScheduler

/**
 * 待办到期提醒广播接收器
 *
 * 由 [ReminderScheduler] 通过 AlarmManager 在到达（提前提醒后的）截止时间时触发，
 * 弹出一条本地通知，并附带「推迟」快捷操作（#46）。
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "轻刻提醒"
        val todoId = intent.getStringExtra(ReminderScheduler.EXTRA_TODO_ID) ?: ""

        // 推迟快捷操作：10 分钟 / 1 小时 / 明天（#46）
        val snoozeActions = listOf(10 to "10 分钟", 60 to "1 小时", 1440 to "明天")
            .map { (minutes, label) ->
                val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
                    action = ACTION_SNOOZE
                    putExtra(ReminderScheduler.EXTRA_TODO_ID, todoId)
                    putExtra(ReminderScheduler.EXTRA_TITLE, title)
                    putExtra(EXTRA_SNOOZE_MINUTES, minutes)
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    todoId.hashCode() + minutes,
                    snoozeIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                NotificationCompat.Action.Builder(0, label, pi).build()
            }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⏰ 待办提醒")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
        snoozeActions.forEach { builder.addAction(it) }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(todoId.hashCode(), builder.build())
    }

    companion object {
        const val CHANNEL_ID = "lightmark_reminders"
        const val ACTION_SNOOZE = "com.lightmark.action.SNOOZE"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
    }
}
