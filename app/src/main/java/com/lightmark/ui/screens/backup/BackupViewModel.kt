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

    /** 导出 HTML（可直接浏览器打开、另存为 PDF 打印）(#99) */
    fun exportHtml(write: (String) -> Boolean) {
        launchOp(successMsg = "HTML 已导出") {
            val todos = todoDao.getAllTodosList().map { it.toDomain() }
            val cats = categoryDao.getAllCategoriesList().associate { it.id to it.name }
            write(buildHtml(todos, cats))
        }
    }

    /** 导出 iCalendar（.ics，可被系统/第三方日历订阅）(#49 / #104 单向) */
    fun exportIcs(write: (String) -> Boolean) {
        launchOp(successMsg = "iCalendar 已导出") {
            val todos = todoDao.getAllTodosList().map { it.toDomain() }
            write(buildIcs(todos))
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

    private fun buildHtml(todos: List<TodoItem>, catNames: Map<String, String>): String {
        fun esc(v: String): String = v
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;")

        val sb = StringBuilder()
        sb.append("<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n")
        sb.append("<meta charset=\"utf-8\">\n")
        sb.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n")
        sb.append("<title>轻刻 待办导出</title>\n<style>\n")
        sb.append(
            """
            :root{color-scheme:light}
            body{font-family:-apple-system,'PingFang SC','Microsoft YaHei',sans-serif;
                 background:#fafafa;color:#1c1b1f;margin:0;padding:24px}
            h1{font-size:22px;margin:0 0 4px}
            .meta{color:#6b6b70;font-size:13px;margin-bottom:20px}
            h2{font-size:16px;margin:24px 0 8px;color:#3c3c43}
            table{width:100%;border-collapse:collapse;background:#fff;
                  box-shadow:0 1px 3px rgba(0,0,0,.08);border-radius:8px;overflow:hidden}
            th,td{padding:8px 10px;font-size:13px;text-align:left;border-bottom:1px solid #eee}
            th{background:#f2f2f5;font-weight:600}
            tr:last-child td{border-bottom:none}
            .done{color:#9a9aa0;text-decoration:line-through}
            .p-URGENT{color:#d32f2f;font-weight:600}
            .p-HIGH{color:#f57c00;font-weight:600}
            .p-MEDIUM{color:#1976d2}
            .p-LOW{color:#388e3c}
            .p-IDLE{color:#9e9e9e}
            .tag{display:inline-block;background:#eceaf4;color:#5b4b9b;
                 border-radius:4px;padding:1px 6px;margin-right:4px;font-size:11px}
            .overdue{color:#d32f2f}
            @media print{body{background:#fff;padding:0}table{box-shadow:none}}
            """.trimIndent()
        )
        sb.append("\n</style>\n</head>\n<body>\n")
        sb.append("<h1>轻刻 · 待办导出</h1>\n")
        sb.append("<div class=\"meta\">导出时间：")
            .append(esc(DateTimeUtils.formatDateTime(System.currentTimeMillis())))
            .append(" · 共 ").append(todos.size).append(" 条 · 提示：浏览器打印可另存为 PDF</div>\n")

        fun section(title: String, list: List<TodoItem>) {
            if (list.isEmpty()) return
            sb.append("<h2>").append(esc(title)).append("（").append(list.size).append("）</h2>\n")
            sb.append("<table><thead><tr>")
            sb.append("<th style=\"width:32px\"></th><th>标题</th><th>优先级</th>")
            sb.append("<th>分类</th><th>标签</th><th>截止</th><th>重复</th>")
            sb.append("</tr></thead><tbody>\n")
            list.sortedWith(
                compareBy<TodoItem> { it.isCompleted }
                    .thenByDescending { it.priority.ordinal }
            ).forEach { t ->
                sb.append("<tr>")
                sb.append("<td>").append(if (t.isCompleted) "✅" else "⬜").append("</td>")
                sb.append("<td class=\"").append(if (t.isCompleted) "done" else "").append("\">")
                    .append(esc(t.title))
                if (t.description.isNotBlank()) {
                    sb.append("<div style=\"color:#8a8a90;font-size:12px;margin-top:2px\">")
                        .append(esc(t.description.take(160))).append("</div>")
                }
                sb.append("</td>")
                sb.append("<td class=\"p-").append(t.priority.name).append("\">")
                    .append(
                        when (t.priority.name) {
                            "URGENT" -> "紧急"
                            "HIGH" -> "高"
                            "MEDIUM" -> "中"
                            "LOW" -> "低"
                            else -> "空闲"
                        }
                    ).append("</td>")
                sb.append("<td>").append(esc(t.categoryId?.let { catNames[it] } ?: "—")).append("</td>")
                sb.append("<td>")
                t.tags.forEach { sb.append("<span class=\"tag\">").append(esc(it)).append("</span>") }
                if (t.tags.isEmpty()) sb.append("—")
                sb.append("</td>")
                val overdue = t.dueDate != null && !t.isCompleted && DateTimeUtils.isOverdue(t.dueDate!!)
                sb.append("<td class=\"").append(if (overdue) "overdue" else "").append("\">")
                    .append(esc(t.dueDate?.let { DateTimeUtils.formatDateTime(it) } ?: "—"))
                    .append("</td>")
                sb.append("<td>").append(esc(Recurrence.label(t.recurrenceRule))).append("</td>")
                sb.append("</tr>\n")
            }
            sb.append("</tbody></table>\n")
        }

        section("待办", todos.filter { !it.isDeleted && !it.isArchived })
        section("已归档", todos.filter { it.isArchived && !it.isDeleted })
        section("回收站", todos.filter { it.isDeleted })

        sb.append("</body>\n</html>\n")
        return sb.toString()
    }

    private fun buildIcs(todos: List<TodoItem>): String {
        val utc = java.text.SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        fun esc(v: String): String = v
            .replace("\\", "\\\\").replace(";", "\\;")
            .replace(",", "\\,").replace("\n", "\\n")

        val sb = StringBuilder()
        sb.append("BEGIN:VCALENDAR\r\n")
        sb.append("VERSION:2.0\r\n")
        sb.append("PRODID:-//LightMark//Todo Export//CN\r\n")
        sb.append("CALSCALE:GREGORIAN\r\n")
        todos.filter { !it.isDeleted && it.dueDate != null }.forEach { t ->
            sb.append("BEGIN:VTODO\r\n")
            sb.append("UID:").append(t.id).append("@lightmark\r\n")
            sb.append("DTSTAMP:").append(utc.format(java.util.Date(t.createdAt))).append("\r\n")
            t.startDate?.let { sb.append("DTSTART:").append(utc.format(java.util.Date(it))).append("\r\n") }
            sb.append("DUE:").append(utc.format(java.util.Date(t.dueDate!!))).append("\r\n")
            sb.append("SUMMARY:").append(esc(t.title)).append("\r\n")
            if (t.description.isNotBlank()) {
                sb.append("DESCRIPTION:").append(esc(t.description)).append("\r\n")
            }
            sb.append("STATUS:").append(if (t.isCompleted) "COMPLETED" else "NEEDS-ACTION").append("\r\n")
            val icsPriority = when (t.priority.name) {
                "URGENT" -> 1
                "HIGH" -> 3
                "MEDIUM" -> 5
                "LOW" -> 7
                else -> 9
            }
            sb.append("PRIORITY:").append(icsPriority).append("\r\n")
            if (t.tags.isNotEmpty()) {
                sb.append("CATEGORIES:").append(t.tags.joinToString(",") { esc(it) }).append("\r\n")
            }
            when (t.recurrenceRule) {
                Recurrence.DAILY -> sb.append("RRULE:FREQ=DAILY\r\n")
                Recurrence.WEEKLY -> sb.append("RRULE:FREQ=WEEKLY\r\n")
                Recurrence.MONTHLY -> sb.append("RRULE:FREQ=MONTHLY\r\n")
                else -> {
                    val rule = t.recurrenceRule
                    if (rule != null && rule.startsWith(Recurrence.INTERVAL_PREFIX)) {
                        val n = rule.removePrefix(Recurrence.INTERVAL_PREFIX)
                        sb.append("RRULE:FREQ=DAILY;INTERVAL=").append(n).append("\r\n")
                    }
                }
            }
            sb.append("END:VTODO\r\n")
        }
        sb.append("END:VCALENDAR\r\n")
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
