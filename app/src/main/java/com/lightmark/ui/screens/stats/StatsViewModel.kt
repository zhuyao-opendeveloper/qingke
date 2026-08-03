package com.lightmark.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.local.dao.CategoryDao
import com.lightmark.data.local.dao.TodoDao
import com.lightmark.domain.model.Category
import com.lightmark.domain.model.Priority
import com.lightmark.domain.model.TodoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 统计页 ViewModel
 *
 * 汇总：总数 / 已完成 / 待办 / 完成率 / 逾期数 /
 * 按优先级分布 / 按分类分布。
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val todoDao: TodoDao,
    private val categoryDao: CategoryDao
) : ViewModel() {

    val stats: StateFlow<StatsData> = combine(
        todoDao.getAllTodos(),
        categoryDao.getAllCategories()
    ) { todoEntities, catEntities ->
        val list: List<TodoItem> = todoEntities.map { it.toDomain() }
        val categories: List<Category> = catEntities.map { it.toDomain() }
        val total = list.size
        val done = list.count { it.isCompleted }
        val pending = total - done
        val overdue = list.count {
            it.dueDate != null && it.dueDate < System.currentTimeMillis() && !it.isCompleted
        }
        val rate = if (total > 0) done.toFloat() / total else 0f

        val byPriority = Priority.entries.associateWith { p ->
            list.count { it.priority == p }
        }
        val byCategory = categories.map { cat ->
            cat to list.count { it.categoryId == cat.id }
        }

        StatsData(
            total = total,
            done = done,
            pending = pending,
            overdue = overdue,
            rate = rate,
            byPriority = byPriority,
            byCategory = byCategory
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsData())
}

data class StatsData(
    val total: Int = 0,
    val done: Int = 0,
    val pending: Int = 0,
    val overdue: Int = 0,
    val rate: Float = 0f,
    val byPriority: Map<Priority, Int> = emptyMap(),
    val byCategory: List<Pair<Category, Int>> = emptyList()
)
