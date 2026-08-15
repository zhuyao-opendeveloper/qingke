package com.lightmark.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
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
 * - 本地昵称
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
            themeId = prefs[KEY_THEME_ID] ?: "DEFAULT",
            customPrimary = prefs[KEY_CUSTOM_PRIMARY],
            backgroundImageUri = prefs[KEY_BG_IMAGE_URI] ?: "",
            nickname = prefs[KEY_NICKNAME] ?: "",
            reminderEnabled = prefs[KEY_REMINDER_ENABLED] ?: true,
            sortOrder = prefs[KEY_SORT_ORDER] ?: "CREATED_DESC",
            trashRetentionDays = prefs[KEY_TRASH_RETENTION_DAYS] ?: 30,
            encouragementEnabled = prefs[KEY_ENCOURAGEMENT_ENABLED] ?: true,
            hapticEnabled = prefs[KEY_HAPTIC_ENABLED] ?: true,
            reminderLeadMinutes = prefs[KEY_REMINDER_LEAD_MINUTES] ?: 10,
            listDensity = prefs[KEY_LIST_DENSITY] ?: "COZY",
            fontScale = prefs[KEY_FONT_SCALE] ?: 1.0f,
            biometricLockEnabled = prefs[KEY_BIOMETRIC_LOCK] ?: false,
            announcementDismissed = prefs[KEY_ANNOUNCEMENT_DISMISSED] ?: false
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

    // ===== 主题 / 背景 =====
    suspend fun setThemeId(id: String) {
        dataStore.edit { it[KEY_THEME_ID] = id }
    }

    suspend fun setCustomPrimary(color: Long?) {
        dataStore.edit { if (color == null) it.remove(KEY_CUSTOM_PRIMARY) else it[KEY_CUSTOM_PRIMARY] = color }
    }

    suspend fun setBackgroundImageUri(uri: String) {
        dataStore.edit { it[KEY_BG_IMAGE_URI] = uri }
    }

    // ===== 个性化称呼（离线版本地昵称，用于首页问候语） =====
    suspend fun setNickname(name: String) {
        dataStore.edit { it[KEY_NICKNAME] = name }
    }

    // ===== 提醒 / 排序 =====
    suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_REMINDER_ENABLED] = enabled }
    }

    suspend fun setSortOrder(order: String) {
        dataStore.edit { it[KEY_SORT_ORDER] = order }
    }

    // ===== 回收站 / 反馈 =====

    /** 回收站保留天数，0 表示永久保留（#101） */
    suspend fun setTrashRetentionDays(days: Int) {
        dataStore.edit { it[KEY_TRASH_RETENTION_DAYS] = days }
    }

    /** 完成任务时是否弹出鼓励语（#123） */
    suspend fun setEncouragementEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ENCOURAGEMENT_ENABLED] = enabled }
    }

    /** 打勾是否震动（#116） */
    suspend fun setHapticEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_HAPTIC_ENABLED] = enabled }
    }

    // ===== 提醒提前量 / 列表密度 / 字号（#14 / #51 / #61） =====
    /** 提前提醒分钟数，0 表示准时提醒（#14） */
    suspend fun setReminderLeadMinutes(minutes: Int) {
        dataStore.edit { it[KEY_REMINDER_LEAD_MINUTES] = minutes }
    }

    /** 列表密度：COMPACT / COZY / DETAILED（#51） */
    suspend fun setListDensity(density: String) {
        dataStore.edit { it[KEY_LIST_DENSITY] = density }
    }

    /** 全局字号缩放系数（#61） */
    suspend fun setFontScale(scale: Float) {
        dataStore.edit { it[KEY_FONT_SCALE] = scale }
    }

    /** 生物识别应用锁开关（#96） */
    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_BIOMETRIC_LOCK] = enabled }
    }

    /** 停更公告横幅是否已关闭（#v3.0.0） */
    suspend fun setAnnouncementDismissed(dismissed: Boolean) {
        dataStore.edit { it[KEY_ANNOUNCEMENT_DISMISSED] = dismissed }
    }

    companion object {
        private val KEY_PRIVACY_ACCEPTED = booleanPreferencesKey("privacy_accepted")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_SEED_COLOR = longPreferencesKey("seed_color")
        private val KEY_ICON_PACK = stringPreferencesKey("icon_pack")
        private val KEY_USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        private val KEY_THEME_ID = stringPreferencesKey("theme_id")
        private val KEY_CUSTOM_PRIMARY = longPreferencesKey("custom_primary")
        private val KEY_BG_IMAGE_URI = stringPreferencesKey("bg_image_uri")
        private val KEY_NICKNAME = stringPreferencesKey("nickname")
        private val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        private val KEY_SORT_ORDER = stringPreferencesKey("sort_order")
        private val KEY_TRASH_RETENTION_DAYS = intPreferencesKey("trash_retention_days")
        private val KEY_ENCOURAGEMENT_ENABLED = booleanPreferencesKey("encouragement_enabled")
        private val KEY_HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        private val KEY_REMINDER_LEAD_MINUTES = intPreferencesKey("reminder_lead_minutes")
        private val KEY_LIST_DENSITY = stringPreferencesKey("list_density")
        private val KEY_FONT_SCALE = floatPreferencesKey("font_scale")
        private val KEY_BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock_enabled")
        private val KEY_ANNOUNCEMENT_DISMISSED = booleanPreferencesKey("announcement_dismissed_v3")
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
    val themeId: String = "DEFAULT",
    val customPrimary: Long? = null,
    val backgroundImageUri: String = "",
    val nickname: String = "",
    val reminderEnabled: Boolean = true,
    val sortOrder: String = "CREATED_DESC",
    val trashRetentionDays: Int = 30,
    val encouragementEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val reminderLeadMinutes: Int = 10,
    val listDensity: String = "COZY",
    val fontScale: Float = 1.0f,
    val biometricLockEnabled: Boolean = false,
    val announcementDismissed: Boolean = false
)
