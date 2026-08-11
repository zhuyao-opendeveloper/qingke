package com.lightmark.ui.screens.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lightmark.data.local.entity.TemplateEntity
import com.lightmark.ui.theme.Dimens

/**
 * 任务模板（#20）
 *
 * 一组子任务存成模板，一键生成完整任务树。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateScreen(
    onNavigateBack: () -> Unit,
    viewModel: TemplateViewModel = hiltViewModel()
) {
    val templates by viewModel.templates.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var editing by remember { mutableStateOf<TemplateEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<TemplateEntity?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("任务模板", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                showEditor = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "新建模板")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (templates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "还没有模板，点右下角新建一个",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = Dimens.lg,
                    end = Dimens.lg,
                    top = Dimens.sm,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.md)
            ) {
                items(templates, key = { it.id }) { tpl ->
                    TemplateCard(
                        template = tpl,
                        onApply = { viewModel.applyTemplate(tpl) },
                        onEdit = {
                            editing = tpl
                            showEditor = true
                        },
                        onDelete = { pendingDelete = tpl }
                    )
                }
            }
        }
    }

    if (showEditor) {
        TemplateEditorDialog(
            initial = editing,
            onDismiss = { showEditor = false },
            onSave = { name, desc, emoji, priority, tags, subtasks, dueInDays ->
                viewModel.saveTemplate(
                    id = editing?.id,
                    name = name,
                    description = desc,
                    emoji = emoji,
                    priority = priority,
                    categoryId = editing?.categoryId,
                    tags = tags,
                    subtasks = subtasks,
                    dueInDays = dueInDays
                )
                showEditor = false
            }
        )
    }

    pendingDelete?.let { tpl ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除模板") },
            text = { Text("确定删除「${tpl.name}」？已生成的任务不受影响。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTemplate(tpl.id)
                    pendingDelete = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateCard(
    template: TemplateEntity,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation)
    ) {
        Column(modifier = Modifier.padding(Dimens.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(template.emoji, fontSize = 22.sp)
                Spacer(Modifier.width(Dimens.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        template.name,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (template.description.isNotBlank()) {
                        Text(
                            template.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "编辑", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(Dimens.sm))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                AssistChip(
                    onClick = onApply,
                    label = { Text("${template.subtaskList.size} 个子任务") }
                )
                if (template.dueInDays != null) {
                    AssistChip(
                        onClick = onApply,
                        label = {
                            Text(
                                if (template.dueInDays == 0) "今天到期"
                                else "${template.dueInDays} 天后到期"
                            )
                        }
                    )
                }
                if (template.usageCount > 0) {
                    Text(
                        "已用 ${template.usageCount} 次",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (template.subtaskList.isNotEmpty()) {
                Spacer(Modifier.height(Dimens.sm))
                template.subtaskList.take(4).forEach { sub ->
                    Text(
                        "· $sub",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (template.subtaskList.size > 4) {
                    Text(
                        "· 还有 ${template.subtaskList.size - 4} 项…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(Dimens.md))

            FilledTonalButton(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Dimens.sm))
                Text("使用此模板")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateEditorDialog(
    initial: TemplateEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, Int?) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var desc by remember { mutableStateOf(initial?.description.orEmpty()) }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "\uD83D\uDCCB") }
    var priority by remember { mutableStateOf(initial?.priority ?: "MEDIUM") }
    var tags by remember { mutableStateOf(initial?.tags.orEmpty()) }
    var subtasks by remember { mutableStateOf(initial?.subtasks.orEmpty()) }
    var dueText by remember { mutableStateOf(initial?.dueInDays?.toString().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新建模板" else "编辑模板") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { if (it.length <= 2) emoji = it },
                        label = { Text("图标") },
                        singleLine = true,
                        modifier = Modifier.width(88.dp)
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("模板名称") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("说明（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subtasks,
                    onValueChange = { subtasks = it },
                    label = { Text("子任务，每行一个") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("标签，逗号分隔（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dueText,
                    onValueChange = { input -> dueText = input.filter { it.isDigit() } },
                    label = { Text("几天后到期（留空则不设）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("优先级", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                    priorityOptions.forEach { (value, label) ->
                        FilterChip(
                            selected = priority == value,
                            onClick = { priority = value },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        name,
                        desc,
                        emoji,
                        priority,
                        tags,
                        subtasks,
                        dueText.toIntOrNull()
                    )
                },
                enabled = name.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private val priorityOptions = listOf(
    "URGENT" to "紧急",
    "HIGH" to "高",
    "MEDIUM" to "中",
    "LOW" to "低",
    "IDLE" to "空闲"
)

