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
    private val gitHubApiService: GitHubApiService,
    private val tokenHolder: TokenHolder
) {
    companion object {
        private const val PREFS_NAME = "lightmark_encrypted_prefs"
        private const val KEY_GITHUB_TOKEN = "github_token"
        private const val KEY_USER_LOGIN = "user_login"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_AVATAR = "user_avatar"
        private const val KEY_LOCAL_MODE = "local_mode"
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
        // 同步到 TokenHolder，确保拦截器能立即带上 Authorization 头
        tokenHolder.token = savedToken
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
            } else if (encryptedPrefs.getBoolean(KEY_LOCAL_MODE, false)) {
                // 之前选择过「本地使用」，直接以本地模式进入
                _authState.value = AuthState.Local
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
            // 先写入 TokenHolder，否则拦截器在校验请求里仍然不带 Authorization 头，
            // GitHub 的 /user 接口会返回 401（这就是之前一直登录失败的根因）
            tokenHolder.token = token
            val userDto = gitHubApiService.getCurrentUser()
            val user = userDto.toDomain()

            // 保存 Token 和用户信息
            saveToken(token)
            saveUserInfo(user)

            _currentUser.value = user
            _authState.value = AuthState.Authenticated
            user
        }.onFailure {
            // 校验失败：清掉刚才临时写入的 token，避免带着无效 token 发后续请求
            tokenHolder.token = null
            _authState.value = AuthState.Error(it.message ?: "登录失败")
        }
    }

    /**
     * 进入本地使用模式（不登录）
     * 持久化标记，下次启动直接进入本地模式，无需再次弹出警告
     */
    fun enterLocalMode() {
        encryptedPrefs.edit()
            .putBoolean(KEY_LOCAL_MODE, true)
            .apply()
        _authState.value = AuthState.Local
    }

    /**
     * 退出登录
     */
    fun logout() {
        // 清除内存中的 token，拦截器随即停止携带 Authorization 头
        tokenHolder.token = null
        encryptedPrefs.edit()
            .remove(KEY_GITHUB_TOKEN)
            .remove(KEY_USER_LOGIN)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_AVATAR)
            .putBoolean(KEY_LOCAL_MODE, false)
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
        // 同步到 TokenHolder，保证后续所有 GitHub 请求都带鉴权头
        tokenHolder.token = token
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
    object Local : AuthState()             // 本地模式（未登录，数据仅存本机）
    object Authenticated : AuthState()     // 已登录
    data class Error(val message: String) : AuthState() // 错误
}
