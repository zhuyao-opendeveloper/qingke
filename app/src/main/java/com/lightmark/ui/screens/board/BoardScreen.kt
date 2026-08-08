package com.lightmark.ui.screens.board

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.lightmark.domain.model.TodoItem
import com.lightmark.ui.components.Dot
import com.lightmark.ui.components.MiniTaskCard
import com.lightmark.ui.theme.Dimens

/**
 * 看板视图（#52）
 *
 * 四列：待办 / 进行中 / 已暂停 / 已完成。
 * 移动端不做拖拽，改为点击卡片弹出菜单选择目标列，操作更稳。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardScreen(
    onNavigateBack: () -> Unit,
    onOpenTodo: (String) -> Unit,
    viewModel: BoardViewModel = hiltViewModel()
) {
    val todos by viewModel.todos.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("看板", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Dimens.md),
            horizontalArrangement = Arrangement.spacedBy(Dimens.md)
        ) {
            BoardColumn.values().forEach { column ->
                val columnItems = todos.filter { columnOf(it) == column }
                BoardColumnView(
                    column = column,
                    items = columnItems,
                    onOpenTodo = onOpenTodo,
                    onMove = { id, target -> viewModel.moveTo(id, target) }
                )
            }
        }
    }
}

@Composable
private fun BoardColumnView(
    column: BoardColumn,
    items: List<TodoItem>,
    onOpenTodo: (String) -> Unit,
    onMove: (String, BoardColumn) -> Unit
) {
    Column(
        modifier = Modifier
            .width(268.dp)
            .fillMaxHeight()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Dot(color = colorOfColumn(column))
            Spacer(modifier = Modifier.width(Dimens.sm))
            Text(
                text = column.label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(Dimens.sm))
            Text(
                text = items.size.toString(),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(Dimens.sm))

        if (items.isEmpty()) {
            Text(
                text = "暂无任务",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Dimens.md)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.sm),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = Dimens.xxl)
            ) {
                items(items, key = { it.id }) { item ->
                    BoardCardWithMenu(
                        item = item,
                        current = column,
                        onOpenTodo = onOpenTodo,
                        onMove = onMove
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardCardWithMenu(
    item: TodoItem,
    current: BoardColumn,
    onOpenTodo: (String) -> Unit,
    onMove: (String, BoardColumn) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        MiniTaskCard(item = item, onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("打开编辑") },
                onClick = { expanded = false; onOpenTodo(item.id) }
            )
            Divider()
            BoardColumn.values().filter { it != current }.forEach { target ->
                DropdownMenuItem(
                    text = { Text("移动到「${target.label}」") },
                    onClick = { expanded = false; onMove(item.id, target) }
                )
            }
        }
    }
}

private fun colorOfColumn(column: BoardColumn): Color = when (column) {
    BoardColumn.TODO -> Color(0xFF90A4AE)
    BoardColumn.DOING -> Color(0xFF2196F3)
    BoardColumn.PAUSED -> Color(0xFFFF9800)
    BoardColumn.DONE -> Color(0xFF4CAF50)
}
