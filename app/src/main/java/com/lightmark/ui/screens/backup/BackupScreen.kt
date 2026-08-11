package com.lightmark.ui.screens.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lightmark.ui.components.LightMarkCard
import com.lightmark.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 备份与导出（#95 / #96 / #97 / #104）
 *
 * 通过系统文件选择器（SAF）读写，无需申请存储权限。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val message by viewModel.message.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 待写出的内容，由具体导出动作填充后再拉起文件选择器
    val pending = remember { PendingExport() }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        pending.action?.invoke { content ->
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                }
            }.isSuccess
        }
        pending.action = null
    }

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }
        }.getOrNull()
        if (text != null) viewModel.importJson(text)
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("备份与导出", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.lg),
            verticalArrangement = Arrangement.spacedBy(Dimens.md)
        ) {
            Text(
                "数据完全保存在本机。导出的文件由你自己选择保存位置，不会上传到任何服务器。",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (busy) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(Dimens.sm))
                    Text("处理中…", fontSize = 13.sp)
                }
            }

            ActionCard(
                title = "导出 JSON 全量备份",
                desc = "包含待办、分类、归档与回收站，可用于恢复",
                icon = Icons.Filled.Download
            ) {
                pending.action = { write -> viewModel.exportJson(write) }
                createDocLauncher.launch(fileName("json"))
            }

            ActionCard(
                title = "导出 Markdown 清单",
                desc = "带复选框的清单，适合贴到笔记 / 文档",
                icon = Icons.Filled.Description
            ) {
                pending.action = { write -> viewModel.exportMarkdown(write) }
                createDocLauncher.launch(fileName("md"))
            }

            ActionCard(
                title = "导出 CSV 表格",
                desc = "Excel / 表格软件可直接打开（含 BOM，不乱码）",
                icon = Icons.Filled.TableChart
            ) {
                pending.action = { write -> viewModel.exportCsv(write) }
                createDocLauncher.launch(fileName("csv"))
            }

            ActionCard(
                title = "导出 HTML / 打印为 PDF",
                desc = "浏览器打开即可查看，用系统打印可另存为 PDF",
                icon = Icons.Filled.Html
            ) {
                pending.action = { write -> viewModel.exportHtml(write) }
                createDocLauncher.launch(fileName("html"))
            }

            ActionCard(
                title = "导出 iCalendar (.ics)",
                desc = "导入系统日历 / Google Calendar / Outlook 查看截止日程",
                icon = Icons.Filled.EventNote
            ) {
                pending.action = { write -> viewModel.exportIcs(write) }
                createDocLauncher.launch(fileName("ics"))
            }

            Spacer(modifier = Modifier.height(Dimens.sm))

            ActionCard(
                title = "从 JSON 备份导入",
                desc = "同 ID 的任务会被覆盖，其余合并保留",
                icon = Icons.Filled.Upload
            ) {
                openDocLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            }
        }
    }
}

/** 保存待执行的导出动作（选择文件后再真正生成内容） */
private class PendingExport {
    var action: (((String) -> Boolean) -> Unit)? = null
}

private fun fileName(ext: String): String {
    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
    return "lightmark_$stamp.$ext"
}

@Composable
private fun ActionCard(
    title: String,
    desc: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    LightMarkCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(Dimens.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    desc,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
