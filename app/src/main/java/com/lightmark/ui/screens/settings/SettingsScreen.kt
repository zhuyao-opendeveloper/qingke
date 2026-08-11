package com.lightmark.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import com.lightmark.domain.model.IconPack
import com.lightmark.domain.model.ThemeMode
import com.lightmark.icons.LightMarkIcon
import com.lightmark.icons.MaterialIconProvider
import com.lightmark.ui.components.LightMarkCard
import com.lightmark.ui.theme.Dimens
import com.lightmark.ui.theme.PRESET_NAMES
import com.lightmark.ui.theme.PRESET_ORDER

/**
 * 设置页面
 *
 * 用户可在此配置：
 * - 外观（主题模式 / 动态取色 / 图标库）
 * - 个性化称呼（本地昵称）
 * - 提醒开关
 * - 分类管理入口
 * - 数据同步 / 账户退出
 * - 重新查看隐私政策
 * - 关于
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    var showIconPackDialog by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.setBackgroundImageUri(it.toString()) }
    }

    // 自定义主题色可选色板
    val customColors = listOf(
        0xFF6750A4, 0xFF2E9E7B, 0xFF2D6FE0, 0xFFE1609A,
        0xFF4B7A2E, 0xFF5B6BB5, 0xFFE6852C, 0xFFD32F2F,
        0xFF00897B, 0xFF3949AB
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(text = "设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        LightMarkIcon(
                            provider = MaterialIconProvider,
                            icon = { close },
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.lg)
        ) {
            // ====== 外观 ======
            SectionTitle("外观")

            LightMarkCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(text = "主题模式", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(Dimens.sm))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = ThemeMode.fromString(settings.themeMode) == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.LIGHT -> "浅色"
                                            ThemeMode.DARK -> "深色"
                                            ThemeMode.SYSTEM -> "跟随系统"
                                        }, fontSize = 13.sp
                                    )
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Dimens.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("动态取色（Android 12+）", fontSize = 14.sp)
                        Switch(
                            checked = settings.useDynamicColor,
                            onCheckedChange = { viewModel.setUseDynamicColor(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.md))

            LightMarkCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showIconPackDialog = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("图标库", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text(
                            text = when (IconPack.fromString(settings.iconPack)) {
                                IconPack.MATERIAL -> "Material Symbols"
                                IconPack.FLUENT -> "Fluent UI"
                                IconPack.LUCIDE -> "Lucide"
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.lg))

            // ====== 主题与背景 ======
            SectionTitle("主题与背景")
            LightMarkCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("内置主题", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(Dimens.sm))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        PRESET_ORDER.forEach { id ->
                            FilterChip(
                                selected = settings.themeId == id && settings.customPrimary == null,
                                onClick = {
                                    viewModel.setThemeId(id)
                                    viewModel.setCustomPrimary(null)
                                },
                                label = {
                                    Text(PRESET_NAMES[id] ?: id, fontSize = 13.sp)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimens.md))
                    Text("自定义主题色", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(Dimens.sm))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        customColors.forEach { c ->
                            val selected = settings.customPrimary == c
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .border(
                                        2.dp,
                                        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { viewModel.setCustomPrimary(c) }
                            )
                        }
                        // 恢复默认（跟随内置主题）
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { viewModel.setCustomPrimary(null) },
                            contentAlignment = Alignment.Center
                        ) { Text("默认", fontSize = 11.sp) }
                    }

                    Spacer(modifier = Modifier.height(Dimens.md))
                    Text("背景图片", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(Dimens.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        Button(
                            onClick = { pickImage.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) { Text("选择背景图片") }
                        OutlinedButton(
                            onClick = { viewModel.setBackgroundImageUri("") },
                            modifier = Modifier.weight(1f)
                        ) { Text("清除背景") }
                    }
                    if (settings.backgroundImageUri.isNotBlank()) {
                        Spacer(modifier = Modifier.height(Dimens.sm))
                        Text(
                            "已设置自定义背景图片（将应用于全局）",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.lg))

            // ====== 个性化称呼 ======
            SectionTitle("称呼")

            LightMarkCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    OutlinedTextField(
                        value = settings.nickname,
                        onValueChange = { viewModel.setNickname(it.take(12)) },
                        label = { Text("昵称") },
                        placeholder = { Text("留空则只显示问候语") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Dimens.cardCornerRadius)
                    )
                    Spacer(modifier = Modifier.height(Dimens.sm))
                    Text(
                        text = "用于首页问候语，仅保存在本机，最多 12 个字。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.lg))

            // ====== 提醒 ======
            SectionTitle("提醒")
            LightMarkCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("待办到期提醒", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text("到达截止时间时发送本地通知", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.reminderEnabled,
                        onCheckedChange = { viewModel.setReminderEnabled(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.lg))

            // ====== 体验（#116 / #123 / #101） ======
            SectionTitle("体验")
            LightMarkCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("完成鼓励语", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text("完成任务时给一句小鼓励", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.encouragementEnabled,
                            onCheckedChange = { viewModel.setEncouragementEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(Dimens.md))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("触感反馈", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text("勾选完成、长按多选时轻微震动", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.hapticEnabled,
                            onCheckedChange = { viewModel.setHapticEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(Dimens.md))

                    Text("回收站保留时长", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Text("超过时长的已删除任务会自动彻底清除", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(Dimens.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                    ) {
                        listOf(7 to "7 天", 14 to "14 天", 30 to "30 天", 90 to "90 天", 0 to "永久")
                            .forEach { (days, label) ->
                                FilterChip(
                                    selected = settings.trashRetentionDays == days,
                                    onClick = { viewModel.setTrashRetentionDays(days) },
                                    label = { Text(label, fontSize = 13.sp) }
                                )
                            }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.lg))

            // ====== 分类 ======
            SectionTitle("数据")
            LightMarkCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToCategories
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Category, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(Dimens.sm))
                        Text("分类管理", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(Dimens.md))

            // 离线运行说明
            LightMarkCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("完全离线运行", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(Dimens.xs))
                    Text(
                        text = "轻刻不联网、不注册、不上传。所有待办只保存在本机数据库中，" +
                            "卸载应用即彻底清除。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Dimens.sm))
                    Text(
                        text = "换设备请到「工具 → 备份与导出」导出 JSON 备份文件，" +
                            "在新设备导入即可；该文件也可导入轻刻网页版。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.lg))

            // ====== 隐私 ======
            SectionTitle("隐私")
            LightMarkCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.reReviewPrivacy() }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("查看隐私政策", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Icon(Icons.Filled.ChevronRight, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(Dimens.xxxl))

            Text(
                text = "轻刻 LightMark v1.1.0",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(Dimens.xxl))
        }
    }

    if (showIconPackDialog) {
        AlertDialog(
            onDismissRequest = { showIconPackDialog = false },
            title = { Text("选择图标库") },
            text = {
                Column {
                    IconPack.entries.forEach { pack ->
                        val name = when (pack) {
                            IconPack.MATERIAL -> "Material Symbols"
                            IconPack.FLUENT -> "Fluent UI Icons"
                            IconPack.LUCIDE -> "Lucide Icons"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimens.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = IconPack.fromString(settings.iconPack) == pack,
                                onClick = {
                                    viewModel.setIconPack(pack)
                                    showIconPackDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(Dimens.sm))
                            Text(text = name, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIconPackDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = Dimens.sm)
    )
}
