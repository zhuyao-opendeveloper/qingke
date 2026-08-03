package com.lightmark

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.lightmark.auth.AuthManager
import com.lightmark.auth.AuthState
import com.lightmark.data.settings.SettingsRepository
import com.lightmark.domain.model.AppSettings
import com.lightmark.domain.model.IconPack
import com.lightmark.domain.model.ThemeMode
import com.lightmark.ui.navigation.LightMarkNavHost
import com.lightmark.ui.screens.auth.LoginScreen
import com.lightmark.ui.screens.privacy.PrivacyDialog
import com.lightmark.ui.screens.splash.SplashScreen
import com.lightmark.ui.theme.LightMarkTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * 主 Activity——轻刻应用的入口
 *
 * 启动流程：
 * 1. 首次启动弹出隐私政策协议（未同意前不允许进入）；
 * 2. 依据登录状态在 Splash / 登录页 / 主页之间切换；
 * 3. 主题（浅色/深色/跟随系统 + 动态取色）由 DataStore 设置驱动。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authManager: AuthManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 通知权限：用户拒绝也不影响核心功能 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Android 13+ 申请通知权限（用于待办到期提醒）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val authState by authManager.authState.collectAsState()
            val currentUser by authManager.currentUser.collectAsState()
            val settings by settingsRepository.settings.collectAsState(
                initial = com.lightmark.data.settings.LightMarkSettings()
            )

            val appSettings = AppSettings(
                themeMode = ThemeMode.fromString(settings.themeMode),
                seedColor = settings.seedColor,
                iconPack = IconPack.fromString(settings.iconPack),
                useDynamicColor = settings.useDynamicColor
            )

            val showPrivacy = !settings.privacyAccepted

            LightMarkTheme(appSettings = appSettings) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when {
                        showPrivacy -> {
                            PrivacyDialog(
                                onAccept = {
                                    lifecycleScope.launch {
                                        settingsRepository.setPrivacyAccepted(true)
                                    }
                                },
                                onDecline = {
                                    finishAffinity()
                                }
                            )
                        }
                        authState is AuthState.Authenticated -> {
                            LightMarkNavHost(
                                initialRoute = "home",
                                userId = currentUser?.login ?: ""
                            )
                        }
                        authState is AuthState.Loading -> {
                            SplashScreen()
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
