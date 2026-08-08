package com.lightmark.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.auth.AuthManager
import com.lightmark.data.repository.TodoRepository
import com.lightmark.data.settings.LightMarkSettings
import com.lightmark.data.settings.SettingsRepository
import com.lightmark.domain.model.IconPack
import com.lightmark.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设置页 ViewModel
 *
 * 聚合主题、图标库、动态取色、OpenClaw 配置、提醒开关、
 * 账户登录态、数据同步与隐私政策重看。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authManager: AuthManager,
    private val repository: TodoRepository
) : ViewModel() {

    val settings: StateFlow<LightMarkSettings> = settingsRepository.settings
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), LightMarkSettings())

    val isLoggedIn: Boolean get() = authManager.isLoggedIn()
    val currentUser = authManager.currentUser

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

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

    fun setOpenClawEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setOpenClawEnabled(enabled) }
    }

    fun setOpenClawConfig(baseUrl: String, apiKey: String, model: String) {
        viewModelScope.launch { settingsRepository.setOpenClawConfig(baseUrl, apiKey, model) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReminderEnabled(enabled) }
    }

    fun logout() {
        authManager.logout()
    }

    fun reReviewPrivacy() {
        viewModelScope.launch { settingsRepository.setPrivacyAccepted(false) }
    }

    fun syncNow() {
        if (_syncState.value == SyncState.Loading) return
        viewModelScope.launch {
            _syncState.value = SyncState.Loading
            val token = authManager.getToken()
            val login = authManager.currentUser.value?.login
            val result = if (!token.isNullOrBlank() && !login.isNullOrBlank()) {
                repository.syncToGitHub(token, login)
            } else {
                Result.failure(Exception("未登录"))
            }
            _syncState.value = result.fold(
                onSuccess = { SyncState.Success },
                onFailure = { SyncState.Error(it.message ?: "同步失败") }
            )
        }
    }
}

sealed interface SyncState {
    data object Idle : SyncState
    data object Loading : SyncState
    data object Success : SyncState
    data class Error(val message: String) : SyncState
}
