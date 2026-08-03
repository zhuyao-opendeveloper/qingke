package com.lightmark.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.lightmark.data.reminder.ReminderScheduler

/**
 * 待办到期提醒广播接收器
 *
 * 由 [ReminderScheduler] 通过 AlarmManager 在到达截止时间时触发，
 * 弹出一条本地通知。
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "轻刻提醒"
        val todoId = intent.getStringExtra(ReminderScheduler.EXTRA_TODO_ID) ?: ""

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⏰ 待办提醒")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(todoId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "lightmark_reminders"
    }
}
