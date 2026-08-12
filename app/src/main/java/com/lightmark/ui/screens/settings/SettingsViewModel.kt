package com.lightmark.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.repository.TodoRepository
import com.lightmark.data.settings.LightMarkSettings
import com.lightmark.data.settings.SettingsRepository
import com.lightmark.domain.model.IconPack
import com.lightmark.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设置页 ViewModel
 *
 * 聚合主题、图标库、动态取色、提醒开关、体验偏好与隐私政策重看。
 * 轻刻为完全离线应用，不含账号与云同步相关设置。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val repository: TodoRepository
) : ViewModel() {

    val settings: StateFlow<LightMarkSettings> = settingsRepository.settings
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), LightMarkSettings())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode.name) }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setUseDynamicColor(enabled) }
    }

    fun setThemeId(id: String) {
        viewModelScope.launch { settingsRepository.setThemeId(id) }
    }

    fun setCustomPrimary(color: Long?) {
        viewModelScope.launch { settingsRepository.setCustomPrimary(color) }
    }

    fun setBackgroundImageUri(uri: String) {
        viewModelScope.launch { settingsRepository.setBackgroundImageUri(uri) }
    }

    fun setIconPack(pack: IconPack) {
        viewModelScope.launch { settingsRepository.setIconPack(pack.name) }
    }

    /** 本地昵称，用于首页问候语 */
    fun setNickname(name: String) {
        viewModelScope.launch { settingsRepository.setNickname(name) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReminderEnabled(enabled) }
    }

    /** 回收站保留天数（#101），0 表示永久保留 */
    fun setTrashRetentionDays(days: Int) {
        viewModelScope.launch { settingsRepository.setTrashRetentionDays(days) }
    }

    /** 完成任务鼓励语（#123） */
    fun setEncouragementEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setEncouragementEnabled(enabled) }
    }

    /** 触感反馈（#116） */
    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHapticEnabled(enabled) }
    }

    /** 提前提醒分钟数（#14） */
    fun setReminderLeadMinutes(minutes: Int) {
        viewModelScope.launch { settingsRepository.setReminderLeadMinutes(minutes) }
    }

    /** 列表密度（#51） */
    fun setListDensity(density: String) {
        viewModelScope.launch { settingsRepository.setListDensity(density) }
    }

    /** 全局字号缩放（#61） */
    fun setFontScale(scale: Float) {
        viewModelScope.launch { settingsRepository.setFontScale(scale) }
    }

    /** 生物识别应用锁开关（#96） */
    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBiometricLockEnabled(enabled) }
    }

    /** 一键清空本机全部数据（#102） */
    fun clearAllData() {
        viewModelScope.launch { repository.clearAllData() }
    }

    fun reReviewPrivacy() {
        viewModelScope.launch { settingsRepository.setPrivacyAccepted(false) }
    }
}
