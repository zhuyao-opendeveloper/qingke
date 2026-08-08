package com.lightmark.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lightmark.data.alarm.AlarmScheduler
import com.lightmark.data.local.dao.AlarmDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 开机 / 应用升级后重排所有已启用的闹钟
 *
 * AlarmManager 注册的闹钟在设备重启后会丢失，必须重新注册。
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var alarmDao: AlarmDao
    @Inject lateinit var scheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                alarmDao.observeAll().first().forEach { entity ->
                    val alarm = entity.toDomain()
                    if (alarm.enabled) scheduler.schedule(alarm)
                }
            } catch (e: Exception) {
                // 忽略：重排失败不应影响开机流程
            } finally {
                pending.finish()
            }
        }
    }
}
