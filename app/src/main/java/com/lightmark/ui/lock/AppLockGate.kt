package com.lightmark.ui.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lightmark.ui.theme.Dimens
import java.util.concurrent.Executor

/**
 * 生物识别应用锁门禁（#96）
 *
 * 当设置中开启「生物识别锁」时：
 * - 应用进入前台（冷启动 / 从后台返回）会自动要求指纹或面容验证；
 * - 验证通过前不渲染真实内容，仅显示锁定页；
 * - 应用切到后台再回来会重新上锁。
 *
 * 纯本地实现，无任何网络依赖。
 */
@Composable
fun AppLockGate(
    biometricLockEnabled: Boolean,
    content: @Composable () -> Unit
) {
    if (!biometricLockEnabled) {
        content()
        return
    }

    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var unlocked by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    // 切到后台即重新上锁
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                unlocked = false
                authError = null
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    if (unlocked) {
        content()
        return
    }

    val prompt = remember(activity) {
        if (activity == null) null
        else {
            val executor: Executor = ContextCompat.getMainExecutor(activity)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlocked = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    authError = errString.toString()
                }

                override fun onAuthenticationFailed() {
                    authError = "认证失败，请重试"
                }
            }
            BiometricPrompt(activity, executor, callback)
        }
    }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("解锁轻刻")
            .setSubtitle("验证指纹或面容以进入应用")
            .setNegativeButtonText("取消")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                        or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()
    }

    fun launchAuth() {
        if (activity != null && prompt != null) {
            try {
                prompt.authenticate(promptInfo)
            } catch (e: Exception) {
                authError = "无法启动生物识别：${e.message}"
            }
        } else {
            authError = "当前设备不支持生物识别"
        }
    }

    // 进入即自动弹出系统认证
    androidx.compose.runtime.LaunchedEffect(Unit) { launchAuth() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(Dimens.md))
            Text("轻刻已锁定", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(Dimens.xs))
            Text(
                "验证指纹或面容以进入",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Dimens.lg))
            TextButton(onClick = { launchAuth() }) {
                Text("点击解锁")
            }
            if (authError != null) {
                Spacer(modifier = Modifier.height(Dimens.sm))
                Text(
                    authError!!,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
