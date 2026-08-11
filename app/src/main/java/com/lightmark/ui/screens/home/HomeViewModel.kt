package com.lightmark.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.local.dao.CategoryDao
import com.lightmark.data.local.dao.TodoDao
import com.lightmark.data.local.entity.CategoryEntity
import com.lightmark.data.local.entity.TodoEntity
import com.lightmark.data.repository.TodoRepository
import com.lightmark.data.settings.SettingsRepository
import com.lightmark.domain.model.Category
import com.lightmark.domain.model.IconPack
import com.lightmark.domain.model.Priority
import com.lightmark.domain.model.Recurrence
import com.lightmark.domain.model.SmartList
import com.lightmark.domain.model.TodoItem
import com.lightmark.icons.IconProvider
import com.lightmark.icons.getIconProvider
import com.lightmark.util.Encouragement
import com.lightmark.util.NaturalLanguageParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主页 ViewModel
 *
 * 职责：
 * - 提供待办列表（搜索 / 分类 / 排序，置顶优先）
 * - 完成 / 删除（带撤销）/ 置顶
 * - 分类列表（用于筛选）
 * - 下拉刷新（本地回收站过期清理）
 * - 图标库跟随设置
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val todoDao: TodoDao,
    private val categoryDao: CategoryDao,
    private val repository: TodoRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.CREATED_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _iconPack = MutableStateFlow(IconPack.MATERIAL)
    val currentIconProvider: IconProvider
        get() = getIconProvider(_iconPack.value)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _lastDeleted = MutableStateFlow<TodoItem?>(null)
    val lastDeleted: StateFlow<TodoItem?> = _lastDeleted.asStateFlow()

    /** 当前列表视图：待办 / 已归档 / 回收站 */
    private val _viewMode = MutableStateFlow(HomeViewMode.ACTIVE)
    val viewMode: StateFlow<HomeViewMode> = _viewMode.asStateFlow()

    /** 根据视图模式选择底层数据源 */
    private val baseTodos: kotlinx.coroutines.flow.Flow<List<TodoEntity>> =
        _viewMode.flatMapLatest { mode ->
            when (mode) {
                HomeViewMode.ACTIVE -> todoDao.getActiveTodos()
                HomeViewMode.ARCHIVED -> todoDao.getArchivedTodos()
                HomeViewMode.TRASH -> todoDao.getTrashTodos()
            }
        }

    /** 完成任务后的随机鼓励语（#123） */
    private val _encouragement = MutableStateFlow<String?>(null)
    val encouragement: StateFlow<String?> = _encouragement.asStateFlow()

    fun consumeEncouragement() {
        _encouragement.value = null
    }

    /** 触感反馈开关（#116） */
    private val _hapticEnabled = MutableStateFlow(true)
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    /** 列表密度（#51）：COMPACT / COZY / DETAILED */
    private val _listDensity = MutableStateFlow("COZY")
    val listDensity: StateFlow<String> = _listDensity.asStateFlow()

    init {
        viewModelScope.launch {
            cleanupTrash()
        }
        viewModelScope.launch {
            settingsRepository.settings.map { it.iconPack }.collect { pack ->
                _iconPack.value = IconPack.fromString(pack)
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.map { it.hapticEnabled }.collect { enabled ->
                _hapticEnabled.value = enabled
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.map { it.listDensity }.collect { density ->
                _listDensity.value = density
            }
        }
    }

    /** 回收站按保留天数自动清理（#101），0 表示永久保留 */
    private suspend fun cleanupTrash() {
        val days = runCatching { settingsRepository.currentSettings().trashRetentionDays }
            .getOrDefault(30)
        if (days <= 0) return
        val cutoff = System.currentTimeMillis() - days * 86_400_000L
        todoDao.getAllTodosList().forEach { entity ->
            val deletedAt = entity.deletedAt
            if (entity.isDeleted && deletedAt != null && deletedAt < cutoff) {
                todoDao.deleteTodoById(entity.id)
            }
        }
    }

    val categories: StateFlow<List<Category>> = categoryDao.getAllCategories()
        .map { list -> list.map { entity -> entity.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 每个父任务的直接子任务数量（用于卡片徽标，#3） */
    val subtaskCounts: StateFlow<Map<String, Int>> = todoDao.getAllTodos()
        .map { list ->
            list.filter { it.parentId != null && !it.isDeleted }
                .groupingBy { it.parentId!! }.eachCount()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** 快速筛选（#59 智能清单） */
    private val _quickFilter = MutableStateFlow(QuickFilter.ALL)
    val quickFilter: StateFlow<QuickFilter> = _quickFilter.asStateFlow()

    /** 自定义智能清单（#28）：已保存的清单 */
    val smartLists: StateFlow<List<SmartList>> = repository.getSmartLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 分组显示（#58） */
    private val _groupMode = MutableStateFlow(GroupMode.NONE)
    val groupMode: StateFlow<GroupMode> = _groupMode.asStateFlow()

    private val filteredTodos: StateFlow<List<TodoItem>> = combine(
        baseTodos,
        _searchQuery,
        _selectedCategoryId,
        _sortOrder,
        _quickFilter
    ) { allTodos, query, categoryId, sort, quick ->
        var filtered = allTodos.map { it.toDomain() }

        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
            }
        }

        if (categoryId != null) {
            filtered = filtered.filter { it.categoryId == categoryId }
        }

        filtered = applyQuickFilter(filtered, quick)

        val comparator = when (sort) {
            SortOrder.CREATED_DESC -> compareByDescending<TodoItem> { it.createdAt }
            SortOrder.CREATED_ASC -> compareBy<TodoItem> { it.createdAt }
            SortOrder.PRIORITY_DESC -> compareByDescending { it.priority.ordinal }
            SortOrder.DUE_DATE_ASC -> compareBy { it.dueDate ?: Long.MAX_VALUE }
            SortOrder.ALPHABETICAL -> compareBy { it.title }
        }

        // 置顶优先，组内保持所选排序（稳定排序）
        filtered.sortedWith(compareByDescending<TodoItem> { it.isPinned }.then(comparator))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 分组后的顺序 + 每个分组首项对应的标题（用于列表内插入分组头） */
    private val groupedResult: StateFlow<Pair<List<TodoItem>, Map<String, String>>> =
        combine(filteredTodos, _groupMode, categories) { list, mode, cats ->
            buildGrouping(list, mode, cats)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList<TodoItem>() to emptyMap<String, String>()
        )

    val todos: StateFlow<List<TodoItem>> = groupedResult
        .map { it.first }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** todoId -> 分组标题（仅每组第一项有值） */
    val groupLabels: StateFlow<Map<String, String>> = groupedResult
        .map { it.second }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setQuickFilter(filter: QuickFilter) {
        _quickFilter.value = filter
        exitSelection()
    }

    /** 套用自定义智能清单（#28）：一次性设置筛选 / 分类 / 关键词 / 排序 */
    fun applySmartList(list: SmartList) {
        _quickFilter.value = list.quickFilter
            ?.let { runCatching { QuickFilter.valueOf(it) }.getOrNull() } ?: QuickFilter.ALL
        _selectedCategoryId.value = list.categoryId
        _searchQuery.value = list.query ?: ""
        _sortOrder.value = list.sortOrder
            ?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() } ?: SortOrder.CREATED_DESC
        _isSearching.value = !list.query.isNullOrBlank()
        exitSelection()
    }

    fun setGroupMode(mode: GroupMode) {
        _groupMode.value = mode
    }

    private fun applyQuickFilter(list: List<TodoItem>, filter: QuickFilter): List<TodoItem> {
        if (filter == QuickFilter.ALL) return list
        val zone = java.time.ZoneId.systemDefault()
        val today = java.time.LocalDate.now()
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val tomorrowStart = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val weekEnd = today.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()

        return when (filter) {
            QuickFilter.ALL -> list
            QuickFilter.TODAY -> list.filter { it.dueDate != null && it.dueDate!! in todayStart until tomorrowStart }
            QuickFilter.WEEK -> list.filter { it.dueDate != null && it.dueDate!! in todayStart until weekEnd }
            QuickFilter.OVERDUE -> list.filter { !it.isCompleted && it.dueDate != null && it.dueDate!! < now }
            QuickFilter.NO_DATE -> list.filter { it.dueDate == null }
            QuickFilter.PINNED -> list.filter { it.isPinned }
            QuickFilter.IMPORTANT -> list.filter { it.priority.ordinal >= Priority.HIGH.ordinal }
            QuickFilter.COMPLETED -> list.filter { it.isCompleted }
            QuickFilter.UNFINISHED -> list.filter { !it.isCompleted }
        }
    }

    private fun buildGrouping(
        list: List<TodoItem>,
        mode: GroupMode,
        cats: List<Category>
    ): Pair<List<TodoItem>, Map<String, String>> {
        if (mode == GroupMode.NONE || list.isEmpty()) return list to emptyMap()

        val catName = cats.associate { it.id to it.name }
        val keyOf: (TodoItem) -> String = { item -> groupTitleOf(item, mode, catName) }

        val names = list.map(keyOf).distinct()
        val sortedNames = when (mode) {
            GroupMode.CATEGORY -> names.sortedWith(compareBy({ it == UNCATEGORIZED }, { it }))
            GroupMode.PRIORITY -> names.sortedBy { PRIORITY_ORDER.indexOf(it).takeIf { i -> i >= 0 } ?: 99 }
            GroupMode.DUE -> names.sortedBy { DUE_ORDER.indexOf(it).takeIf { i -> i >= 0 } ?: 99 }
            GroupMode.NONE -> names
        }
        val rank = sortedNames.withIndex().associate { (i, n) -> n to i }

        val ordered = list.sortedBy { rank[keyOf(it)] ?: 99 }
        val labels = LinkedHashMap<String, String>()
        var last: String? = null
        ordered.forEach { item ->
            val title = keyOf(item)
            if (title != last) {
                labels[item.id] = title
                last = title
            }
        }
        return ordered to labels
    }

    private fun groupTitleOf(item: TodoItem, mode: GroupMode, catName: Map<String, String>): String =
        when (mode) {
            GroupMode.NONE -> ""
            GroupMode.CATEGORY -> item.categoryId?.let { catName[it] } ?: UNCATEGORIZED
            GroupMode.PRIORITY -> when (item.priority) {
                Priority.IDLE -> "空闲"
                Priority.LOW -> "低"
                Priority.MEDIUM -> "中"
                Priority.HIGH -> "高"
                Priority.URGENT -> "紧急"
            }
            GroupMode.DUE -> dueBucketOf(item.dueDate)
        }

    private fun dueBucketOf(due: Long?): String {
        if (due == null) return "无日期"
        val zone = java.time.ZoneId.systemDefault()
        val today = java.time.LocalDate.now()
        val date = java.time.Instant.ofEpochMilli(due).atZone(zone).toLocalDate()
        return when {
            date.isBefore(today) -> "已逾期"
            date == today -> "今天"
            date == today.plusDays(1) -> "明天"
            date.isBefore(today.plusDays(8)) -> "本周内"
            else -> "更晚"
        }
    }

    fun toggleComplete(todoId: String) {
        viewModelScope.launch {
            val todo = todoDao.getTodoById(todoId) ?: return@launch
            val willComplete = !todo.isCompleted
            todoDao.updateTodo(
                todo.copy(
                    isCompleted = willComplete,
                    completedAt = if (willComplete) System.currentTimeMillis() else null,
                    updatedAt = System.currentTimeMillis()
                )
            )
            // 重复任务：完成后自动生成下一次（#13）
            if (willComplete && !todo.recurrenceRule.isNullOrBlank() && todo.recurrenceRule != Recurrence.NONE) {
                val base = todo.dueDate ?: System.currentTimeMillis()
                val next = Recurrence.nextOccurrence(todo.recurrenceRule, base)
                if (next != null) {
                    todoDao.insertTodo(
                        todo.copy(
                            id = TodoItem.generateId(),
                            isCompleted = false,
                            completedAt = null,
                            dueDate = next,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            // 完成反馈：随机鼓励语（#123）
            if (willComplete) {
                val enabled = runCatching {
                    settingsRepository.currentSettings().encouragementEnabled
                }.getOrDefault(true)
                if (enabled) {
                    val startOfDay = java.time.LocalDate.now()
                        .atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    val doneToday = todoDao.getAllTodosList().count {
                        it.isCompleted && (it.completedAt ?: 0L) >= startOfDay
                    }
                    _encouragement.value = Encouragement.pick(
                        Priority.fromString(todo.priority),
                        doneToday
                    )
                }
            }
        }
    }

    fun togglePin(todoId: String) {
        viewModelScope.launch {
            val todo = todoDao.getTodoById(todoId) ?: return@launch
            todoDao.updateTodo(
                todo.copy(isPinned = !todo.isPinned, updatedAt = System.currentTimeMillis())
            )
        }
    }

    fun deleteTodo(todoId: String) {
        viewModelScope.launch {
            val entity = todoDao.getTodoById(todoId) ?: return@launch
            _lastDeleted.value = entity.toDomain()
            // 软删除：移入回收站（#25）
            todoDao.updateTodo(
                entity.copy(
                    isDeleted = true,
                    deletedAt = System.currentTimeMillis(),
                    isArchived = false,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /** 永久删除（仅回收站视图使用） */
    fun hardDelete(todoId: String) {
        viewModelScope.launch {
            todoDao.deleteTodoById(todoId)
        }
    }

    /** 归档任务（#25） */
    fun archiveTodo(todoId: String) {
        viewModelScope.launch {
            val entity = todoDao.getTodoById(todoId) ?: return@launch
            todoDao.updateTodo(
                entity.copy(isArchived = true, isDeleted = false, updatedAt = System.currentTimeMillis())
            )
        }
    }

    /** 取消归档 */
    fun unarchiveTodo(todoId: String) {
        viewModelScope.launch {
            val entity = todoDao.getTodoById(todoId) ?: return@launch
            todoDao.updateTodo(entity.copy(isArchived = false, updatedAt = System.currentTimeMillis()))
        }
    }

    /** 从回收站恢复 */
    fun restoreTodo(todoId: String) {
        viewModelScope.launch {
            val entity = todoDao.getTodoById(todoId) ?: return@launch
            todoDao.updateTodo(
                entity.copy(isDeleted = false, deletedAt = null, updatedAt = System.currentTimeMillis())
            )
        }
    }

    fun setViewMode(mode: HomeViewMode) {
        _viewMode.value = mode
        exitSelection()
    }

    // ---------------- 批量多选（#19） ----------------

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    fun startSelection(todoId: String) {
        _selectionMode.value = true
        _selectedIds.value = setOf(todoId)
    }

    fun toggleSelection(todoId: String) {
        val current = _selectedIds.value
        _selectedIds.value = if (current.contains(todoId)) current - todoId else current + todoId
        if (_selectedIds.value.isEmpty()) _selectionMode.value = false
    }

    fun selectAllVisible() {
        _selectedIds.value = todos.value.map { it.id }.toSet()
        _selectionMode.value = _selectedIds.value.isNotEmpty()
    }

    fun exitSelection() {
        _selectionMode.value = false
        _selectedIds.value = emptySet()
    }

    private fun eachSelected(block: suspend (TodoEntity) -> Unit) {
        val ids = _selectedIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { id ->
                todoDao.getTodoById(id)?.let { block(it) }
            }
            exitSelection()
        }
    }

    fun bulkComplete() = eachSelected { entity ->
        todoDao.updateTodo(
            entity.copy(
                isCompleted = true,
                completedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun bulkUncomplete() = eachSelected { entity ->
        todoDao.updateTodo(
            entity.copy(isCompleted = false, completedAt = null, updatedAt = System.currentTimeMillis())
        )
    }

    fun bulkDelete() = eachSelected { entity ->
        if (_viewMode.value == HomeViewMode.TRASH) {
            todoDao.deleteTodoById(entity.id)
        } else {
            todoDao.updateTodo(
                entity.copy(
                    isDeleted = true,
                    deletedAt = System.currentTimeMillis(),
                    isArchived = false,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun bulkArchive() = eachSelected { entity ->
        todoDao.updateTodo(
            entity.copy(isArchived = true, isDeleted = false, updatedAt = System.currentTimeMillis())
        )
    }

    fun bulkRestore() = eachSelected { entity ->
        todoDao.updateTodo(
            entity.copy(
                isDeleted = false,
                deletedAt = null,
                isArchived = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun bulkSetPriority(priority: Priority) = eachSelected { entity ->
        todoDao.updateTodo(entity.copy(priority = priority.name, updatedAt = System.currentTimeMillis()))
    }

    fun bulkSetCategory(categoryId: String?) = eachSelected { entity ->
        todoDao.updateTodo(entity.copy(categoryId = categoryId, updatedAt = System.currentTimeMillis()))
    }

    /**
     * 自然语言快速添加（#1 闪电添加 / #2 自然语言识别 / #21 语音 / #22 剪贴板）
     *
     * 例："明天下午3点交报告 @工作 #紧急 每周"
     * 解析出的分类若不存在会自动创建。
     */
    fun quickAdd(raw: String) {
        if (raw.isBlank()) return
        viewModelScope.launch {
            val parsed = NaturalLanguageParser.parse(raw)
            val catId = parsed.categoryName?.let { name ->
                val existing = categoryDao.getAllCategoriesList()
                    .firstOrNull { it.name.equals(name, ignoreCase = true) }
                existing?.id ?: run {
                    val newCat = CategoryEntity(
                        id = "cat_${System.currentTimeMillis()}_${(0..9999).random()}",
                        name = name,
                        color = 0xFF6750A4L,
                        icon = "Label",
                        createdAt = System.currentTimeMillis()
                    )
                    categoryDao.insertCategory(newCat)
                    newCat.id
                }
            }
            val item = TodoItem(
                title = parsed.title,
                priority = parsed.priority ?: Priority.MEDIUM,
                categoryId = catId,
                tags = parsed.tags,
                dueDate = parsed.dueDate,
                recurrenceRule = parsed.recurrenceRule
            )
            todoDao.insertTodo(TodoEntity.fromDomain(item))
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            _lastDeleted.value?.let { item ->
                todoDao.insertTodo(TodoEntity.fromDomain(item))
            }
            _lastDeleted.value = null
        }
    }

    fun clearLastDeleted() {
        _lastDeleted.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch() {
        _isSearching.value = !_isSearching.value
        if (!_isSearching.value) {
            _searchQuery.value = ""
        }
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun filterByCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    /**
     * 下拉刷新：本地数据由 Room Flow 自动推送，这里只做一次过期清理并给出反馈动画。
     */
    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            runCatching { cleanupTrash() }
            _isRefreshing.value = false
        }
    }
}

enum class SortOrder {
    CREATED_DESC,
    CREATED_ASC,
    PRIORITY_DESC,
    DUE_DATE_ASC,
    ALPHABETICAL
}

/** 快速筛选（智能清单，#59） */
enum class QuickFilter(val label: String) {
    ALL("全部"),
    TODAY("今天"),
    WEEK("本周"),
    OVERDUE("已逾期"),
    NO_DATE("无日期"),
    PINNED("已置顶"),
    IMPORTANT("重要"),
    UNFINISHED("未完成"),
    COMPLETED("已完成")
}

/** 分组方式（#58） */
enum class GroupMode(val label: String) {
    NONE("不分组"),
    CATEGORY("按分类"),
    PRIORITY("按优先级"),
    DUE("按到期")
}

private const val UNCATEGORIZED = "未分类"
private val PRIORITY_ORDER = listOf("紧急", "高", "中", "低", "空闲")
private val DUE_ORDER = listOf("已逾期", "今天", "明天", "本周内", "更晚", "无日期")

/** 首页列表视图模式（#25 归档/回收站） */
enum class HomeViewMode {
    ACTIVE,     // 待办（活跃）
    ARCHIVED,   // 已归档
    TRASH       // 回收站
}
