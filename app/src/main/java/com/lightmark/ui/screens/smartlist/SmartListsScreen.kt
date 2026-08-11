package com.lightmark.ui.screens.smartlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lightmark.domain.model.Category
import com.lightmark.domain.model.SmartList
import com.lightmark.ui.components.LightMarkCard
import com.lightmark.ui.theme.Dimens

/**
 * 自定义智能清单管理界面（#28）
 *
 * 把筛选条件（快速筛选 / 分类 / 关键词 / 排序）保存为命名清单，首页一键套用。
 * 纯本地实现，不依赖网络。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartListsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SmartListsViewModel = hiltViewModel()
) {
    val smartLists by viewModel.smartLists.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("📋") }
    var quickFilter by remember { mutableStateOf<com.lightmark.ui.screens.home.QuickFilter?>(null) }
    var category by remember { mutableStateOf<Category?>(null) }
    var query by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf<com.lightmark.ui.screens.home.SortOrder?>(null) }
    var editingId by remember { mutableStateOf<String?>(null) }

    fun resetForm() {
        name = ""
        emoji = "📋"
        quickFilter = null
        category = null
        query = ""
        sortOrder = null
        editingId = null
    }

    fun startEdit(sl: SmartList) {
        editingId = sl.id
        name = sl.name
        emoji = sl.emoji
        quickFilter = sl.quickFilter?.let { runCatching { com.lightmark.ui.screens.home.QuickFilter.valueOf(it) }.getOrNull() }
        category = categories.firstOrNull { it.id == sl.categoryId }
        query = sl.query ?: ""
        sortOrder = sl.sortOrder?.let { runCatching { com.lightmark.ui.screens.home.SortOrder.valueOf(it) }.getOrNull() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智能清单", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            LightMarkCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Dimens.md)) {
                    Text(
                        text = if (editingId != null) "编辑清单" else "新建清单",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(Dimens.sm))

                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        OutlinedTextField(
                            value = emoji,
                            onValueChange = { if (it.length <= 2) emoji = it },
                            modifier = Modifier.width(64.dp),
                            label = { Text("图标") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("名称") },
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimens.sm))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OptionDropdown(
                            label = "快速筛选",
                            options = com.lightmark.ui.screens.home.QuickFilter.entries.toList(),
                            selected = quickFilter,
                            onSelected = { quickFilter = it },
                            textOf = { it.label }
                        )
                        OptionDropdown(
                            label = "排序",
                            options = com.lightmark.ui.screens.home.SortOrder.entries.toList(),
                            selected = sortOrder,
                            onSelected = { sortOrder = it },
                            textOf = { it.name }
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimens.sm))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OptionDropdown(
                            label = "分类（不限）",
                            options = categories,
                            selected = category,
                            onSelected = { category = it },
                            textOf = { it.name }
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimens.sm))

                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("关键词（可选，留空表示不限）") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(Dimens.md))

                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        Button(
                            onClick = {
                                if (name.isBlank()) return@Button
                                val list = SmartList(
                                    id = editingId ?: SmartList().id,
                                    name = name.trim(),
                                    emoji = emoji.ifBlank { "📋" },
                                    quickFilter = quickFilter?.name,
                                    categoryId = category?.id,
                                    query = query.trim().ifBlank { null },
                                    sortOrder = sortOrder?.name
                                )
                                viewModel.save(list)
                                resetForm()
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(if (editingId != null) "保存修改" else "保存清单") }
                        if (editingId != null) {
                            OutlinedButton(onClick = { resetForm() }) { Text("取消") }
                        }
                    }
                }
            }

            Text(
                text = "已保存 ${smartLists.size} 个清单",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.sm, bottom = Dimens.xs)
            )

            if (smartLists.isEmpty()) {
                Text(
                    text = "还没有智能清单。在上方定义筛选条件并保存即可。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                smartLists.forEach { sl ->
                    LightMarkCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(Dimens.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${sl.emoji} ${sl.name}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { startEdit(sl) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.delete(sl.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> OptionDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    onSelected: (T?) -> Unit,
    textOf: (T) -> String
) {
    var expanded by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { expanded = true }) {
        Text(if (selected != null) textOf(selected) else label, fontSize = 13.sp)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("（不限）") },
            onClick = { onSelected(null); expanded = false }
        )
        options.forEach { opt ->
            DropdownMenuItem(
                text = { Text(textOf(opt)) },
                onClick = { onSelected(opt); expanded = false }
            )
        }
    }
}
