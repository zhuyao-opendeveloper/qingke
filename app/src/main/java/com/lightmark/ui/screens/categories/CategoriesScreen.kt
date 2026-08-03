package com.lightmark.ui.screens.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lightmark.domain.model.Category
import com.lightmark.ui.theme.Dimens

/**
 * 分类管理页
 *
 * 列出分类（含色块与待办数），支持新增 / 编辑 / 删除。
 * 新增/编辑使用底部弹窗（ModalBottomSheet 风格 AlertDialog），
 * 含名称输入与预设调色板。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val counts by viewModel.counts.collectAsState()

    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Category?>(null) }
    var nameInput by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(PRESET_COLORS[0]) }
    var pendingDelete by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分类管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editing = null
                    nameInput = ""
                    selectedColor = PRESET_COLORS[0]
                    showEditor = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = "新增分类")
            }
        }
    ) { padding ->
        if (categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "还没有分类，点右下角 + 新建",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(Dimens.lg),
                verticalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                items(categories, key = { it.id }) { cat ->
                    val count = counts[cat.id] ?: 0
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.cardCornerRadius)),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = Dimens.cardElevation,
                        onClick = {
                            editing = cat
                            nameInput = cat.name
                            selectedColor = cat.color
                            showEditor = true
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimens.lg),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(cat.color))
                            )
                            Spacer(modifier = Modifier.width(Dimens.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cat.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "$count 条待办",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { pendingDelete = cat }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 编辑弹窗
    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editing == null) "新建分类" else "编辑分类") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Dimens.md))
                    Text("颜色", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(Dimens.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        PRESET_COLORS.forEach { c ->
                            val selected = c == selectedColor
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .clickable { selectedColor = c }
                                    .then(
                                        if (selected)
                                            Modifier.background(
                                                Color.Transparent,
                                                CircleShape
                                            )
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onPrimary)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editing == null) {
                            viewModel.addCategory(nameInput, selectedColor)
                        } else {
                            viewModel.updateCategory(editing!!, nameInput, selectedColor)
                        }
                        showEditor = false
                    },
                    enabled = nameInput.isNotBlank()
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditor = false }) { Text("取消") }
            }
        )
    }

    // 删除确认
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除分类") },
            text = { Text("确定删除「${pendingDelete!!.name}」？该分类下的待办不会被删除，仅解除关联。") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteCategory(pendingDelete!!); pendingDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

private val PRESET_COLORS = listOf(
    0xFF6750A4, 0xFF2196F3, 0xFF4CAF50, 0xFFFF9800,
    0xFFE91E63, 0xFF009688, 0xFFF44336, 0xFF3F51B5,
    0xFF795548, 0xFF607D8B, 0xFF9C27B0, 0xFF00BCD4
)
