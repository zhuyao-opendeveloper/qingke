package com.lightmark.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.local.dao.TodoDao
import com.lightmark.data.local.entity.TodoEntity
import com.lightmark.domain.model.IconPack
import com.lightmark.domain.model.TodoItem
import com.lightmark.icons.IconProvider
import com.lightmark.icons.getIconProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主页 ViewModel
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val todoDao: TodoDao
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
                    it.tags.any { tag -> tag.contains(query as CharSequence, true) }
            }
        }

        if (categoryId != null) {
            filtered = filtered.filter { it.categoryId == categoryId }
        }

        when (sort) {
            SortOrder.CREATED_DESC -> filtered.sortedByDescending { it.createdAt }
            SortOrder.CREATED_ASC -> filtered.sortedBy { it.createdAt }
            SortOrder.PRIORITY_DESC -> filtered.sortedByDescending { it.priority.ordinal }
            SortOrder.DUE_DATE_ASC -> filtered.sortedBy { it.dueDate ?: Long.MAX_VALUE }
            SortOrder.ALPHABETICAL -> filtered.sortedBy { it.title }
        }
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

    fun deleteTodo(todoId: String) {
        viewModelScope.launch {
            todoDao.deleteTodoById(todoId)
        }
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
}

enum class SortOrder {
    CREATED_DESC,
    CREATED_ASC,
    PRIORITY_DESC,
    DUE_DATE_ASC,
    ALPHABETICAL
}

private fun TodoEntity.toDomain() = TodoItem(
    id = id,
    title = title,
    description = description,
    isCompleted = isCompleted,
    priority = com.lightmark.domain.model.Priority.fromString(priority),
    categoryId = categoryId,
    tags = tags,
    dueDate = dueDate,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt
)

