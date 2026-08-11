package com.lightmark.ui.screens.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.repository.TodoRepository
import com.lightmark.domain.model.TodoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 专注模式 ViewModel（#58 / #59）
 *
 * 加载单个任务并在沉浸式界面中配合番茄钟计时。纯本地，无网络依赖。
 */
@HiltViewModel
class FocusViewModel @Inject constructor(
    private val repository: TodoRepository
) : ViewModel() {

    private val _task = MutableStateFlow<TodoItem?>(null)
    val task: StateFlow<TodoItem?> = _task.asStateFlow()

    fun load(todoId: String) {
        viewModelScope.launch {
            _task.value = repository.getTodoById(todoId)
        }
    }

    fun markComplete(todoId: String) {
        viewModelScope.launch {
            val t = repository.getTodoById(todoId) ?: return@launch
            repository.update(t.copy(isCompleted = true))
        }
    }
}
