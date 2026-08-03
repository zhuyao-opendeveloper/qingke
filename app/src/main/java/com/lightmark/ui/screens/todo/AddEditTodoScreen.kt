package com.lightmark.ui.screens.todo

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lightmark.domain.model.Priority
import com.lightmark.icons.IconProvider
import com.lightmark.icons.LightMarkIcon
import com.lightmark.icons.MaterialIconProvider
import com.lightmark.ui.components.LightMarkButton
import com.lightmark.ui.theme.Dimens
import com.lightmark.util.DateTimeUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTodoScreen(
    todoId: String?,
    iconProvider: IconProvider,
    onNavigateBack: () -> Unit,
    viewModel: AddEditTodoViewModel = hiltViewModel()
) {
    val isEditing = todoId != null
    val title by viewModel.title.collectAsState()
    val description by viewModel.description.collectAsState()
    val priority by viewModel.priority.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val categoryId by viewModel.categoryId.collectAsState()
    val isPinned by viewModel.isPinned.collectAsState()
    val dueDate by viewModel.dueDate.collectAsState()
    val reminderEnabled by viewModel.reminderEnabled.collectAsState()
    val aiState by viewModel.aiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val saveComplete by viewModel.saveComplete.collectAsState()

    var tagInput by remember { mutableStateOf("") }
    var showCategoryDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(todoId) {
        if (todoId != null) {
            viewModel.loadTodo(todoId)
        }
    }

    LaunchedEffect(saveComplete) {
        if (saveComplete) {
            onNavigateBack()
        }
    }

    LaunchedEffect(aiState) {
        if (aiState is AiUiState.Success || aiState is AiUiState.Error) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearAiState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "编辑待办" else "新建待办",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        LightMarkIcon(
                            provider = iconProvider,
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
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.updateTitle(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("标题") },
                placeholder = { Text("写点什么...") },
                singleLine = true,
                shape = RoundedCornerShape(Dimens.cardCornerRadius)
            )

            Spacer(modifier = Modifier.height(Dimens.md))

            OutlinedTextField(
                value = description,
                onValueChange = { viewModel.updateDescription(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                label = { Text("描述（可选）") },
                shape = RoundedCornerShape(Dimens.cardCornerRadius),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(Dimens.lg))

            // AI 智能区
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.cardCornerRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(Dimens.md)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(Dimens.sm))
                        Text("AI 助手", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(Dimens.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        FilledTonalButton(
                            onClick = { viewModel.smartFill() },
                            enabled = aiState != AiUiState.Loading && title.isNotBlank()
                        ) {
                            Text("智能填写")
                        }
                        FilledTonalButton(
                            onClick = { viewModel.polishDescription() },
                            enabled = aiState != AiUiState.Loading && description.isNotBlank()
                        ) {
                            Text("润色描述")
                        }
                    }
                    AnimatedVisibility(
                        visible = aiState is AiUiState.Loading,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = Dimens.sm)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(Dimens.sm))
                            Text("AI 思考中...", fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    AnimatedVisibility(
                        visible = aiState is AiUiState.Success || aiState is AiUiState.Error,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = fadeOut()
                    ) {
                        val msg = when (val s = aiState) {
                            is AiUiState.Success -> s.message
                            is AiUiState.Error -> "⚠ ${s.message}"
                            else -> ""
                        }
                        Text(
                            text = msg,
                            fontSize = 13.sp,
                            color = if (aiState is AiUiState.Error)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Dimens.sm)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.lg))

            // 分类
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                Text("分类", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(56.dp))
                FilledTonalButton(onClick = { showCategoryDialog = true }) {
                    val name = categories.find { it.id == categoryId }?.name ?: "未分类"
                    Text(name)
                }
            }

            Spacer(modifier = Modifier.height(Dimens.lg))

            Text("优先级", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(Dimens.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                Priority.entries.forEach { p ->
                    val label = when (p) {
                        Priority.LOW -> "低"
                        Priority.MEDIUM -> "中"
                        Priority.HIGH -> "高"
                        Priority.URGENT -> "紧急"
                    }
                    FilterChip(
                        selected = priority == p,
                        onClick = { viewModel.updatePriority(p) },
                        label = { Text(label, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (p) {
                                Priority.LOW -> MaterialTheme.colorScheme.secondaryContainer
                                Priority.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer
                                Priority.HIGH -> MaterialTheme.colorScheme.errorContainer
                                Priority.URGENT -> MaterialTheme.colorScheme.primaryContainer
                            }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.lg))

            // 截止时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                Icon(imageVector = Icons.Filled.Schedule, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Text("截止时间", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(64.dp))
                FilledTonalButton(onClick = {
                    showDateTimePicker(context, dueDate) { ts -> viewModel.setDueDate(ts) }
                }) {
                    Text(if (dueDate != null) DateTimeUtils.formatDateTime(dueDate!!) else "设置")
                }
                if (dueDate != null) {
                    IconButton(onClick = { viewModel.setDueDate(null) }) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "清除",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp))
                    }
                }
            }

            // 置顶
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                Icon(imageVector = Icons.Filled.PushPin, contentDescription = null,
                    tint = if (isPinned) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp))
                Text("置顶此待办", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f))
                Switch(checked = isPinned, onCheckedChange = { viewModel.setPinned(it) })
            }

            // 提醒
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                Icon(imageVector = Icons.Filled.Schedule, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Text("到点提醒我", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f))
                Switch(checked = reminderEnabled, onCheckedChange = { viewModel.setReminderEnabled(it) })
            }

            Spacer(modifier = Modifier.height(Dimens.lg))

            Text("标签", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(Dimens.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("添加标签") },
                    singleLine = true,
                    shape = RoundedCornerShape(Dimens.cardCornerRadius)
                )
                FilledTonalButton(
                    onClick = {
                        if (tagInput.isNotBlank()) {
                            viewModel.addTag(tagInput)
                            tagInput = ""
                        }
                    }
                ) {
                    Text("添加")
                }
            }
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    tags.forEach { tag ->
                        InputChip(
                            selected = false,
                            onClick = { viewModel.removeTag(tag) },
                            label = { Text(tag, fontSize = 12.sp) },
                            trailingIcon = {
                                Icon(
                                    imageVector = MaterialIconProvider.close,
                                    contentDescription = "移除",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.xxxl))

            if (isLoading) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LightMarkButton(
                    text = if (isEditing) "保存修改" else "创建待办",
                    enabled = title.isNotBlank(),
                    onClick = { viewModel.save() }
                )
            }
        }
    }

    // 分类选择弹窗
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("选择分类") },
            text = {
                Column {
                    CategoryOption("未分类", categoryId == null) {
                        viewModel.setCategoryId(null); showCategoryDialog = false
                    }
                    categories.forEach { cat ->
                        CategoryOption(cat.name, categoryId == cat.id) {
                            viewModel.setCategoryId(cat.id); showCategoryDialog = false
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun CategoryOption(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(Dimens.sm))
        Text(text = name, fontSize = 15.sp)
    }
}

/** 日期 + 时间选择器（原生 Dialog，稳定可靠） */
private fun showDateTimePicker(
    context: android.content.Context,
    initial: Long?,
    onResult: (Long) -> Unit
) {
    val cal = Calendar.getInstance().apply { initial?.let { timeInMillis = it } }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            cal.set(year, month, day)
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    cal.set(Calendar.MINUTE, minute)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    onResult(cal.timeInMillis)
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}
