package com.lightmark.ui.screens.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lightmark.ui.components.LightMarkButton
import com.lightmark.ui.theme.Dimens

/**
 * 首次启动的隐私政策与用户协议弹窗
 *
 * 全屏对话框，内容可滚动阅读。
 * - 「同意并继续」：写入已同意状态并进入应用
 * - 「不同意并退出」：退出应用
 *
 * @param onAccept 用户同意
 * @param onDecline 用户拒绝（退出应用）
 */
@Composable
fun PrivacyDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* 必须明确选择，禁止点击外部关闭 */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimens.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(Dimens.xl))

                Text(
                    text = "隐私政策与用户协议",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(Dimens.lg))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    PrivacyParagraph(
                        title = "一、我们收集的信息",
                        body = "轻刻（LightMark）是一款本地优先的待办清单应用。默认情况下，你的待办数据仅存储在设备本地数据库中。" +
                            "当你选择使用 GitHub 账户登录并开启数据同步时，相关数据会以加密文件的形式存储在你自己的 GitHub 私有仓库中，他人无法访问。"
                    )
                    PrivacyParagraph(
                        title = "二、AI 与 OpenClaw 功能",
                        body = "应用内置 AI 能力（智能填写、润色、生成待办、对话）。当你在「设置 → 集成」中主动填入 OpenClaw 的接口地址与密钥后，" +
                            "你输入的标题、描述、待办文本会被发送至你所配置的接口以完成智能处理。我们不会代为存储这些密钥，也不会上传至任何第三方服务器。" +
                            "如不使用 AI 功能，应用将以本地规则兜底运行，数据不出本机。"
                    )
                    PrivacyParagraph(
                        title = "三、提醒与通知",
                        body = "当你为待办设置截止时间并开启提醒时，应用会使用系统闹钟在指定时间向你发送本地通知。我们不会将提醒内容上传到任何服务器。"
                    )
                    PrivacyParagraph(
                        title = "四、数据存储与安全",
                        body = "GitHub Token 使用 Android 密钥库（EncryptedSharedPreferences）加密保存。我们建议你仅授予最小必要权限（repo）。"
                    )
                    PrivacyParagraph(
                        title = "五、你的权利",
                        body = "你可以随时在「设置」中清除登录信息、关闭 AI 功能、查看本协议。继续使用该应用即表示你已阅读并同意上述条款。"
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.md))

                LightMarkButton(
                    text = "同意并继续",
                    onClick = onAccept
                )

                Spacer(modifier = Modifier.height(Dimens.sm))

                TextButton(
                    onClick = onDecline,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "不同意并退出",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.sm))
            }
        }
    }
}

@Composable
private fun PrivacyParagraph(title: String, body: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(Dimens.xs))
    Text(
        text = body,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start
    )
    Spacer(modifier = Modifier.height(Dimens.lg))
}
