package com.lightmark.ui.screens.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lightmark.domain.model.Priority
import com.lightmark.domain.model.TodoItem
import com.lightmark.ui.components.MiniTaskCard
import com.lightmark.ui.theme.Dimens

/**
 * 四象限视图（#53，艾森豪威尔矩阵）
 *
 * 重要 = 优先级 ≥ 高；紧急 = 48 小时内到期或已逾期。
 * 点击卡片可打开编辑，或直接调整优先级切换象限。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatrixScreen(
    onNavigateBack: () -> Unit,
    onOpenTodo: (String) -> Unit,
    viewModel: BoardViewModel = hiltViewModel()
) {
    val todos by viewModel.todos.collectAsState()
    val pending = todos.filter { !it.isCompleted }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("四象限", fontWeight = FontWeight.Bold) },
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
                .padding(Dimens.md),
            verticalArrangement = Arrangement.spacedBy(Dimens.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Dimens.md)
            ) {
                QuadrantBox(
                    quadrant = Quadrant.Q1,
                    items = pending.filter { quadrantOf(it) == Quadrant.Q1 },
                    onOpenTodo = onOpenTodo,
                    onSetPriority = viewModel::setPriority,
                    onComplete = viewModel::toggleComplete,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                QuadrantBox(
                    quadrant = Quadrant.Q2,
                    items = pending.filter { quadrantOf(it) == Quadrant.Q2 },
                    onOpenTodo = onOpenTodo,
                    onSetPriority = viewModel::setPriority,
                    onComplete = viewModel::toggleComplete,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Dimens.md)
            ) {
                QuadrantBox(
                    quadrant = Quadrant.Q3,
                    items = pending.filter { quadrantOf(it) == Quadrant.Q3 },
                    onOpenTodo = onOpenTodo,
                    onSetPriority = viewModel::setPriority,
                    onComplete = viewModel::toggleComplete,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                QuadrantBox(
                    quadrant = Quadrant.Q4,
                    items = pending.filter { quadrantOf(it) == Quadrant.Q4 },
                    onOpenTodo = onOpenTodo,
                    onSetPriority = viewModel::setPriority,
                    onComplete = viewModel::toggleComplete,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun QuadrantBox(
    quadrant: Quadrant,
    items: List<TodoItem>,
    onOpenTodo: (String) -> Unit,
    onSetPriority: (String, Priority) -> Unit,
    onComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = colorOfQuadrant(quadrant)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.08f))
            .padding(Dimens.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(Dimens.xs))
            Text(
                text = quadrant.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(Dimens.xs))
            Text(
                text = items.size.toString(),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = quadrant.hint,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Dimens.xs))
        Divider(color = accent.copy(alpha = 0.25f))
        Spacer(modifier = Modifier.height(Dimens.xs))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.xs),
            contentPadding = PaddingValues(bottom = Dimens.sm)
        ) {
            items(items, key = { it.id }) { item ->
                MatrixCardWithMenu(
                    item = item,
                    onOpenTodo = onOpenTodo,
                    onSetPriority = onSetPriority,
                    onComplete = onComplete
                )
            }
        }
    }
}

@Composable
private fun MatrixCardWithMenu(
    item: TodoItem,
    onOpenTodo: (String) -> Unit,
    onSetPriority: (String, Priority) -> Unit,
    onComplete: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        MiniTaskCard(item = item, onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("打开编辑") },
                onClick = { expanded = false; onOpenTodo(item.id) }
            )
            DropdownMenuItem(
                text = { Text("标记完成") },
                onClick = { expanded = false; onComplete(item.id) }
            )
            Divider()
            Priority.values().reversed().forEach { p ->
                DropdownMenuItem(
                    text = { Text("优先级 → ${com.lightmark.ui.components.priorityLabelOf(p)}") },
                    onClick = { expanded = false; onSetPriority(item.id, p) }
                )
            }
        }
    }
}

private fun colorOfQuadrant(q: Quadrant): Color = when (q) {
    Quadrant.Q1 -> Color(0xFFF44336)
    Quadrant.Q2 -> Color(0xFF2196F3)
    Quadrant.Q3 -> Color(0xFFFF9800)
    Quadrant.Q4 -> Color(0xFF9E9E9E)
}
