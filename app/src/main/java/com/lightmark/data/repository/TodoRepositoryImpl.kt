package com.lightmark.data.repository

import com.lightmark.data.local.dao.AlarmDao
import com.lightmark.data.local.dao.CategoryDao
import com.lightmark.data.local.dao.HabitDao
import com.lightmark.data.local.dao.InboxDao
import com.lightmark.data.local.dao.TemplateDao
import com.lightmark.data.local.dao.TodoDao
import com.lightmark.data.local.entity.TodoEntity
import com.lightmark.domain.model.TodoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 待办仓库实现（纯本地）
 *
 * 轻刻自 v2.0.0 起为完全离线应用：所有数据只存在设备本机的 Room 数据库中，
 * 不含任何网络请求。跨设备迁移请使用「工具 → 备份与导出」的 JSON 备份文件。
 */
@Singleton
class TodoRepositoryImpl @Inject constructor(
    private val todoDao: TodoDao,
    private val categoryDao: CategoryDao,
    private val habitDao: HabitDao,
    private val templateDao: TemplateDao,
    private val alarmDao: AlarmDao,
    private val inboxDao: InboxDao
) : TodoRepository {

    override fun getAllTodos(): Flow<List<TodoItem>> {
        return todoDao.getAllTodos().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTodoById(id: String): TodoItem? {
        return todoDao.getTodoById(id)?.toDomain()
    }

    override suspend fun insert(todo: TodoItem) {
        todoDao.insertTodo(TodoEntity.fromDomain(todo))
    }

    override suspend fun update(todo: TodoItem) {
        todoDao.updateTodo(TodoEntity.fromDomain(todo))
    }

    override suspend fun delete(todo: TodoItem) {
        todoDao.deleteTodo(TodoEntity.fromDomain(todo))
    }

    override suspend fun deleteById(id: String) {
        todoDao.deleteTodoById(id)
    }

    override suspend fun clearAllData() {
        todoDao.clearAll()
        categoryDao.clearAll()
        habitDao.clearHabits()
        habitDao.clearChecks()
        habitDao.clearGoals()
        templateDao.clearAll()
        alarmDao.clearAll()
        inboxDao.clearAll()
    }

    // region 全局标签管理（#30）

    override suspend fun getAllTags(): List<String> {
        return todoDao.getAllTodosList()
            .flatMap { it.tags.split(",").map { t -> t.trim() } }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    override suspend fun renameTag(oldTag: String, newTag: String) {
        if (newTag.isBlank()) return
        todoDao.getAllTodosList().forEach { entity ->
            val tags = entity.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
            val idx = tags.indexOf(oldTag)
            if (idx >= 0) {
                tags[idx] = newTag
                todoDao.updateTodo(entity.copy(tags = tags.joinToString(",")))
            }
        }
    }

    override suspend fun deleteTag(tag: String) {
        todoDao.getAllTodosList().forEach { entity ->
            val tags = entity.tags.split(",").map { it.trim() }.filter { it.isNotBlank() && it != tag }
            todoDao.updateTodo(entity.copy(tags = tags.joinToString(",")))
        }
    }

    // endregion
}
