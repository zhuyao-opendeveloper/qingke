package com.lightmark.ui.screens.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.local.dao.TodoDao
import com.lightmark.domain.model.Priority
import com.lightmark.domain.model.TodoItem
import com.lightmark.domain.model.TodoStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 看板 / 四象限共用 ViewModel（#52 / #53）
 *
 * 数据源为「活跃」任务（不含归档与回收站），包含已完成任务以便在看板「已完成」列展示。
 */
@HiltViewModel
class BoardViewModel @Inject constructor(
    private val todoDao: TodoDao
) : ViewModel() {

    val todos: StateFlow<List<TodoItem>> = todoDao.getActiveTodos()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 把任务移动到指定看板列 */
    fun moveTo(todoId: String, column: BoardColumn) {
        viewModelScope.launch {
            val entity = todoDao.getTodoById(todoId) ?: return@launch
            val now = System.currentTimeMillis()
            val updated = when (column) {
                BoardColumn.TODO -> entity.copy(
                    isCompleted = false,
                    completedAt = null,
                    status = TodoStatus.ACTIVE.name,
                    startDate = null,
                    updatedAt = now
                )
                BoardColumn.DOING -> entity.copy(
                    isCompleted = false,
                    completedAt = null,
                    status = TodoStatus.ACTIVE.name,
                    startDate = entity.startDate ?: now,
                    updatedAt = now
                )
                BoardColumn.PAUSED -> entity.copy(
                    isCompleted = false,
                    completedAt = null,
                    status = TodoStatus.PAUSED.name,
                    updatedAt = now
                )
                BoardColumn.DONE -> entity.copy(
                    isCompleted = true,
                    completedAt = now,
                    updatedAt = now
                )
            }
            todoDao.updateTodo(updated)
        }
    }

    /** 四象限里直接调整优先级 / 截止时间以改变象限归属 */
    fun setPriority(todoId: String, priority: Priority) {
        viewModelScope.launch {
            val entity = todoDao.getTodoById(todoId) ?: return@launch
            todoDao.updateTodo(
                entity.copy(priority = priority.name, updatedAt = System.currentTimeMillis())
            )
        }
    }

    fun toggleComplete(todoId: String) {
        viewModelScope.launch {
            val entity = todoDao.getTodoById(todoId) ?: return@launch
            val willComplete = !entity.isCompleted
            todoDao.updateTodo(
                entity.copy(
                    isCompleted = willComplete,
                    completedAt = if (willComplete) System.currentTimeMillis() else null,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}

/** 看板列（#52） */
enum class BoardColumn(val label: String) {
    TODO("待办"),
    DOING("进行中"),
    PAUSED("已暂停"),
    DONE("已完成");
}

/** 判断任务属于哪一列 */
fun columnOf(item: TodoItem): BoardColumn {
    if (item.isCompleted) return BoardColumn.DONE
    val status = TodoStatus.fromString(item.status)
    if (status != TodoStatus.ACTIVE) return BoardColumn.PAUSED
    val start = item.startDate
    return if (start != null && start <= System.currentTimeMillis()) BoardColumn.DOING
    else BoardColumn.TODO
}

/** 四象限（#53）：重要 = 优先级 ≥ 高；紧急 = 48 小时内到期或已逾期 */
enum class Quadrant(val label: String, val hint: String) {
    Q1("重要且紧急", "立刻做"),
    Q2("重要不紧急", "计划做"),
    Q3("紧急不重要", "委托/快速做"),
    Q4("不重要不紧急", "少做或不做");
}

private const val URGENT_WINDOW_MS = 48L * 60L * 60L * 1000L

fun quadrantOf(item: TodoItem): Quadrant {
    val important = item.priority.ordinal >= Priority.HIGH.ordinal
    val due = item.dueDate
    val urgent = due != null && due <= System.currentTimeMillis() + URGENT_WINDOW_MS
    return when {
        important && urgent -> Quadrant.Q1
        important && !urgent -> Quadrant.Q2
        !important && urgent -> Quadrant.Q3
        else -> Quadrant.Q4
    }
}
