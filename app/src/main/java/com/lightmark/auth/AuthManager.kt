package com.lightmark.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.lightmark.domain.model.GitHubUser
import com.lightmark.data.remote.GitHubApiService
import com.lightmark.data.remote.dto.GitHubUserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GitHub 认证管理器
 *
 * 功能：
 * - 使用 GitHub Personal Access Token 登录
 * - 验证 Token 有效性
 * - 安全存储 Token（EncryptedSharedPreferences）
 * - 管理登录状态
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gitHubApiService: GitHubApiService
) {
    companion object {
        private const val PREFS_NAME = "lightmark_encrypted_prefs"
        private const val KEY_GITHUB_TOKEN = "github_token"
        private const val KEY_USER_LOGIN = "user_login"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_AVATAR = "user_avatar"
    }

    // 登录状态
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // 当前用户
    private val _currentUser = MutableStateFlow<GitHubUser?>(null)
    val currentUser: StateFlow<GitHubUser?> = _currentUser.asStateFlow()

    // 加密 SharedPreferences
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    init {
        // 初始化时检查是否有保存的 token
        val savedToken = getToken()
        if (!savedToken.isNullOrBlank()) {
            val login = encryptedPrefs.getString(KEY_USER_LOGIN, null)
            val name = encryptedPrefs.getString(KEY_USER_NAME, null)
            val avatar = encryptedPrefs.getString(KEY_USER_AVATAR, null)
            if (!login.isNullOrBlank()) {
                _currentUser.value = GitHubUser(
                    login = login,
                    id = 0, // 本地缓存的用户信息 id 可能不准
                    name = name,
                    avatarUrl = avatar
                )
                _authState.value = AuthState.Authenticated
            }
        }
    }

    /**
     * 使用 GitHub Token 登录
     * 验证 Token 有效性并保存
     */
    suspend fun loginWithToken(token: String): Result<GitHubUser> {
        _authState.value = AuthState.Loading
        return runCatching {
            val userDto = gitHubApiService.getCurrentUser()
            val user = userDto.toDomain()

            // 保存 Token 和用户信息
            saveToken(token)
            saveUserInfo(user)

            _currentUser.value = user
            _authState.value = AuthState.Authenticated
            user
        }.onFailure {
            _authState.value = AuthState.Error(it.message ?: "登录失败")
        }
    }

    /**
     * 退出登录
     */
    fun logout() {
        encryptedPrefs.edit()
            .remove(KEY_GITHUB_TOKEN)
            .remove(KEY_USER_LOGIN)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_AVATAR)
            .apply()
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
    }

    /**
     * 获取当前保存的 Token
     */
    fun getToken(): String? = encryptedPrefs.getString(KEY_GITHUB_TOKEN, null)

    /**
     * 是否已登录
     */
    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    // --- 私有方法 ---

    private fun saveToken(token: String) {
        encryptedPrefs.edit()
            .putString(KEY_GITHUB_TOKEN, token)
            .apply()
    }

    private fun saveUserInfo(user: GitHubUser) {
        encryptedPrefs.edit()
            .putString(KEY_USER_LOGIN, user.login)
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_USER_AVATAR, user.avatarUrl)
            .apply()
    }

    private fun GitHubUserDto.toDomain(): GitHubUser = GitHubUser(
        login = login,
        id = id,
        name = name,
        avatarUrl = avatarUrl,
        email = email,
        bio = bio
    )
}

/**
 * 认证状态
 */
sealed class AuthState {
    object Initial : AuthState()           // 初始状态（检查中）
    object Loading : AuthState()           // 登录中
    object Unauthenticated : AuthState()   // 未登录
    object Authenticated : AuthState()     // 已登录
    data class Error(val message: String) : AuthState() // 错误
}
