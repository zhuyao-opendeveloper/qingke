package com.lightmark.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.lightmark.MainActivity
import com.lightmark.R
import com.lightmark.data.alarm.AlarmScheduler
import com.lightmark.data.local.dao.AlarmDao
import com.lightmark.domain.model.AlarmSounds
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 闹钟响铃前台服务
 *
 * - 循环播放 res/raw 下的内置音效（找不到时回退到系统默认闹钟铃声）
 * - 前台通知带「停止」动作
 * - 最长响铃 2 分钟后自动停止
 * - 若是重复闹钟，响铃后自动排下一次
 */
@AndroidEntryPoint
class AlarmSoundService : Service() {

    @Inject lateinit var alarmDao: AlarmDao
    @Inject lateinit var alarmScheduler: AlarmScheduler

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { stopEverything() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopEverything()
                return START_NOT_STICKY
            }
        }

        val alarmId = intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID).orEmpty()
        val label = intent?.getStringExtra(AlarmScheduler.EXTRA_LABEL).orEmpty()
        val soundId = intent?.getStringExtra(AlarmScheduler.EXTRA_SOUND_ID).orEmpty()
        val vibrate = intent?.getBooleanExtra(AlarmScheduler.EXTRA_VIBRATE, true) ?: true

        startForeground(NOTIF_ID, buildNotification(label))
        startSound(soundId)
        if (vibrate) startVibrate()

        handler.removeCallbacks(autoStop)
        handler.postDelayed(autoStop, MAX_RING_MS)

        // 重复闹钟：排下一次；一次性闹钟：自动关闭开关
        if (alarmId.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val entity = alarmDao.getById(alarmId) ?: return@launch
                val alarm = entity.toDomain()
                if (alarm.repeatDays.isEmpty()) {
                    alarmDao.upsert(entity.copy(enabled = false))
                } else {
                    alarmScheduler.schedule(alarm)
                }
            }
        }
        return START_STICKY
    }

    private fun startSound(soundId: String) {
        stopSound()
        val resId = resolveRawRes(soundId)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        player = try {
            if (resId != 0) {
                MediaPlayer.create(this, resId)?.apply {
                    setAudioAttributes(attrs)
                    isLooping = true
                    start()
                }
            } else {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                MediaPlayer().apply {
                    setAudioAttributes(attrs)
                    setDataSource(this@AlarmSoundService, uri)
                    isLooping = true
                    prepare()
                    start()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveRawRes(soundId: String): Int {
        val id = soundId.ifBlank { AlarmSounds.DEFAULT_ID }
        return resources.getIdentifier(id, "raw", packageName)
    }

    private fun startVibrate() {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
        vibrator = v
        val pattern = longArrayOf(0, 600, 800)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            v?.vibrate(pattern, 0)
        }
    }

    private fun buildNotification(label: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, AlarmSoundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏰ 闹钟")
            .setContentText(label.ifBlank { "时间到了" })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(contentIntent, true)
            .addAction(0, "停止", stopIntent)
            .build()
    }

    private fun stopSound() {
        player?.runCatching {
            if (isPlaying) stop()
            release()
        }
        player = null
    }

    private fun stopEverything() {
        handler.removeCallbacks(autoStop)
        stopSound()
        vibrator?.cancel()
        vibrator = null
        getSystemService(NotificationManager::class.java)?.cancel(NOTIF_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopSound()
        vibrator?.cancel()
        handler.removeCallbacks(autoStop)
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.lightmark.action.ALARM_SOUND_START"
        const val ACTION_STOP = "com.lightmark.action.ALARM_SOUND_STOP"
        const val CHANNEL_ID = "lightmark_alarms"
        const val NOTIF_ID = 20250801
        private const val MAX_RING_MS = 2 * 60 * 1000L
    }
}
