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
import com.lightmark.domain.model.TodoStatus
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

    private val _startDate = MutableStateFlow<Long?>(null)
    val startDate: StateFlow<Long?> = _startDate.asStateFlow()

    private val _isBlocked = MutableStateFlow(false)
    val isBlocked: StateFlow<Boolean> = _isBlocked.asStateFlow()

    private val _status = MutableStateFlow(TodoStatus.ACTIVE)
    val status: StateFlow<TodoStatus> = _status.asStateFlow()

    private val _recurrenceRule = MutableStateFlow<String?>(null)
    val recurrenceRule: StateFlow<String?> = _recurrenceRule.asStateFlow()

    private val _parentId = MutableStateFlow<String?>(null)
    val parentId: StateFlow<String?> = _parentId.asStateFlow()

    private val _isPrivate = MutableStateFlow(false)
    val isPrivate: StateFlow<Boolean> = _isPrivate.asStateFlow()

    private val _energy = MutableStateFlow("NONE") // 精力标记（#36）
    val energy: StateFlow<String> = _energy.asStateFlow()

    private val _blockedByTaskId = MutableStateFlow<String?>(null) // 依赖阻塞（#16）
    val blockedByTaskId: StateFlow<String?> = _blockedByTaskId.asStateFlow()

    private val _linkedTaskIds = MutableStateFlow<List<String>>(emptyList()) // 双向链接（#34）
    val linkedTaskIds: StateFlow<List<String>> = _linkedTaskIds.asStateFlow()

    private val _attachments = MutableStateFlow<List<String>>(emptyList()) // 附件URI（#6）
    val attachments: StateFlow<List<String>> = _attachments.asStateFlow()

    private val _notes = MutableStateFlow("") // 备注/进展（#7）
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _estimatedMinutes = MutableStateFlow(0) // 预计耗时分钟（#87）
    val estimatedMinutes: StateFlow<Int> = _estimatedMinutes.asStateFlow()

    /** 可作为父任务的候选（排除已删除与自身） */
    val parentCandidates: StateFlow<List<TodoItem>> = todoDao.getAllTodos()
        .map { list -> list.filter { !it.isDeleted }.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            _startDate.value = todo.startDate
            _isBlocked.value = todo.isBlocked
            _status.value = TodoStatus.fromString(todo.status)
            _recurrenceRule.value = todo.recurrenceRule
            _parentId.value = todo.parentId
            _isPrivate.value = todo.isPrivate
            _energy.value = todo.energy
            _blockedByTaskId.value = todo.blockedByTaskId
            _linkedTaskIds.value = if (todo.linkedTaskIds.isBlank()) emptyList()
                else todo.linkedTaskIds.split(",").map { it.trim() }
            _attachments.value = if (todo.attachments.isBlank()) emptyList()
                else todo.attachments.split(",").map { it.trim() }
            _notes.value = todo.notes
            _estimatedMinutes.value = todo.estimatedMinutes
        }
    }

    fun updateTitle(value: String) { _title.value = value }
    fun updateDescription(value: String) { _description.value = value }
    fun updatePriority(value: Priority) { _priority.value = value }
    fun setCategoryId(id: String?) { _categoryId.value = id }
    fun setPinned(pinned: Boolean) { _isPinned.value = pinned }
    fun setDueDate(ts: Long?) { _dueDate.value = ts }
    fun setStartDate(ts: Long?) { _startDate.value = ts }
    fun setBlocked(blocked: Boolean) { _isBlocked.value = blocked }
    fun setStatus(s: TodoStatus) { _status.value = s }
    fun setRecurrenceRule(rule: String?) { _recurrenceRule.value = rule }
    fun setParentId(id: String?) { _parentId.value = id }
    fun setPrivate(private: Boolean) { _isPrivate.value = private }
    fun setEnergy(energy: String) { _energy.value = energy }
    fun setBlockedBy(id: String?) { _blockedByTaskId.value = id }
    fun toggleLinkedTask(id: String) {
        _linkedTaskIds.value = if (_linkedTaskIds.value.contains(id))
            _linkedTaskIds.value - id else _linkedTaskIds.value + id
    }
    fun addAttachments(uris: List<String>) {
        _attachments.value = (_attachments.value + uris).distinct()
    }
    fun removeAttachment(uri: String) {
        _attachments.value = _attachments.value - uri
    }
    fun setReminderEnabled(enabled: Boolean) { _reminderEnabled.value = enabled }
    fun setNotes(value: String) { _notes.value = value }
    fun setEstimatedMinutes(minutes: Int) { _estimatedMinutes.value = minutes.coerceAtLeast(0) }

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
                    startDate = _startDate.value,
                    isBlocked = _isBlocked.value,
                    status = _status.value.name,
                    isArchived = existing?.isArchived ?: false,
                    isDeleted = existing?.isDeleted ?: false,
                    deletedAt = existing?.deletedAt,
                    parentId = _parentId.value,
                    isPrivate = _isPrivate.value,
                    recurrenceRule = _recurrenceRule.value,
                    energy = _energy.value,
                    blockedByTaskId = _blockedByTaskId.value,
                    linkedTaskIds = _linkedTaskIds.value.joinToString(","),
                    attachments = _attachments.value.joinToString(","),
                    notes = _notes.value,
                    estimatedMinutes = _estimatedMinutes.value,
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
                    startDate = _startDate.value,
                    isBlocked = _isBlocked.value,
                    status = _status.value.name,
                    parentId = _parentId.value,
                    isPrivate = _isPrivate.value,
                    recurrenceRule = _recurrenceRule.value,
                    energy = _energy.value,
                    blockedByTaskId = _blockedByTaskId.value,
                    linkedTaskIds = _linkedTaskIds.value.joinToString(","),
                    attachments = _attachments.value.joinToString(","),
                    notes = _notes.value,
                    estimatedMinutes = _estimatedMinutes.value,
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
