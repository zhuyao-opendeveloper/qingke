package com.lightmark.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 轻刻全局设置仓库
 *
 * 基于 DataStore(Preferences) 持久化用户偏好：
 * - 隐私协议是否已同意
 * - 主题（模式 / 种子色 / 图标库 / 动态取色）
 * - OpenClaw AI 接入配置
 * - 提醒开关 / 默认排序
 *
 * 所有读取以 Flow 形式暴露，写入提供挂起函数。
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    /** 聚合后的设置流 */
    val settings: Flow<LightMarkSettings> = dataStore.data.map { prefs ->
        LightMarkSettings(
            privacyAccepted = prefs[KEY_PRIVACY_ACCEPTED] ?: false,
            themeMode = prefs[KEY_THEME_MODE] ?: "SYSTEM",
            seedColor = prefs[KEY_SEED_COLOR] ?: 0xFF6750A4,
            iconPack = prefs[KEY_ICON_PACK] ?: "MATERIAL",
            useDynamicColor = prefs[KEY_USE_DYNAMIC_COLOR] ?: true,
            openClawEnabled = prefs[KEY_OPENCLAW_ENABLED] ?: false,
            openClawBaseUrl = prefs[KEY_OPENCLAW_BASE_URL] ?: "https://api.openclaw.ai/v1/",
            openClawApiKey = prefs[KEY_OPENCLAW_API_KEY] ?: "",
            openClawModel = prefs[KEY_OPENCLAW_MODEL] ?: "gpt-4o-mini",
            reminderEnabled = prefs[KEY_REMINDER_ENABLED] ?: true,
            sortOrder = prefs[KEY_SORT_ORDER] ?: "CREATED_DESC"
        )
    }

    /** 同步读取当前设置（用于非 Compose 上下文，如 Receiver） */
    suspend fun currentSettings(): LightMarkSettings = settings.first()

    // ===== 隐私 =====
    suspend fun setPrivacyAccepted(accepted: Boolean) {
        dataStore.edit { it[KEY_PRIVACY_ACCEPTED] = accepted }
    }

    // ===== 主题 =====
    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun setSeedColor(color: Long) {
        dataStore.edit { it[KEY_SEED_COLOR] = color }
    }

    suspend fun setIconPack(pack: String) {
        dataStore.edit { it[KEY_ICON_PACK] = pack }
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        dataStore.edit { it[KEY_USE_DYNAMIC_COLOR] = enabled }
    }

    // ===== OpenClaw =====
    suspend fun setOpenClawEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_OPENCLAW_ENABLED] = enabled }
    }

    suspend fun setOpenClawConfig(baseUrl: String, apiKey: String, model: String) {
        dataStore.edit {
            it[KEY_OPENCLAW_BASE_URL] = baseUrl
            it[KEY_OPENCLAW_API_KEY] = apiKey
            it[KEY_OPENCLAW_MODEL] = model
        }
    }

    // ===== 提醒 / 排序 =====
    suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_REMINDER_ENABLED] = enabled }
    }

    suspend fun setSortOrder(order: String) {
        dataStore.edit { it[KEY_SORT_ORDER] = order }
    }

    companion object {
        private val KEY_PRIVACY_ACCEPTED = booleanPreferencesKey("privacy_accepted")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_SEED_COLOR = longPreferencesKey("seed_color")
        private val KEY_ICON_PACK = stringPreferencesKey("icon_pack")
        private val KEY_USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        private val KEY_OPENCLAW_ENABLED = booleanPreferencesKey("openclaw_enabled")
        private val KEY_OPENCLAW_BASE_URL = stringPreferencesKey("openclaw_base_url")
        private val KEY_OPENCLAW_API_KEY = stringPreferencesKey("openclaw_api_key")
        private val KEY_OPENCLAW_MODEL = stringPreferencesKey("openclaw_model")
        private val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        private val KEY_SORT_ORDER = stringPreferencesKey("sort_order")
    }
}

/**
 * 轻刻运行时设置聚合对象
 */
data class LightMarkSettings(
    val privacyAccepted: Boolean = false,
    val themeMode: String = "SYSTEM",
    val seedColor: Long = 0xFF6750A4,
    val iconPack: String = "MATERIAL",
    val useDynamicColor: Boolean = true,
    val openClawEnabled: Boolean = false,
    val openClawBaseUrl: String = "https://api.openclaw.ai/v1/",
    val openClawApiKey: String = "",
    val openClawModel: String = "gpt-4o-mini",
    val reminderEnabled: Boolean = true,
    val sortOrder: String = "CREATED_DESC"
)
