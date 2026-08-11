package com.lightmark.data.repository

import com.lightmark.domain.model.SmartList
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

    /** 清空本机全部数据（#102）：待办 / 分类 / 习惯 / 打卡 / 目标 / 模板 / 闹钟 / 收件箱 */
    suspend fun clearAllData()

    /** 全局标签管理（#30）：列出所有去重标签 */
    suspend fun getAllTags(): List<String>

    /** 全局标签管理（#30）：重命名标签（跨所有待办改写） */
    suspend fun renameTag(oldTag: String, newTag: String)

    /** 全局标签管理（#30）：删除标签（从所有待办移除） */
    suspend fun deleteTag(tag: String)

    /** 自定义智能清单（#28）：订阅全部清单 */
    fun getSmartLists(): Flow<List<SmartList>>

    /** 自定义智能清单（#28）：保存（新增 / 更新） */
    suspend fun saveSmartList(list: SmartList)

    /** 自定义智能清单（#28）：删除 */
    suspend fun deleteSmartList(id: String)
}
