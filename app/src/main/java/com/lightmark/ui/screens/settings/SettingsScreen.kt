package com.lightmark.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lightmark.domain.model.IconPack
import com.lightmark.domain.model.ThemeMode
import com.lightmark.icons.MaterialIconProvider
import com.lightmark.ui.components.LightMarkCard
import com.lightmark.ui.theme.Dimens

/**
 * 设置页面
 *
 * 用户可在此配置：
 * - 主题（浅色/深色/跟随系统）
 * - 图标库（Material Symbols / Fluent UI / Lucide）
 * - 账户信息与退出登录
 * - 数据同步
 * - OpenClaw 接入配置
 * - 关于
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    var iconPack by remember { mutableStateOf(IconPack.MATERIAL) }
    var showIconPackDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = MaterialIconProvider.close,
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
            // ====== 外观设置 ======
            SectionTitle("外观")

            // 主题选择
            LightMarkCard(
                contentPadding = PaddingValues(Dimens.md),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "主题模式",
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(Dimens.sm))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val label = when (mode) {
                                ThemeMode.LIGHT -> "浅色"
                                ThemeMode.DARK -> "深色"
                                ThemeMode.SYSTEM -> "跟随系统"
                            }
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { themeMode = mode },
                                label = { Text(label, fontSize = 13.sp) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.md))

            // 图标库选择
            LightMarkCard(
                contentPadding = PaddingValues(Dimens.md),
                modifier = Modifier.fillMaxWidth(),
                onClick = { showIconPackDialog = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "图标库",
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        Text(
                            text = when (iconPack) {
                                IconPack.MATERIAL -> "Material Symbols"
                                IconPack.FLUENT -> "Fluent UI"
                                IconPack.LUCIDE -> "Lucide"
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.lg))

            // ====== 数据设置 ======
            SectionTitle("数据")

            LightMarkCard(
                contentPadding = PaddingValues(Dimens.md),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "数据同步",
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "同步到 GitHub 私有仓库",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = true,
                            onCheckedChange = { }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.sm))
                    TextButton(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("立即同步")
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.lg))

            // ====== 账户 ======
            SectionTitle("账户")

            LightMarkCard(
                contentPadding = PaddingValues(Dimens.md),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "GitHub 账户",
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "已登录",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "U",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.sm))
                    TextButton(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("退出登录")
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.lg))

            // ====== 集成 ======
            SectionTitle("集成")

            LightMarkCard(
                contentPadding = PaddingValues(Dimens.md),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "OpenClaw",
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "通过 OpenClaw API 接入 AI 能力",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = true,
                        onCheckedChange = { }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.xxxl))

            // 版本信息
            Text(
                text = "轻刻 LightMark v1.0.0",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(Dimens.xxl))
        }
    }

    // 图标库选择对话框
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
                                selected = iconPack == pack,
                                onClick = {
                                    iconPack = pack
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
                TextButton(onClick = { showIconPackDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 设置页面分区标题
 */
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
