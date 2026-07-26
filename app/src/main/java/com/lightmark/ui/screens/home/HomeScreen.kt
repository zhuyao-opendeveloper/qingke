package com.lightmark.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lightmark.ui.components.EmptyState
import com.lightmark.ui.components.LightMarkSearchBar
import com.lightmark.ui.components.TodoItemCard
import com.lightmark.ui.theme.Dimens

/**
 * 首页——待办清单主界面
 *
 * 简洁、现代化的待办列表视图
 * 包含：
 * - 顶部标题栏（用户信息 + 设置入口）
 * - 搜索栏
 * - 待办列表（圆角悬浮卡片）
 * - 空状态占位
 * - FAB 添加按钮（在 Scaffold 中）
 *
 * @param viewModel 主页 ViewModel
 * @param onNavigateToAdd 跳转到添加页面
 * @param onNavigateToEdit 跳转到编辑页面
 * @param onNavigateToSettings 跳转到设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val todos by viewModel.todos.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val iconProvider = viewModel.currentIconProvider

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部标题栏
        TopAppBar(
            title = {
                Text(
                    text = "轻刻",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            },
            actions = {
                // 搜索按钮
                IconButton(onClick = { viewModel.toggleSearch() }) {
                    Icon(
                        imageVector = com.lightmark.icons.MaterialIconProvider.search,
                        contentDescription = "搜索"
                    )
                }
                // 设置按钮
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = com.lightmark.icons.MaterialIconProvider.settings,
                        contentDescription = "设置"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // 搜索栏（可展开/收起）
        AnimatedVisibility(
            visible = isSearching,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            LightMarkSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                iconProvider = iconProvider,
                modifier = Modifier.padding(bottom = Dimens.sm)
            )
        }

        // 待办列表
        if (todos.isEmpty()) {
            EmptyState(
                iconProvider = iconProvider,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = Dimens.sm,
                    bottom = 80.dp // 给 FAB 留空间
                )
            ) {
                // 今日待办标题
                item {
                    Text(
                        text = "待办事项 (${todos.size})",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            start = Dimens.lg,
                            bottom = Dimens.sm
                        )
                    )
                }

                items(
                    items = todos,
                    key = { it.id }
                ) { todo ->
                    TodoItemCard(
                        item = todo,
                        onToggle = { viewModel.toggleComplete(todo.id) },
                        onClick = { onNavigateToEdit(todo.id) },
                        onDelete = { viewModel.deleteTodo(todo.id) },
                        iconProvider = iconProvider
                    )
                }
            }
        }
    }
}
