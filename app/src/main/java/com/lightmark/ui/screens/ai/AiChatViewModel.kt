package com.lightmark.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.local.dao.TodoDao
import com.lightmark.data.local.entity.TodoEntity
import com.lightmark.domain.ai.AiService
import com.lightmark.domain.model.TodoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AI 对话 ViewModel
 *
 * - 与 AI 自由对话（在线走 OpenClaw，离线走本地兜底）
 * - 一键把自然语言拆成多条待办并写入数据库
 */
@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val aiService: AiService,
    private val todoDao: TodoDao
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = "ai",
                content = "你好，我是轻刻 AI 助手。你可以问我任何问题，或让我帮你把一段话拆解成待办事项。"
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _lastAddedCount = MutableStateFlow<Int?>(null)
    val lastAddedCount: StateFlow<Int?> = _lastAddedCount.asStateFlow()

    fun send(text: String) {
        val content = text.trim()
        if (content.isBlank() || _isThinking.value) return
        _messages.value = _messages.value + ChatMessage("user", content)
        _isThinking.value = true
        viewModelScope.launch {
            val reply = aiService.chat(content)
            _messages.value = _messages.value + ChatMessage("ai", reply)
            _isThinking.value = false
        }
    }

    /** 把当前输入拆成待办并写入数据库 */
    fun generateAndAdd(prompt: String) {
        val content = prompt.trim()
        if (content.isBlank() || _isThinking.value) return
        _isThinking.value = true
        viewModelScope.launch {
            val todos: List<TodoItem> = aiService.generateTodos(content)
            todos.forEach { todoDao.insertTodo(TodoEntity.fromDomain(it)) }
            _lastAddedCount.value = todos.size
            _isThinking.value = false
            _messages.value = _messages.value + ChatMessage(
                "ai",
                if (todos.isNotEmpty()) "已为你创建 ${todos.size} 条待办 ✅" else "没有解析出待办，换个说法试试～"
            )
        }
    }

    fun clearAddedCount() { _lastAddedCount.value = null }
}

data class ChatMessage(
    val role: String, // "user" | "ai"
    val content: String
)
