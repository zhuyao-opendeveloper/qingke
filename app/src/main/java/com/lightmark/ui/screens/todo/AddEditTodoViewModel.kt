package com.lightmark.ui.screens.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.local.dao.TodoDao
import com.lightmark.data.local.entity.TodoEntity
import com.lightmark.domain.model.Priority
import com.lightmark.domain.model.TodoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 添加/编辑待办 ViewModel
 *
 * 功能：
 * - 创建新待办事项
 * - 编辑已有待办事项
 * - 加载已有待办数据填充表单
 */
@HiltViewModel
class AddEditTodoViewModel @Inject constructor(
    private val todoDao: TodoDao
) : ViewModel() {

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _priority = MutableStateFlow(Priority.MEDIUM)
    val priority: StateFlow<Priority> = _priority.asStateFlow()

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveComplete = MutableStateFlow(false)
    val saveComplete: StateFlow<Boolean> = _saveComplete.asStateFlow()

    private var editingTodoId: String? = null

    /**
     * 加载已有待办（编辑模式）
     */
    fun loadTodo(todoId: String) {
        editingTodoId = todoId
        viewModelScope.launch {
            val todo = todoDao.getTodoById(todoId) ?: return@launch
            _title.value = todo.title
            _description.value = todo.description
            _priority.value = Priority.fromString(todo.priority)
            _tags.value = if (todo.tags.isBlank()) emptyList() else todo.tags.split(",").map { it.trim() }
        }
    }

    fun updateTitle(value: String) { _title.value = value }
    fun updateDescription(value: String) { _description.value = value }
    fun updatePriority(value: Priority) { _priority.value = value }

    fun addTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isNotBlank() && !_tags.value.contains(trimmed)) {
            _tags.value = _tags.value + trimmed
        }
    }

    fun removeTag(tag: String) {
        _tags.value = _tags.value - tag
    }

    /**
     * 保存待办（新建或更新）
     */
    fun save() {
        if (_title.value.isBlank()) return

        _isLoading.value = true
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (editingTodoId != null) {
                // 更新
                val existing = todoDao.getTodoById(editingTodoId!!)
                val updated = (existing ?: TodoEntity(
                    id = editingTodoId!!,
                    title = _title.value
                )).copy(
                    title = _title.value,
                    description = _description.value,
                    priority = _priority.value.name,
                    tags = _tags.value.joinToString(","),
                    updatedAt = now
                )
                todoDao.updateTodo(updated)
            } else {
                // 新建
                val newTodo = TodoEntity(
                    id = TodoItem.generateId(),
                    title = _title.value,
                    description = _description.value,
                    priority = _priority.value.name,
                    tags = _tags.value.joinToString(","),
                    createdAt = now,
                    updatedAt = now
                )
                todoDao.insertTodo(newTodo)
            }
            _isLoading.value = false
            _saveComplete.value = true
        }
    }
}
