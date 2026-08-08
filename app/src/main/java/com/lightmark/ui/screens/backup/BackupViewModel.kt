package com.lightmark.ui.screens.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.local.dao.CategoryDao
import com.lightmark.data.local.dao.TodoDao
import com.lightmark.data.local.entity.CategoryEntity
import com.lightmark.data.local.entity.TodoEntity
import com.lightmark.domain.model.Recurrence
import com.lightmark.domain.model.SyncData
import com.lightmark.domain.model.TodoItem
import com.lightmark.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * 备份 / 导出 / 导入（#95 / #96 / #104）
 *
 * 全部走本地文件（SAF），不依赖任何服务端。
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val todoDao: TodoDao,
    private val categoryDao: CategoryDao,
    private val json: Json
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    /** 生成 JSON 全量备份内容 */
    fun exportJson(write: (String) -> Boolean) {
        launchOp(successMsg = "JSON 备份已保存") {
            val todos = todoDao.getAllTodosList().map { it.toDomain() }
            val categories = categoryDao.getAllCategoriesList().map { it.toDomain() }
            val data = SyncData(todos = todos, categories = categories)
            write(json.encodeToString(SyncData.serializer(), data))
        }
    }

    /** 导出 Markdown 清单 */
    fun exportMarkdown(write: (String) -> Boolean) {
        launchOp(successMsg = "Markdown 已导出") {
            val todos = todoDao.getAllTodosList().map { it.toDomain() }
            write(buildMarkdown(todos))
        }
    }

    /** 导出 CSV（Excel 可直接打开） */
    fun exportCsv(write: (String) -> Boolean) {
        launchOp(successMsg = "CSV 已导出") {
            val todos = todoDao.getAllTodosList().map { it.toDomain() }
            write(buildCsv(todos))
        }
    }

    /** 从 JSON 备份导入（合并，同 id 覆盖） */
    fun importJson(content: String) {
        launchOp(successMsg = "导入完成") {
            val data = json.decodeFromString(SyncData.serializer(), content)
            if (data.categories.isNotEmpty()) {
                categoryDao.insertAll(data.categories.map { CategoryEntity.fromDomain(it) })
            }
            if (data.todos.isNotEmpty()) {
                todoDao.insertAll(data.todos.map { TodoEntity.fromDomain(it) })
            }
            _message.value = "导入完成：${data.todos.size} 条待办、${data.categories.size} 个分类"
            true
        }
    }

    private fun launchOp(successMsg: String, block: suspend () -> Boolean) {
        if (_busy.value) return
        _message.value = null
        viewModelScope.launch {
            _busy.value = true
            val result = runCatching { block() }
            _busy.value = false
            result.onSuccess { ok ->
                if (_message.value == null) {
                    _message.value = if (ok) successMsg else "操作已取消"
                }
            }.onFailure { e ->
                _message.value = "失败：${e.message ?: e::class.java.simpleName}"
            }
        }
    }

    private fun buildMarkdown(todos: List<TodoItem>): String {
        val sb = StringBuilder()
        sb.append("# 轻刻 待办导出\n\n")
        sb.append("> 导出时间：${DateTimeUtils.formatDateTime(System.currentTimeMillis())}\n\n")

        val active = todos.filter { !it.isDeleted && !it.isArchived }
        val archived = todos.filter { it.isArchived && !it.isDeleted }
        val trashed = todos.filter { it.isDeleted }

        fun section(title: String, list: List<TodoItem>) {
            if (list.isEmpty()) return
            sb.append("## $title（${list.size}）\n\n")
            list.sortedBy { it.isCompleted }.forEach { t ->
                sb.append(if (t.isCompleted) "- [x] " else "- [ ] ")
                sb.append(t.title)
                val meta = mutableListOf<String>()
                meta.add("优先级:${t.priority.name}")
                t.dueDate?.let { meta.add("截止:${DateTimeUtils.formatDateTime(it)}") }
                t.startDate?.let { meta.add("开始:${DateTimeUtils.formatDateTime(it)}") }
                if (!t.recurrenceRule.isNullOrBlank() && t.recurrenceRule != Recurrence.NONE) {
                    meta.add("重复:${Recurrence.label(t.recurrenceRule)}")
                }
                if (t.tags.isNotEmpty()) meta.add(t.tags.joinToString(" ") { "#$it" })
                if (meta.isNotEmpty()) sb.append("  `").append(meta.joinToString(" | ")).append("`")
                sb.append("\n")
                if (t.description.isNotBlank()) {
                    t.description.lines().forEach { line -> sb.append("    ").append(line).append("\n") }
                }
            }
            sb.append("\n")
        }

        section("待办", active)
        section("已归档", archived)
        section("回收站", trashed)
        return sb.toString()
    }

    private fun buildCsv(todos: List<TodoItem>): String {
        fun esc(v: String): String = "\"" + v.replace("\"", "\"\"") + "\""
        val sb = StringBuilder()
        // UTF-8 BOM，避免 Excel 打开中文乱码
        sb.append('\uFEFF')
        sb.append("标题,描述,是否完成,优先级,状态,标签,截止时间,开始时间,重复,已归档,已删除,创建时间\n")
        todos.forEach { t ->
            sb.append(esc(t.title)).append(',')
            sb.append(esc(t.description)).append(',')
            sb.append(if (t.isCompleted) "是" else "否").append(',')
            sb.append(t.priority.name).append(',')
            sb.append(t.status).append(',')
            sb.append(esc(t.tags.joinToString(" "))).append(',')
            sb.append(esc(t.dueDate?.let { DateTimeUtils.formatDateTime(it) } ?: "")).append(',')
            sb.append(esc(t.startDate?.let { DateTimeUtils.formatDateTime(it) } ?: "")).append(',')
            sb.append(esc(Recurrence.label(t.recurrenceRule))).append(',')
            sb.append(if (t.isArchived) "是" else "否").append(',')
            sb.append(if (t.isDeleted) "是" else "否").append(',')
            sb.append(esc(DateTimeUtils.formatDateTime(t.createdAt)))
            sb.append('\n')
        }
        return sb.toString()
    }
}
