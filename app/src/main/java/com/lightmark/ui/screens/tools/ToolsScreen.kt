package com.lightmark.ui.screens.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lightmark.ui.components.LightMarkCard
import com.lightmark.ui.theme.Dimens
import com.lightmark.util.openLightMarkWeb

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("工具箱", fontWeight = FontWeight.Bold) },
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
            ToolCard("番茄钟", "专注计时，工作 / 休息循环", Icons.Filled.Timer) { onNavigate("pomodoro") }
            ToolCard("闹钟", "设置提醒闹钟，到点响铃", Icons.Filled.Alarm) { onNavigate("alarm") }
            ToolCard("收集箱", "快速记录灵感与待办", Icons.Filled.Inbox) { onNavigate("inbox") }
            ToolCard("添加日历", "日 / 月 / 年视图查看待办", Icons.Filled.CalendarMonth) { onNavigate("calendar") }
            ToolCard("看板视图", "按待办 / 进行中 / 暂停 / 完成分列", Icons.Filled.Dashboard) { onNavigate("board") }
            ToolCard("四象限", "重要 × 紧急，帮你决定先做什么", Icons.Filled.ViewModule) { onNavigate("matrix") }
            ToolCard("习惯与目标", "每日打卡、连续天数、目标进度与里程碑", Icons.Filled.LocalFireDepartment) { onNavigate("habit") }
            ToolCard("任务模板", "常用流程存成模板，一键生成任务与子任务", Icons.Filled.ContentCopy) { onNavigate("template") }
            ToolCard("表格视图", "类 Excel 表格，一屏对比全部字段", Icons.Filled.TableChart) { onNavigate("table") }
            ToolCard("回顾复盘", "周报 / 月报，完成率、分布与逾期清理", Icons.Filled.Insights) { onNavigate("review") }
            ToolCard("标签管理", "重命名 / 合并 / 删除标签，统一改写所有待办", Icons.Filled.Sell) { onNavigate("tags") }
            ToolCard("心情记录", "每日打卡心情分数，回顾情绪变化", Icons.Filled.Favorite) { onNavigate("mood") }
            ToolCard("备份与导出", "JSON 备份、Markdown / CSV / HTML / iCal 导出与导入", Icons.Filled.Backup) { onNavigate("backup") }
            LightMarkCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { openLightMarkWeb(context) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(Dimens.sm)
                ) {
                    Text("🌐", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(Dimens.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("轻刻网页版", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(
                            "云端同步、AI 对话、跨设备访问，浏览器打开即用",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolCard(title: String, desc: String, icon: ImageVector, onClick: () -> Unit) {
    LightMarkCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(Dimens.sm)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(Dimens.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
