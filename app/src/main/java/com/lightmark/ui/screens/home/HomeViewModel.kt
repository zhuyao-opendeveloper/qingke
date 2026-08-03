package com.lightmark.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.auth.AuthManager
import com.lightmark.data.local.dao.CategoryDao
import com.lightmark.data.local.dao.TodoDao
import com.lightmark.data.local.entity.TodoEntity
import com.lightmark.data.repository.TodoRepository
import com.lightmark.data.settings.SettingsRepository
import com.lightmark.domain.model.Category
import com.lightmark.domain.model.IconPack
import com.lightmark.domain.model.Priority
import com.lightmark.domain.model.TodoItem
import com.lightmark.icons.IconProvider
import com.lightmark.icons.getIconProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
 * - 下拉刷新（从 GitHub 同步）
 * - 图标库跟随设置
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val todoDao: TodoDao,
    private val categoryDao: CategoryDao,
    private val authManager: AuthManager,
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

    init {
        viewModelScope.launch {
            settingsRepository.settings.map { it.iconPack }.collect { pack ->
                _iconPack.value = IconPack.fromString(pack)
            }
        }
    }

    val categories: StateFlow<List<Category>> = categoryDao.getAllCategories()
        .map { list -> list.map { entity -> entity.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todos: StateFlow<List<TodoItem>> = combine(
        todoDao.getAllTodos(),
        _searchQuery,
        _selectedCategoryId,
        _sortOrder
    ) { allTodos, query, categoryId, sort ->
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

    fun toggleComplete(todoId: String) {
        viewModelScope.launch {
            val todo = todoDao.getTodoById(todoId) ?: return@launch
            todoDao.updateTodo(
                todo.copy(
                    isCompleted = !todo.isCompleted,
                    completedAt = if (todo.isCompleted) null else System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
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
            todoDao.deleteTodoById(todoId)
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

    /** 下拉刷新：已登录则从 GitHub 拉取最新数据 */
    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            runCatching {
                val token = authManager.getToken()
                val login = authManager.currentUser.value?.login
                if (!token.isNullOrBlank() && !login.isNullOrBlank()) {
                    repository.syncFromGitHub(token, login)
                }
            }
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
