package com.lightmark.data.repository

import com.lightmark.domain.model.TodoItem
import kotlinx.coroutines.flow.Flow

/**
 * 待办数据仓库接口
 *
 * 统一数据访问层，封装本地 Room 和远程 GitHub API
 */
interface TodoRepository {
    /** 获取所有待办（流式） */
    fun getAllTodos(): Flow<List<TodoItem>>

    /** 根据 ID 获取待办 */
    suspend fun getTodoById(id: String): TodoItem?

    /** 插入待办 */
    suspend fun insert(todo: TodoItem)

    /** 更新待办 */
    suspend fun update(todo: TodoItem)

    /** 删除待办 */
    suspend fun delete(todo: TodoItem)

    /** 按 ID 删除 */
    suspend fun deleteById(id: String)
}
