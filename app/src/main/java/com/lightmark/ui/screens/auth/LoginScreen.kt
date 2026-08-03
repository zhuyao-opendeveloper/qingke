package com.lightmark.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lightmark.ui.components.LightMarkButton
import com.lightmark.ui.components.LightMarkTextButton
import com.lightmark.ui.theme.Dimens

/**
 * 登录页面
 *
 * 使用 GitHub Personal Access Token 登录
 * 渐变背景 + 圆角卡片设计
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onLocalUse: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    var token by remember { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }
    var showLocalWarning by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val loginSuccess by viewModel.loginSuccess.collectAsState()

    // 登录成功后回调
    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            onLoginSuccess()
        }
    }

    val gradientColors = listOf(
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surface
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = gradientColors,
                    startY = 0f,
                    endY = 1000f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo 区域
            Text(
                text = "轻刻",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "LightMark",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "简洁 · 优雅 · 私有的待办清单",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(Dimens.huge))

            // 登录卡片
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(Dimens.xl)) {
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

                    OutlinedTextField(
                        value = token,
                        onValueChange = {
                            token = it
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
                                Text(text = if (showToken) "隐藏" else "显示", fontSize = 12.sp)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        isError = errorMessage != null,
                        supportingText = {
                            if (errorMessage != null) {
                                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(Dimens.lg))

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        LightMarkButton(
                            text = "登录",
                            enabled = token.isNotBlank(),
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.login(token)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(Dimens.md))

                    Text(
                        text = "如何获取 Token？\nGitHub 设置 → Developer Settings → Personal Access Tokens → 新建 Token（需要 repo 权限）",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        textAlign = TextAlign.Start,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(Dimens.md))

                    LightMarkTextButton(
                        text = "暂不登录，本地使用",
                        onClick = { showLocalWarning = true }
                    )
                }
            }
        }
    }

    if (showLocalWarning) {
        AlertDialog(
            onDismissRequest = { showLocalWarning = false },
            confirmButton = {
                TextButton(onClick = {
                    showLocalWarning = false
                    onLocalUse()
                }) {
                    Text("仍然使用")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocalWarning = false }) {
                    Text("取消")
                }
            },
            title = { Text("本地使用提醒") },
            text = {
                Text(
                    "你将进入「本地使用」模式：所有待办、分类与设置数据都只保存在本机，" +
                    "无法同步或上传到任何云端。更换设备或卸载应用后，这些数据将无法找回。\n\n" +
                    "如需跨设备备份，请改用 GitHub 账号登录。",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )
    }
}
