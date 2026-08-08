package com.lightmark.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.repository.TodoRepository
import com.lightmark.domain.model.TodoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

/** 日历视图模式 */
enum class CalendarViewMode(val label: String) {
    DAY("日"), MONTH("月"), YEAR("年")
}

/** 一天的键：yyyyMMdd 整数，便于分组比较 */
fun dayKeyOf(millis: Long): Int {
    val c = Calendar.getInstance().apply { timeInMillis = millis }
    return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
}

fun dayKeyOf(year: Int, month0: Int, day: Int): Int = year * 10000 + (month0 + 1) * 100 + day

@HiltViewModel
class CalendarViewModel @Inject constructor(
    repository: TodoRepository
) : ViewModel() {

    /** 有截止日期的待办，按 yyyyMMdd 分组 */
    val todosByDay: StateFlow<Map<Int, List<TodoItem>>> = repository.getAllTodos()
        .map { list ->
            list.filter { it.dueDate != null }
                .groupBy { dayKeyOf(it.dueDate!!) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** 无截止日期的待办数量，用于提示 */
    val undatedCount: StateFlow<Int> = repository.getAllTodos()
        .map { list -> list.count { it.dueDate == null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}
