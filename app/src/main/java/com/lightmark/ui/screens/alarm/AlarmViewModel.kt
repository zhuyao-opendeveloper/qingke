package com.lightmark.ui.screens.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.alarm.AlarmScheduler
import com.lightmark.data.local.dao.AlarmDao
import com.lightmark.data.local.entity.AlarmEntity
import com.lightmark.domain.model.Alarm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val alarmDao: AlarmDao,
    private val scheduler: AlarmScheduler
) : ViewModel() {

    val alarms: StateFlow<List<Alarm>> = alarmDao.observeAll()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(alarm: Alarm) {
        viewModelScope.launch {
            alarmDao.upsert(AlarmEntity.fromDomain(alarm))
            scheduler.schedule(alarm)
        }
    }

    fun create(hour: Int, minute: Int, label: String, repeatDays: Set<Int>, soundId: String, vibrate: Boolean) {
        save(
            Alarm(
                id = UUID.randomUUID().toString(),
                label = label.trim(),
                hour = hour,
                minute = minute,
                enabled = true,
                repeatDays = repeatDays,
                soundId = soundId,
                vibrate = vibrate
            )
        )
    }

    fun toggle(alarm: Alarm) {
        val next = alarm.copy(enabled = !alarm.enabled)
        viewModelScope.launch {
            alarmDao.upsert(AlarmEntity.fromDomain(next))
            if (next.enabled) scheduler.schedule(next) else scheduler.cancel(next.id)
        }
    }

    fun remove(alarm: Alarm) {
        viewModelScope.launch {
            scheduler.cancel(alarm.id)
            alarmDao.delete(AlarmEntity.fromDomain(alarm))
        }
    }

    /** 距离下次响铃还有多久的描述 */
    fun countdownText(alarm: Alarm): String {
        if (!alarm.enabled) return "已关闭"
        val delta = scheduler.nextTriggerAt(alarm) - System.currentTimeMillis()
        if (delta <= 0) return "即将响铃"
        val totalMin = delta / 60000
        val d = totalMin / (60 * 24)
        val h = (totalMin % (60 * 24)) / 60
        val m = totalMin % 60
        return buildString {
            append("还有 ")
            if (d > 0) append("${d}天")
            if (h > 0) append("${h}小时")
            append("${m}分钟")
        }
    }
}
