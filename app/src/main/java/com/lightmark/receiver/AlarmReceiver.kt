package com.lightmark.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.lightmark.data.alarm.AlarmScheduler
import com.lightmark.service.AlarmSoundService

/**
 * 闹钟到点广播接收器
 *
 * 收到 AlarmManager 的广播后拉起 [AlarmSoundService] 播放铃声并显示可停止的通知。
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_LABEL).orEmpty()
        val soundId = intent.getStringExtra(AlarmScheduler.EXTRA_SOUND_ID).orEmpty()
        val vibrate = intent.getBooleanExtra(AlarmScheduler.EXTRA_VIBRATE, true)

        val svc = Intent(context, AlarmSoundService::class.java).apply {
            action = AlarmSoundService.ACTION_START
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_LABEL, label)
            putExtra(AlarmScheduler.EXTRA_SOUND_ID, soundId)
            putExtra(AlarmScheduler.EXTRA_VIBRATE, vibrate)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svc)
        } else {
            context.startService(svc)
        }
    }
}
