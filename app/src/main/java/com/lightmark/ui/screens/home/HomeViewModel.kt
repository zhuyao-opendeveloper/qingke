package com.lightmark.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.local.dao.TodoDao
import com.lightmark.domain.model.*
import com.lightmark.icons.IconProvider
import com.lightmark.icons.getIconProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主页 ViewModel
 *
 * 管理待办列表、搜索、筛选、排序等状�? */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val todoDao: TodoDao
) : ViewModel() {

    // 搜索关键�?    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 当前筛选分�?    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    // 当前排列方式
    private val _sortOrder = MutableStateFlow(SortOrder.CREATED_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    // 当前界面模式（查�?搜索�?    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // 当前图标库（从设置读取）
    private val _iconPack = MutableStateFlow(IconPack.MATERIAL)
    val currentIconProvider: IconProvider
        get() = getIconProvider(_iconPack.value)

    // 待办列表（来自本�?Room 数据库）
    val todos: StateFlow<List<TodoItem>> = combine(
        todoDao.getAllTodos(),
        _searchQuery,
        _selectedCategoryId,
        _sortOrder
    ) { allTodos, query, categoryId, sort ->
        var filtered = allTodos.map { it.toDomain() }

        // 搜索过滤
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
            }
        }

        // 分类过滤
        if (categoryId != null) {
            filtered = filtered.filter { it.categoryId == categoryId }
        }

        // 排序
        when (sort) {
            SortOrder.CREATED_DESC -> filtered.sortedByDescending { it.createdAt }
            SortOrder.CREATED_ASC -> filtered.sortedBy { it.createdAt }
            SortOrder.PRIORITY_DESC -> filtered.sortedByDescending { it.priority.ordinal }
            SortOrder.DUE_DATE_ASC -> filtered.sortedBy { it.dueDate ?: Long.MAX_VALUE }
            SortOrder.ALPHABETICAL -> filtered.sortedBy { it.title }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 切换完成状�?*/
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

    /** 删除待办 */
    fun deleteTodo(todoId: String) {
        viewModelScope.launch {
            todoDao.deleteTodoById(todoId)
        }
    }

    /** 更新搜索关键�?*/
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /** 切换搜索模式 */
    fun toggleSearch() {
        _isSearching.value = !_isSearching.value
        if (!_isSearching.value) {
            _searchQuery.value = ""
        }
    }

    /** 设置排序 */
    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    /** 筛选分�?*/
    fun filterByCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }
}

/** 排序方式 */
enum class SortOrder {
    CREATED_DESC,     // 最新优�?    CREATED_ASC,      // 最早优�?    PRIORITY_DESC,    // 优先级高优先
    DUE_DATE_ASC,     // 截止期近优先
    ALPHABETICAL      // 字母�?}

