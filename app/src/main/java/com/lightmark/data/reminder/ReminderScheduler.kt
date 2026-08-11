package com.lightmark.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.lightmark.receiver.ReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import com.lightmark.data.settings.SettingsRepository

/**
 * 提醒调度器
 *
 * 基于 AlarmManager 在待办截止时间（减去提前提醒量）向系统闹钟注册一条本地通知。
 * 不依赖网络，到点由 [ReminderReceiver] 弹出通知。
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    suspend fun schedule(todoId: String, title: String, dueDate: Long, enabled: Boolean) {
        if (!enabled || dueDate <= System.currentTimeMillis()) return
        // #14 提前提醒量：在截止时间之前 N 分钟触发
        val lead = settingsRepository.currentSettings().reminderLeadMinutes.coerceAtLeast(0)
        val fireAt = (dueDate - lead * 60_000L).coerceAtLeast(System.currentTimeMillis() + 1_000L)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TODO_ID, todoId)
            putExtra(EXTRA_TITLE, title)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            todoId.hashCode(),
            intent,
            FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
        )
        alarmManager?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            fireAt,
            pi
        )
    }

    fun cancel(todoId: String) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            todoId.hashCode(),
            intent,
            FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
        )
        alarmManager?.cancel(pi)
    }

    companion object {
        const val EXTRA_TODO_ID = "todo_id"
        const val EXTRA_TITLE = "title"
    }
}
