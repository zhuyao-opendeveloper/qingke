package com.lightmark.ui.screens.todo

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
    val isLoading by viewModel.isLoading.collectAsState()
    val saveComplete by viewModel.saveComplete.collectAsState()

    var tagInput by remember { mutableStateOf("") }

    // 编辑模式：加载已有数据
    LaunchedEffect(todoId) {
        if (todoId != null) {
            viewModel.loadTodo(todoId)
        }
    }

    // 保存完成后返回
    LaunchedEffect(saveComplete) {
        if (saveComplete) {
            onNavigateBack()
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

            Text(
                text = "优先级",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Dimens.sm))

            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
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

            Text(
                text = "标签",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                ) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
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
}
