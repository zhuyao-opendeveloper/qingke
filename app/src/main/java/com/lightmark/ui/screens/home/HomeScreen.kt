package com.lightmark.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.lightmark.icons.IconProvider
import com.lightmark.icons.LightMarkIcon
import com.lightmark.domain.model.Priority
import com.lightmark.ui.components.EmptyState
import com.lightmark.ui.components.LightMarkSearchBar
import com.lightmark.ui.components.QuickAddDialog
import com.lightmark.ui.components.TodoItemCard
import com.lightmark.ui.components.priorityLabelOf
import com.lightmark.ui.theme.Dimens
import androidx.compose.foundation.gestures.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

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
    onNavigateToAi: () -> Unit,
    onNavigateToFocus: (String) -> Unit = {},
    onNavigateToSmartLists: () -> Unit = {}
) {
    val todos by viewModel.todos.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pendingUndo by viewModel.pendingUndo.collectAsState()
    val conflictingIds by viewModel.conflictingIds.collectAsState()
    val todayEstimatedMinutes by viewModel.todayEstimatedMinutes.collectAsState()
    val announcementDismissed by viewModel.announcementDismissed.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val subtaskCounts by viewModel.subtaskCounts.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val quickFilter by viewModel.quickFilter.collectAsState()
    val groupMode by viewModel.groupMode.collectAsState()
    val groupLabels by viewModel.groupLabels.collectAsState()
    val encouragement by viewModel.encouragement.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    val listDensity by viewModel.listDensity.collectAsState()
    val smartLists by viewModel.smartLists.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()

    // 手动排序拖拽状态（#32）
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val manualOrderIds = remember(sortOrder) { mutableStateOf<List<String>?>(null) }
    val displayList = if (sortOrder == SortOrder.MANUAL) {
        manualOrderIds.value?.let { order ->
            todos.sortedBy { order.indexOf(it.id).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE }
        } ?: todos
    } else todos
    val biometricLockEnabled by viewModel.biometricLockEnabled.collectAsState()
    val showPrivate by viewModel.showPrivate.collectAsState()
    val iconProvider = viewModel.currentIconProvider

    // 触感反馈（#116）
    val hapticFeedback = LocalHapticFeedback.current
    val buzz: () -> Unit = {
        if (hapticEnabled) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var showSortMenu by remember { mutableStateOf(false) }
    var showBulkPriorityMenu by remember { mutableStateOf(false) }
    var showQuickAdd by remember { mutableStateOf(false) }

    // 全功能撤销提示（#124）
    LaunchedEffect(pendingUndo) {
        val action = pendingUndo
        if (action != null) {
            val result = snackbarHostState.showSnackbar(
                message = action.message,
                actionLabel = "撤销",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoLastAction()
            } else {
                viewModel.clearPendingUndo()
            }
        }
    }

    // 完成鼓励语（#123）
    LaunchedEffect(encouragement) {
        val text = encouragement
        if (text != null) {
            snackbarHostState.showSnackbar(
                message = text,
                duration = SnackbarDuration.Short
            )
            viewModel.consumeEncouragement()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 多选操作栏（#19）
            if (selectionMode) {
                TopAppBar(
                    title = { Text("已选 ${selectedIds.size} 项", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSelection() }) {
                            Icon(Icons.Filled.Close, contentDescription = "退出多选")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAllVisible() }) {
                            Icon(Icons.Filled.SelectAll, contentDescription = "全选")
                        }
                        if (viewMode == HomeViewMode.TRASH) {
                            IconButton(onClick = { viewModel.bulkRestore() }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "批量恢复")
                            }
                        } else {
                            IconButton(onClick = { viewModel.bulkComplete() }) {
                                Icon(Icons.Filled.DoneAll, contentDescription = "批量完成")
                            }
                            IconButton(onClick = { viewModel.bulkArchive() }) {
                                Icon(Icons.Filled.Bookmark, contentDescription = "批量归档")
                            }
                            IconButton(onClick = { showBulkPriorityMenu = true }) {
                                Icon(Icons.Filled.Flag, contentDescription = "批量设置优先级")
                            }
                        }
                        IconButton(onClick = { viewModel.bulkDelete() }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "批量删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        DropdownMenu(
                            expanded = showBulkPriorityMenu,
                            onDismissRequest = { showBulkPriorityMenu = false }
                        ) {
                            Priority.entries.reversed().forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("优先级 → ${priorityLabelOf(p)}") },
                                    onClick = {
                                        showBulkPriorityMenu = false
                                        viewModel.bulkSetPriority(p)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                )
            } else {
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
                    IconButton(onClick = { showQuickAdd = true }) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = "闪电添加",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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
                        Divider()
                        Text(
                            text = "分组显示",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = Dimens.lg, top = Dimens.sm, bottom = Dimens.xs)
                        )
                        GroupMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                enabled = sortOrder != SortOrder.MANUAL,
                                text = { Text(mode.label) },
                                onClick = {
                                    viewModel.setGroupMode(mode)
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    RadioButton(
                                        selected = groupMode == mode,
                                        onClick = { viewModel.setGroupMode(mode) }
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
            }

            // 视图切换：待办 / 已归档 / 回收站（#25）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.lg, vertical = Dimens.xs),
                horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                HomeViewMode.entries.forEach { mode ->
                    FilterChip(
                        selected = viewMode == mode,
                        onClick = { viewModel.setViewMode(mode) },
                        label = {
                            Text(
                                when (mode) {
                                    HomeViewMode.ACTIVE -> "待办"
                                    HomeViewMode.ARCHIVED -> "已归档"
                                    HomeViewMode.TRASH -> "回收站"
                                }, fontSize = 13.sp
                            )
                        }
                    )
                }
            }

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
                    val visibleCategories = if (biometricLockEnabled && !showPrivate)
                        categories.filter { !it.isPrivate } else categories
                    visibleCategories.forEach { cat ->
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

            // 快速筛选（#59 智能清单）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.lg, vertical = Dimens.xs),
                horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                QuickFilter.entries.forEach { f ->
                    FilterChip(
                        selected = quickFilter == f,
                        onClick = { viewModel.setQuickFilter(f) },
                        label = { Text(f.label, fontSize = 13.sp) }
                    )
                }
                if (biometricLockEnabled) {
                    FilterChip(
                        selected = showPrivate,
                        onClick = { viewModel.setShowPrivate(!showPrivate) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(if (showPrivate) "隐藏私密" else "显示私密", fontSize = 13.sp) }
                    )
                }
            }

            // 自定义智能清单（#28）：已保存的命名筛选
            if (smartLists.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Dimens.lg, vertical = Dimens.xs),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                ) {
                    smartLists.forEach { sl ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.applySmartList(sl) },
                            label = { Text("${sl.emoji} ${sl.name}", fontSize = 13.sp) }
                        )
                    }
                    AssistChip(
                        onClick = onNavigateToSmartLists,
                        label = { Text("管理", fontSize = 13.sp) }
                    )
                }
            }

            // 停更公告横幅（#v3.0.0，可关闭）
            if (!announcementDismissed) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.lg, vertical = Dimens.xs),
                    shape = RoundedCornerShape(Dimens.cardCornerRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📢 暂时停更公告",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "作者正在备战高考，暂无法更新。大一开学后将继续开发，感谢陪伴 ❤",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        IconButton(onClick = { viewModel.dismissAnnouncement() }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "关闭公告",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
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
                    val baseDensity = LocalDensity.current
                    val densityFactor = when (listDensity) {
                        "COMPACT" -> 0.85f
                        "DETAILED" -> 1.15f
                        else -> 1.0f
                    }
                    CompositionLocalProvider(
                        LocalDensity provides Density(baseDensity.density * densityFactor, fontScale)
                    ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = Dimens.sm,
                            bottom = 96.dp
                        )
                    ) {
                        if (viewMode == HomeViewMode.ACTIVE && conflictingIds.isNotEmpty()) {
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Dimens.lg, vertical = Dimens.xs),
                                    shape = RoundedCornerShape(Dimens.cardCornerRadius),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = "⚠ 有 ${conflictingIds.size} 个任务的时间安排相互冲突，请检查开始/截止时间（#88）",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(Dimens.md)
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = when (viewMode) {
                                    HomeViewMode.ACTIVE -> "待办事项 (${todos.size})"
                                    HomeViewMode.ARCHIVED -> "已归档 (${todos.size})"
                                    HomeViewMode.TRASH -> "回收站 (${todos.size})"
                                },
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    start = Dimens.lg,
                                    bottom = Dimens.sm
                                )
                            )
                        }

                        items(
                            items = displayList,
                            key = { it.id }
                        ) { todo ->
                            Column(
                                modifier = if (sortOrder == SortOrder.MANUAL) {
                                    val itemHPx = with(LocalDensity.current) { 76.dp.toPx() }
                                    Modifier
                                        .offset { IntOffset(0, if (draggingId == todo.id) dragOffsetY.roundToInt() else 0) }
                                        .pointerInput(todo.id, sortOrder) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggingId = todo.id
                                                    dragOffsetY = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    dragOffsetY += dragAmount.y
                                                    val itemH = itemHPx
                                                    val base = manualOrderIds.value ?: todos.map { it.id }
                                                    val from = base.indexOf(todo.id)
                                                    if (from < 0) return@detectDragGesturesAfterLongPress
                                                    val maxIdx = (displayList.size - 1).coerceAtLeast(0)
                                                    val target = (from + (dragOffsetY / itemH).toInt())
                                                        .coerceIn(0, maxIdx)
                                                    if (target != from) {
                                                        val cur = base.toMutableList()
                                                        cur.add(target, cur.removeAt(from))
                                                        manualOrderIds.value = cur
                                                        dragOffsetY -= (target - from) * itemH
                                                    }
                                                    change.consume()
                                                },
                                                onDragEnd = {
                                                    viewModel.applyManualOrder(manualOrderIds.value ?: displayList.map { it.id })
                                                    draggingId = null
                                                    dragOffsetY = 0f
                                                },
                                                onDragCancel = {
                                                    draggingId = null
                                                    dragOffsetY = 0f
                                                }
                                            )
                                        }
                                } else Modifier
                            ) {
                            groupLabels[todo.id]?.let { label ->
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(
                                        start = Dimens.lg,
                                        top = Dimens.md,
                                        bottom = Dimens.xs
                                    )
                                )
                            }
                            AnimatedVisibility(
                                visible = true,
                                enter = slideInVertically(
                                    initialOffsetY = { it / 4 }
                                ) + fadeIn(animationSpec = tween(300)),
                                modifier = Modifier
                            ) {
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        when (value) {
                                            SwipeToDismissBoxValue.StartToEnd -> {
                                                buzz()
                                                viewModel.toggleComplete(todo.id)
                                                false
                                            }
                                            SwipeToDismissBoxValue.EndToStart -> {
                                                if (viewMode == HomeViewMode.TRASH) viewModel.hardDelete(todo.id)
                                                else viewModel.deleteTodo(todo.id)
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
                                    onToggle = {
                                        buzz()
                                        viewModel.toggleComplete(todo.id)
                                    },
                                    onClick = {
                                        if (selectionMode) viewModel.toggleSelection(todo.id)
                                        else onNavigateToEdit(todo.id)
                                    },
                                    onLongClick = {
                                        buzz()
                                        viewModel.startSelection(todo.id)
                                    },
                                    selectionMode = selectionMode,
                                    selected = selectedIds.contains(todo.id),
                                    onDelete = {
                                        if (viewMode == HomeViewMode.TRASH) viewModel.hardDelete(todo.id)
                                        else viewModel.deleteTodo(todo.id)
                                    },
                                    onPin = { viewModel.togglePin(todo.id) },
                                    subtaskCount = subtaskCounts[todo.id] ?: 0,
                                    showArchive = viewMode != HomeViewMode.TRASH,
                                    showRestore = viewMode == HomeViewMode.TRASH,
                                    onArchive = {
                                        if (viewMode == HomeViewMode.ARCHIVED) viewModel.unarchiveTodo(todo.id)
                                        else viewModel.archiveTodo(todo.id)
                                    },
                                    onRestore = { viewModel.restoreTodo(todo.id) },
                                    onFocus = { onNavigateToFocus(todo.id) },
                                    iconProvider = iconProvider,
                                    isConflict = conflictingIds.contains(todo.id)
                                )
                                    }
                                )
                            }
                            }
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

        // 闪电添加（自然语言 / 语音 / 剪贴板）
        if (showQuickAdd) {
            QuickAddDialog(
                onDismiss = { showQuickAdd = false },
                onConfirm = { raw -> viewModel.quickAdd(raw) }
            )
        }
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
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
        else -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.lg, vertical = Dimens.sm)
            .clip(RoundedCornerShape(Dimens.cardCornerRadius))
            .background(color),
        contentAlignment = when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
            else -> Alignment.Center
        }
    ) {
        if (direction != SwipeToDismissBoxValue.Settled) {
            Icon(
                imageVector = if (direction == SwipeToDismissBoxValue.StartToEnd)
                    iconProvider.checkCircle else iconProvider.delete,
                contentDescription = null,
                tint = if (direction == SwipeToDismissBoxValue.StartToEnd)
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
    SortOrder.MANUAL -> "手动排序"
}
