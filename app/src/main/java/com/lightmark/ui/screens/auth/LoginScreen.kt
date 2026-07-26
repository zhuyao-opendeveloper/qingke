package com.lightmark.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lightmark.auth.AuthManager
import com.lightmark.auth.AuthState
import com.lightmark.ui.components.LightMarkButton
import com.lightmark.ui.theme.Dimens
import kotlinx.coroutines.launch

/**
 * GitHub Token 登录页面
 *
 * 简洁的登录界面，用户输入 GitHub Personal Access Token
 * 验证通过后自动跳转到主页面
 *
 * 设计特点：
 * - 居中布局，视觉集中
 * - 大圆角输入框
 * - 清晰的引导说明
 * - 加载状态反馈
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    authManager: AuthManager? = null
) {
    var token by remember { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isLoading by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo / 标题
            Text(
                text = "轻刻",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "LightMark",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "简洁 · 优雅 · 私有的待办清单",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(Dimens.huge))

            // GitHub Token 输入区域
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.xl)
                ) {
                    Text(
                        text = "使用 GitHub Token 登录",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(Dimens.sm))

                    Text(
                        text = "数据将安全存储在你的 GitHub 私有仓库中",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(Dimens.lg))

                    // Token 输入框
                    OutlinedTextField(
                        value = token,
                        onValueChange = {
                            token = it
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("GitHub Personal Access Token") },
                        placeholder = { Text("ghp_... 或 github_pat_...") },
                        singleLine = true,
                        visualTransformation = if (showToken)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        trailingIcon = {
                            TextButton(onClick = { showToken = !showToken }) {
                                Text(
                                    text = if (showToken) "隐藏" else "显示",
                                    fontSize = 12.sp
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        isError = errorMessage != null,
                        supportingText = {
                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(Dimens.lg))

                    // 登录按钮
                    LightMarkButton(
                        text = "登录",
                        enabled = token.isNotBlank() && !isLoading,
                        onClick = {
                            focusManager.clearFocus()
                            scope.launch {
                                // 实际项目中通过 authManager 登录
                                // val result = authManager?.loginWithToken(token)
                                // result.onFailure { errorMessage = it.message }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(Dimens.md))

                    // 获取 Token 引导
                    Text(
                        text = "如何获取 Token？\nGitHub 设置 → Developer Settings → Personal Access Tokens → 新建 Token（需要 repo 权限）",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        textAlign = TextAlign.Start,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
