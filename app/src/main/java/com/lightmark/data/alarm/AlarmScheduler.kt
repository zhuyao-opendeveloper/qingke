package com.lightmark.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.lightmark.domain.model.Alarm
import com.lightmark.receiver.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 闹钟调度器
 *
 * 使用 AlarmManager 精确闹钟在指定时间唤醒 [AlarmReceiver]。
 * 支持「仅一次」与「按周重复」两种模式：
 * - 仅一次：下一个到达该时刻的时间点
 * - 重复：下一个满足 repeatDays 的时刻
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    /** 计算下一次触发的时间戳（毫秒） */
    fun nextTriggerAt(alarm: Alarm, from: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = from
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (alarm.repeatDays.isEmpty()) {
            if (cal.timeInMillis <= from) cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }
        // 最多向后找 8 天，命中重复日即返回
        for (i in 0..7) {
            val probe = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
            if (probe.timeInMillis <= from) continue
            // Calendar.MONDAY=2 … SUNDAY=1，转成 1=周一 … 7=周日
            val dow = when (val d = probe.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> 7
                else -> d - 1
            }
            if (dow in alarm.repeatDays) return probe.timeInMillis
        }
        return cal.timeInMillis
    }

    fun schedule(alarm: Alarm) {
        cancel(alarm.id)
        if (!alarm.enabled) return
        val triggerAt = nextTriggerAt(alarm)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_LABEL, alarm.label)
            putExtra(EXTRA_SOUND_ID, alarm.soundId)
            putExtra(EXTRA_VIBRATE, alarm.vibrate)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val am = alarmManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                // 无精确闹钟权限时退化为非精确闹钟，仍能提醒（可能有几分钟误差）
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(alarmId: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_FIRE }
        val pi = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager?.cancel(pi)
    }

    companion object {
        const val ACTION_FIRE = "com.lightmark.action.ALARM_FIRE"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_LABEL = "alarm_label"
        const val EXTRA_SOUND_ID = "alarm_sound_id"
        const val EXTRA_VIBRATE = "alarm_vibrate"
    }
}
