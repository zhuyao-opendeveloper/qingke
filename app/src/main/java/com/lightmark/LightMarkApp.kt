package com.lightmark

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.lightmark.receiver.ReminderReceiver
import dagger.hilt.android.HiltAndroidApp

/**
 * 轻刻 (LightMark) - 待办清单应用
 *
 * 简洁、美观、现代化的待办管理工具
 * 支持 GitHub 账户登录，数据私有化存储
 * 多图标库切换，多主题支持
 */
@HiltAndroidApp
class LightMarkApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createReminderChannel()
    }

    private fun createReminderChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReminderReceiver.CHANNEL_ID,
                "待办提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "待办到达截止时间时的提醒通知"
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
