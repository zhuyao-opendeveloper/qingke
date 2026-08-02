package com.lightmark.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.auth.AuthManager
import com.lightmark.auth.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 登录页 ViewModel
 *
 * 处理 GitHub Token 登录逻辑：
 * - 验证 Token 有效性
 * - 保存用户信息
 * - 管理加载/错误状态
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess.asStateFlow()

    /**
     * 使用 Token 登录
     */
    fun login(token: String) {
        if (token.isBlank()) {
            _errorMessage.value = "请输入 GitHub Token"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            authManager.loginWithToken(token)
                .onSuccess {
                    _isLoading.value = false
                    _loginSuccess.value = true
                }
                .onFailure { error ->
                    _isLoading.value = false
                    _errorMessage.value = error.message ?: "登录失败，请检查 Token 是否正确"
                }
        }
    }
}
