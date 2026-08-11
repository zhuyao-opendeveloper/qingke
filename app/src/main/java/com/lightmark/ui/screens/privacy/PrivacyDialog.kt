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
                        title = "一、我们不收集任何信息",
                        body = "轻刻（LightMark）是一款完全离线的待办清单应用。应用未申请网络访问权限，不含任何联网代码，" +
                            "因此在技术上无法收集、上传或共享你的任何数据。我们不要求注册账号，不获取手机号、位置、通讯录、设备标识等个人信息。"
                    )
                    PrivacyParagraph(
                        title = "二、数据存储位置",
                        body = "你创建的全部内容——待办、分类、标签、习惯打卡、目标、模板、番茄钟记录——" +
                            "均只保存在本设备的应用私有数据库中，其他应用无法读取。卸载轻刻即彻底删除这些数据，且不可恢复，请提前备份。"
                    )
                    PrivacyParagraph(
                        title = "三、智能功能如何工作",
                        body = "应用内的智能填写、文本润色、摘要提炼、一句话生成待办，全部由设备本机的规则算法完成，" +
                            "不调用任何大模型接口，不产生网络请求。你输入的任何文字都不会离开本机。"
                    )
                    PrivacyParagraph(
                        title = "四、提醒与通知",
                        body = "当你为待办设置截止时间并开启提醒时，应用会使用系统闹钟在指定时间发送本地通知。" +
                            "该过程完全在设备内完成，不经过任何服务器。"
                    )
                    PrivacyParagraph(
                        title = "五、备份与导出",
                        body = "你可以在「工具 → 备份与导出」中主动导出 JSON / Markdown / CSV / HTML / iCal 文件。" +
                            "导出文件由你自行保存和传输，应用不会代为上传。导出内容包含你的待办明细，请妥善保管。"
                    )
                    PrivacyParagraph(
                        title = "六、权限说明",
                        body = "应用仅申请与核心功能直接相关的权限：通知（发送到期提醒）、精确闹钟（准时唤醒提醒）、" +
                            "震动（完成任务触感反馈）、开机自启（重启后恢复未触发的提醒）。未申请网络、位置、通讯录、摄像头、麦克风等权限。"
                    )
                    PrivacyParagraph(
                        title = "七、你的权利",
                        body = "你对本机数据拥有完全控制权：可随时删除任意条目、清空回收站、导出备份或卸载应用。" +
                            "你可以在「设置 → 隐私」中重新查看本协议。继续使用即表示你已阅读并同意上述条款。"
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
