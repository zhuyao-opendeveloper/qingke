package com.lightmark.data.repository

import com.lightmark.data.local.dao.TodoDao
import com.lightmark.data.remote.GitHubApiService
import com.lightmark.domain.model.Category
import com.lightmark.domain.model.SyncData
import com.lightmark.domain.model.TodoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 待办数据仓库实现
 *
 * 本地：Room 数据库（离线可用）
 * 远程：GitHub 私有仓库（数据私有化）
 */
@Singleton
class TodoRepositoryImpl @Inject constructor(
    private val todoDao: TodoDao,
    private val gitHubApiService: GitHubApiService,
    private val json: Json
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
        todoDao.insertTodo(todo.toEntity())
    }

    override suspend fun update(todo: TodoItem) {
        todoDao.updateTodo(todo.toEntity())
    }

    override suspend fun delete(todo: TodoItem) {
        todoDao.deleteTodo(todo.toEntity())
    }

    override suspend fun deleteById(id: String) {
        todoDao.deleteTodoById(id)
    }

    override suspend fun syncToGitHub(token: String, login: String): Result<Unit> = runCatching {
        val todos = todoDao.getAllTodosList().map { it.toDomain() }
        val categories = emptyList<Category>()

        val syncData = SyncData(
            todos = todos,
            categories = categories,
            lastSync = System.currentTimeMillis()
        )

        val content = json.encodeToString(SyncData.serializer(), syncData)
        val base64Content = Base64.getEncoder().encodeToString(content.toByteArray(Charsets.UTF_8))

        gitHubApiService.createOrUpdateFile(
            owner = login,
            repo = "lightmark-data",
            path = SyncData.FILE_PATH,
            body = com.lightmark.data.remote.GitHubContentRequestDto(
                message = "同步轻刻数据 - ${System.currentTimeMillis()}",
                content = base64Content
            )
        )
    }

    override suspend fun syncFromGitHub(token: String, login: String): Result<Unit> = runCatching {
        val response = gitHubApiService.getFileContent(
            owner = login,
            repo = "lightmark-data",
            path = SyncData.FILE_PATH
        )

        val contentBytes = Base64.getDecoder().decode(response.content)
        val content = String(contentBytes, Charsets.UTF_8)

        val syncData = json.decodeFromString(SyncData.serializer(), content)

        syncData.todos.forEach { todo ->
            todoDao.insertTodo(todo.toEntity())
        }
    }

    // ------- 扩展函数 -------

    private fun TodoItem.toEntity() = com.lightmark.data.local.entity.TodoEntity(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted,
        priority = priority.name,
        categoryId = categoryId,
        tags = tags,
        dueDate = dueDate,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt
    )

    private fun com.lightmark.data.local.entity.TodoEntity.toDomain() = TodoItem(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted,
        priority = com.lightmark.domain.model.Priority.fromString(priority),
        categoryId = categoryId,
        tags = tags,
        dueDate = dueDate,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt
    )
}
