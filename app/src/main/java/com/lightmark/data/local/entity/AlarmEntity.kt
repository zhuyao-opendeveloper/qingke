package com.lightmark.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lightmark.domain.model.Alarm
import com.lightmark.domain.model.AlarmSounds

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey val id: String,
    val label: String = "",
    val hour: Int = 7,
    val minute: Int = 0,
    val enabled: Boolean = true,
    /** 重复日以逗号分隔存储，如 "1,2,3,4,5" */
    val repeatDays: String = "",
    val soundId: String = AlarmSounds.DEFAULT_ID,
    val vibrate: Boolean = true
) {
    fun toDomain(): Alarm = Alarm(
        id = id,
        label = label,
        hour = hour,
        minute = minute,
        enabled = enabled,
        repeatDays = repeatDays.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet(),
        soundId = soundId,
        vibrate = vibrate
    )

    companion object {
        fun fromDomain(alarm: Alarm): AlarmEntity = AlarmEntity(
            id = alarm.id,
            label = alarm.label,
            hour = alarm.hour,
            minute = alarm.minute,
            enabled = alarm.enabled,
            repeatDays = alarm.repeatDays.sorted().joinToString(","),
            soundId = alarm.soundId,
            vibrate = alarm.vibrate
        )
    }
}
