package com.lightmark.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lightmark.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

/**
 * 待办事项 DAO
 */
@Dao
interface TodoDao {

    /** 获取所有待办（按更新时间倒序） */
    @Query("SELECT * FROM todos ORDER BY updatedAt DESC")
    fun getAllTodos(): Flow<List<TodoEntity>>

    /** 活跃待办：未删除、未归档（首页默认列表） */
    @Query("SELECT * FROM todos WHERE isDeleted = 0 AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getActiveTodos(): Flow<List<TodoEntity>>

    /** 已归档待办 */
    @Query("SELECT * FROM todos WHERE isArchived = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getArchivedTodos(): Flow<List<TodoEntity>>

    /** 回收站（已软删除） */
    @Query("SELECT * FROM todos WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getTrashTodos(): Flow<List<TodoEntity>>

    /** 某个父任务的直接子任务 */
    @Query("SELECT * FROM todos WHERE parentId = :parentId AND isDeleted = 0 ORDER BY createdAt ASC")
    fun getSubtasks(parentId: String): Flow<List<TodoEntity>>

    /** 按生命周期状态筛选 */
    @Query("SELECT * FROM todos WHERE status = :status AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getTodosByStatus(status: String): Flow<List<TodoEntity>>

    /** 根据ID获取单条待办 */
    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoById(id: String): TodoEntity?

    /** 搜索待办（标题或描述匹配） */
    @Query("SELECT * FROM todos WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchTodos(query: String): Flow<List<TodoEntity>>

    /** 按完成状态筛选 */
    @Query("SELECT * FROM todos WHERE isCompleted = :completed ORDER BY updatedAt DESC")
    fun getTodosByCompleted(completed: Boolean): Flow<List<TodoEntity>>

    /** 按分类筛选 */
    @Query("SELECT * FROM todos WHERE categoryId = :categoryId ORDER BY updatedAt DESC")
    fun getTodosByCategory(categoryId: String): Flow<List<TodoEntity>>

    /** 按优先级筛选 */
    @Query("SELECT * FROM todos WHERE priority = :priority ORDER BY updatedAt DESC")
    fun getTodosByPriority(priority: String): Flow<List<TodoEntity>>

    /** 插入或替换（upsert） */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity)

    /** 批量插入 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(todos: List<TodoEntity>)

    /** 更新 */
    @Update
    suspend fun updateTodo(todo: TodoEntity)

    /** 删除 */
    @Delete
    suspend fun deleteTodo(todo: TodoEntity)

    /** 根据ID删除 */
    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteTodoById(id: String)

    /** 清空所有数据（用于同步覆盖） */
    @Query("DELETE FROM todos")
    suspend fun clearAll()

    /** 获取所有数据（非 Flow，用于同步） */
    @Query("SELECT * FROM todos")
    suspend fun getAllTodosList(): List<TodoEntity>
}
