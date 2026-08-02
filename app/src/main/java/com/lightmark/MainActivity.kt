package com.lightmark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.lightmark.auth.AuthManager
import com.lightmark.auth.AuthState
import com.lightmark.ui.navigation.LightMarkNavHost
import com.lightmark.ui.screens.auth.LoginScreen
import com.lightmark.ui.theme.LightMarkTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 主 Activity——轻刻应用的入口
 *
 * 根据登录状态自动导航到登录页或主页面
 * 使用 Material 3 + 圆角悬浮设计，简洁现代化 UI
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val authState by authManager.authState.collectAsState()
            val currentUser by authManager.currentUser.collectAsState()

            LightMarkTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (authState) {
                        is AuthState.Authenticated -> {
                            LightMarkNavHost(
                                initialRoute = "home",
                                userId = currentUser?.login ?: ""
                            )
                        }
                        is AuthState.Loading -> {
                            // 加载中状态 - 显示启动画面
                            com.lightmark.ui.screens.splash.SplashScreen()
                        }
                        else -> {
                            LoginScreen(
                                onLoginSuccess = { /* ViewModel 中处理导航 */ }
                            )
                        }
                    }
                }
            }
        }
    }
}
