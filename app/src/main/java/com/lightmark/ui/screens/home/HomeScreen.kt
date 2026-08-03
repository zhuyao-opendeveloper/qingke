package com.lightmark.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.animateItemPlacement
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.lightmark.icons.IconProvider
import com.lightmark.icons.LightMarkIcon
import com.lightmark.ui.components.EmptyState
import com.lightmark.ui.components.LightMarkSearchBar
import com.lightmark.ui.components.TodoItemCard
import com.lightmark.ui.theme.Dimens

/**
 * 首页——待办清单主界面
 *
 * 交互与动效：
 * - 下拉刷新（从 GitHub 同步）
 * - 列表项入场动画 + 置顶重排动画（animateItemPlacement）
 * - 左右滑动：右滑完成 / 左滑删除（带动画背景）
 * - 删除撤销（Snackbar）
 * - 分类筛选、排序菜单
 * - 顶栏快捷入口：搜索 / AI / 设置
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    userName: String = "",
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAi: () -> Unit
) {
    val todos by viewModel.todos.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val lastDeleted by viewModel.lastDeleted.collectAsState()
    val iconProvider = viewModel.currentIconProvider

    val snackbarHostState = remember { SnackbarHostState() }
    var showSortMenu by remember { mutableStateOf(false) }

    // 删除撤销提示
    LaunchedEffect(lastDeleted) {
        val deleted = lastDeleted
        if (deleted != null) {
            val result = snackbarHostState.showSnackbar(
                message = "已删除「${deleted.title}」",
                actionLabel = "撤销",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            } else {
                viewModel.clearLastDeleted()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部标题栏
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userName.firstOrNull()?.uppercase() ?: "U",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        Text(
                            text = "轻刻",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAi) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "AI 助手",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { viewModel.toggleSearch() }) {
                        LightMarkIcon(
                            provider = iconProvider,
                            icon = { search },
                            contentDescription = "搜索"
                        )
                    }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.Sort,
                            contentDescription = "排序"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        LightMarkIcon(
                            provider = iconProvider,
                            icon = { settings },
                            contentDescription = "设置"
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(sortLabel(order)) },
                                onClick = {
                                    viewModel.setSortOrder(order)
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    RadioButton(
                                        selected = viewModel.sortOrder.value == order,
                                        onClick = { viewModel.setSortOrder(order) }
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            // 搜索栏
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

            // 分类筛选
            if (categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Dimens.lg, vertical = Dimens.xs),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                ) {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { viewModel.filterByCategory(null) },
                        label = { Text("全部", fontSize = 13.sp) }
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { viewModel.filterByCategory(cat.id) },
                            label = { Text(cat.name, fontSize = 13.sp) },
                            leadingIcon = {
                                Surface(
                                    modifier = Modifier.size(10.dp),
                                    shape = CircleShape,
                                    color = Color(cat.color)
                                ) { }
                            }
                        )
                    }
                }
            }

            // 列表 / 空状态（下拉刷新）
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing),
                onRefresh = { viewModel.refresh() }
            ) {
                if (todos.isEmpty()) {
                    EmptyState(
                        iconProvider = iconProvider,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = Dimens.xxxl)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = Dimens.sm,
                            bottom = 96.dp
                        )
                    ) {
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
                            AnimatedVisibility(
                                visible = true,
                                enter = slideInVertically(
                                    initialOffsetY = { it / 4 }
                                ) + fadeIn(animationSpec = tween(300)),
                                modifier = Modifier.animateItemPlacement()
                            ) {
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        when (value) {
                                            DismissValue.DismissedToEnd -> {
                                                viewModel.toggleComplete(todo.id)
                                                false
                                            }
                                            DismissValue.DismissedToStart -> {
                                                viewModel.deleteTodo(todo.id)
                                                false
                                            }
                                            else -> false
                                        }
                                    }
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        DismissBackground(dismissState, iconProvider)
                                    },
                                    content = {
                                        TodoItemCard(
                                            item = todo,
                                            onToggle = { viewModel.toggleComplete(todo.id) },
                                            onClick = { onNavigateToEdit(todo.id) },
                                            onDelete = { viewModel.deleteTodo(todo.id) },
                                            onPin = { viewModel.togglePin(todo.id) },
                                            iconProvider = iconProvider
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 删除撤销 Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp)
        )
    }
}

/**
 * 滑动删除/完成时的背景提示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissBackground(
    state: SwipeToDismissBoxState,
    iconProvider: IconProvider
) {
    val direction = state.dismissDirection
    val color = when (direction) {
        DismissDirection.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
        DismissDirection.EndToStart -> MaterialTheme.colorScheme.errorContainer
        else -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.lg, vertical = Dimens.sm)
            .clip(RoundedCornerShape(Dimens.cardCornerRadius))
            .background(color),
        contentAlignment = when (direction) {
            DismissDirection.StartToEnd -> Alignment.CenterStart
            DismissDirection.EndToStart -> Alignment.CenterEnd
            else -> Alignment.Center
        }
    ) {
        if (direction != null) {
            Icon(
                imageVector = if (direction == DismissDirection.StartToEnd)
                    iconProvider.checkCircle else iconProvider.delete,
                contentDescription = null,
                tint = if (direction == DismissDirection.StartToEnd)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

private fun sortLabel(order: SortOrder): String = when (order) {
    SortOrder.CREATED_DESC -> "最近创建"
    SortOrder.CREATED_ASC -> "最早创建"
    SortOrder.PRIORITY_DESC -> "优先级高→低"
    SortOrder.DUE_DATE_ASC -> "截止时间近→远"
    SortOrder.ALPHABETICAL -> "按标题"
}
