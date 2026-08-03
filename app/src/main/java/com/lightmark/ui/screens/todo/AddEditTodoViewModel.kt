package com.lightmark.ui.screens.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.local.dao.CategoryDao
import com.lightmark.data.local.dao.TodoDao
import com.lightmark.data.local.entity.TodoEntity
import com.lightmark.data.reminder.ReminderScheduler
import com.lightmark.data.settings.SettingsRepository
import com.lightmark.domain.ai.AiService
import com.lightmark.domain.model.Category
import com.lightmark.domain.model.Priority
import com.lightmark.domain.model.TodoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 添加/编辑待办 ViewModel
 *
 * 功能：
 * - 创建 / 编辑待办
 * - 分类、置顶、截止时间、提醒开关
 * - AI 智能填写（推断分类/标签/优先级/截止）、AI 润色描述
 */
@HiltViewModel
class AddEditTodoViewModel @Inject constructor(
    private val todoDao: TodoDao,
    private val categoryDao: CategoryDao,
    private val aiService: AiService,
    private val reminderScheduler: ReminderScheduler,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _priority = MutableStateFlow(Priority.MEDIUM)
    val priority: StateFlow<Priority> = _priority.asStateFlow()

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _categoryId = MutableStateFlow<String?>(null)
    val categoryId: StateFlow<String?> = _categoryId.asStateFlow()

    private val _isPinned = MutableStateFlow(false)
    val isPinned: StateFlow<Boolean> = _isPinned.asStateFlow()

    private val _dueDate = MutableStateFlow<Long?>(null)
    val dueDate: StateFlow<Long?> = _dueDate.asStateFlow()

    private val _reminderEnabled = MutableStateFlow(true)
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    private val _aiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiState: StateFlow<AiUiState> = _aiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveComplete = MutableStateFlow(false)
    val saveComplete: StateFlow<Boolean> = _saveComplete.asStateFlow()

    val categories: StateFlow<List<Category>> = categoryDao.getAllCategories()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var editingTodoId: String? = null

    init {
        viewModelScope.launch {
            _reminderEnabled.value = settingsRepository.settings.first().reminderEnabled
        }
    }

    fun loadTodo(todoId: String) {
        editingTodoId = todoId
        viewModelScope.launch {
            val todo = todoDao.getTodoById(todoId) ?: return@launch
            _title.value = todo.title
            _description.value = todo.description
            _priority.value = Priority.fromString(todo.priority)
            _tags.value = if (todo.tags.isBlank()) emptyList() else todo.tags.split(",").map { it.trim() }
            _categoryId.value = todo.categoryId
            _isPinned.value = todo.isPinned
            _dueDate.value = todo.dueDate
        }
    }

    fun updateTitle(value: String) { _title.value = value }
    fun updateDescription(value: String) { _description.value = value }
    fun updatePriority(value: Priority) { _priority.value = value }
    fun setCategoryId(id: String?) { _categoryId.value = id }
    fun setPinned(pinned: Boolean) { _isPinned.value = pinned }
    fun setDueDate(ts: Long?) { _dueDate.value = ts }
    fun setReminderEnabled(enabled: Boolean) { _reminderEnabled.value = enabled }

    fun addTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isNotBlank() && !_tags.value.contains(trimmed)) {
            _tags.value = _tags.value + trimmed
        }
    }

    fun removeTag(tag: String) {
        _tags.value = _tags.value - tag
    }

    /** AI 智能填写：根据当前标题/描述推断分类、标签、优先级与截止天数 */
    fun smartFill() {
        if (_aiState.value == AiUiState.Loading) return
        viewModelScope.launch {
            _aiState.value = AiUiState.Loading
            runCatching {
                val suggestion = aiService.suggestForTodo(_title.value, _description.value)
                // 分类按名称匹配已有分类
                suggestion.suggestedCategory?.let { name ->
                    val match = categoryDao.getAllCategoriesList().firstOrNull { it.name == name }
                    if (match != null) _categoryId.value = match.id
                }
                // 合并标签
                if (suggestion.suggestedTags.isNotEmpty()) {
                    _tags.value = (_tags.value + suggestion.suggestedTags)
                        .distinct()
                        .take(8)
                }
                // 优先级
                suggestion.suggestedPriority?.let { _priority.value = it }
                // 截止天数
                suggestion.suggestedDueDate?.let { days ->
                    val ts = System.currentTimeMillis() + days * 86_400_000L
                    _dueDate.value = ts
                }
                suggestion
            }.onSuccess {
                _aiState.value = AiUiState.Success("AI 已为你智能填充")
            }.onFailure {
                _aiState.value = AiUiState.Error(it.message ?: "AI 调用失败")
            }
        }
    }

    /** AI 润色描述 */
    fun polishDescription() {
        if (_aiState.value == AiUiState.Loading || _description.value.isBlank()) return
        viewModelScope.launch {
            _aiState.value = AiUiState.Loading
            runCatching { aiService.polishText(_description.value) }
                .onSuccess { polished ->
                    _description.value = polished
                    _aiState.value = AiUiState.Success("描述已润色")
                }
                .onFailure {
                    _aiState.value = AiUiState.Error(it.message ?: "AI 调用失败")
                }
        }
    }

    fun clearAiState() { _aiState.value = AiUiState.Idle }

    fun save() {
        if (_title.value.isBlank()) return
        _isLoading.value = true
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = editingTodoId ?: TodoItem.generateId()
            if (editingTodoId != null) {
                val existing = todoDao.getTodoById(editingTodoId!!)
                val updated = (existing ?: TodoEntity(
                    id = editingTodoId!!,
                    title = _title.value
                )).copy(
                    title = _title.value,
                    description = _description.value,
                    priority = _priority.value.name,
                    tags = _tags.value.joinToString(","),
                    categoryId = _categoryId.value,
                    isPinned = _isPinned.value,
                    dueDate = _dueDate.value,
                    updatedAt = now
                )
                todoDao.updateTodo(updated)
            } else {
                val newTodo = TodoEntity(
                    id = id,
                    title = _title.value,
                    description = _description.value,
                    priority = _priority.value.name,
                    tags = _tags.value.joinToString(","),
                    categoryId = _categoryId.value,
                    isPinned = _isPinned.value,
                    dueDate = _dueDate.value,
                    createdAt = now,
                    updatedAt = now
                )
                todoDao.insertTodo(newTodo)
            }

            // 提醒调度
            if (_dueDate.value != null && _reminderEnabled.value) {
                reminderScheduler.schedule(id, _title.value, _dueDate.value!!, true)
            } else {
                reminderScheduler.cancel(id)
            }

            _isLoading.value = false
            _saveComplete.value = true
        }
    }
}

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Success(val message: String) : AiUiState
    data class Error(val message: String) : AiUiState
}
