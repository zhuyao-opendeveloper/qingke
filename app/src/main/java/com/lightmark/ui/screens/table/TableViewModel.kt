package com.lightmark.ui.screens.table

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.local.dao.CategoryDao
import com.lightmark.data.local.dao.TodoDao
import com.lightmark.domain.model.Priority
import com.lightmark.domain.model.TodoItem
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
 * 表格视图数据源（#56）
 */
@HiltViewModel
class TableViewModel @Inject constructor(
    private val todoDao: TodoDao,
    categoryDao: CategoryDao
) : ViewModel() {

    private val _filter = MutableStateFlow(TableFilter.ALL)
    val filter: StateFlow<TableFilter> = _filter.asStateFlow()

    private val _sort = MutableStateFlow(TableSort.UPDATED)
    val sort: StateFlow<TableSort> = _sort.asStateFlow()

    private val _ascending = MutableStateFlow(false)
    val ascending: StateFlow<Boolean> = _ascending.asStateFlow()

    /** 分类 id -> 名称 */
    val categoryNames: StateFlow<Map<String, String>> = categoryDao.getAllCategories()
        .map { list -> list.associate { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val rows: StateFlow<List<TodoItem>> = combine(
        todoDao.getActiveTodos(),
        _filter,
        _sort,
        _ascending
    ) { entities, filter, sort, asc ->
        var list = entities.map { it.toDomain() }

        list = when (filter) {
            TableFilter.ALL -> list
            TableFilter.UNDONE -> list.filter { !it.isCompleted }
            TableFilter.DONE -> list.filter { it.isCompleted }
            TableFilter.OVERDUE -> list.filter {
                !it.isCompleted && (it.dueDate ?: Long.MAX_VALUE) < System.currentTimeMillis()
            }
        }

        val comparator: Comparator<TodoItem> = when (sort) {
            TableSort.TITLE -> compareBy { it.title }
            TableSort.PRIORITY -> compareBy { -it.priority.ordinal }
            TableSort.DUE -> compareBy { it.dueDate ?: Long.MAX_VALUE }
            TableSort.CATEGORY -> compareBy { it.categoryId ?: "" }
            TableSort.UPDATED -> compareBy { it.updatedAt }
        }

        val sorted = list.sortedWith(comparator)
        if (asc) sorted else sorted.reversed()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(value: TableFilter) {
        _filter.value = value
    }

    /** 点击表头：同列再点一次切换升降序 */
    fun toggleSort(value: TableSort) {
        if (_sort.value == value) {
            _ascending.value = !_ascending.value
        } else {
            _sort.value = value
            _ascending.value = true
        }
    }

    fun toggleComplete(item: TodoItem) {
        viewModelScope.launch {
            val entity = todoDao.getTodoById(item.id) ?: return@launch
            val now = System.currentTimeMillis()
            todoDao.updateTodo(
                entity.copy(
                    isCompleted = !entity.isCompleted,
                    completedAt = if (!entity.isCompleted) now else null,
                    updatedAt = now
                )
            )
        }
    }

    fun setPriority(item: TodoItem, priority: Priority) {
        viewModelScope.launch {
            val entity = todoDao.getTodoById(item.id) ?: return@launch
            todoDao.updateTodo(
                entity.copy(priority = priority.name, updatedAt = System.currentTimeMillis())
            )
        }
    }
}

enum class TableFilter(val label: String) {
    ALL("全部"),
    UNDONE("未完成"),
    DONE("已完成"),
    OVERDUE("已逾期")
}

enum class TableSort(val label: String) {
    TITLE("标题"),
    PRIORITY("优先级"),
    CATEGORY("分类"),
    DUE("到期"),
    UPDATED("更新")
}
