package com.lightmark.ui.screens.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.local.dao.CategoryDao
import com.lightmark.data.local.dao.TemplateDao
import com.lightmark.data.local.dao.TodoDao
import com.lightmark.data.local.entity.CategoryEntity
import com.lightmark.data.local.entity.TemplateEntity
import com.lightmark.data.local.entity.TodoEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * 任务模板视图模型（#20）
 *
 * 能力：
 * - 内置模板首启动自动播种
 * - 自定义模板增删改
 * - 从现有任务（含其子任务）反向存为模板
 * - 一键套用：生成父任务 + 全部子任务
 */
@HiltViewModel
class TemplateViewModel @Inject constructor(
    private val templateDao: TemplateDao,
    private val todoDao: TodoDao,
    private val categoryDao: CategoryDao
) : ViewModel() {

    val templates: StateFlow<List<TemplateEntity>> = templateDao.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        seedBuiltInIfEmpty()
    }

    fun consumeMessage() {
        _message.value = null
    }

    /** 首次进入时播种内置模板 */
    private fun seedBuiltInIfEmpty() {
        viewModelScope.launch {
            if (templateDao.countTemplates() > 0) return@launch
            templateDao.upsertAll(builtInTemplates())
        }
    }

    /** 套用模板：生成父任务与子任务 */
    fun applyTemplate(template: TemplateEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val parentId = UUID.randomUUID().toString()
            val due = template.dueInDays?.let { now + it * 86_400_000L }

            todoDao.insertTodo(
                TodoEntity(
                    id = parentId,
                    title = template.name,
                    description = template.description,
                    priority = template.priority,
                    categoryId = template.categoryId,
                    tags = template.tags,
                    dueDate = due,
                    recurrenceRule = template.recurrenceRule,
                    createdAt = now,
                    updatedAt = now
                )
            )

            val subs = template.subtaskList
            subs.forEachIndexed { index, title ->
                todoDao.insertTodo(
                    TodoEntity(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        priority = template.priority,
                        categoryId = template.categoryId,
                        parentId = parentId,
                        dueDate = due,
                        createdAt = now + index,
                        updatedAt = now + index
                    )
                )
            }

            templateDao.incrementUsage(template.id)
            _message.value = "已生成「${template.name}」，含 ${subs.size} 个子任务"
        }
    }

    /** 新建 / 编辑模板 */
    fun saveTemplate(
        id: String?,
        name: String,
        description: String,
        emoji: String,
        priority: String,
        categoryId: String?,
        tags: String,
        subtasks: String,
        dueInDays: Int?
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val existing = id?.let { templateDao.getTemplateById(it) }
            templateDao.upsertTemplate(
                TemplateEntity(
                    id = id ?: UUID.randomUUID().toString(),
                    name = name.trim(),
                    description = description.trim(),
                    emoji = emoji.ifBlank { "\uD83D\uDCCB" },
                    categoryId = categoryId,
                    tags = tags.trim(),
                    priority = priority,
                    subtasks = subtasks.trim(),
                    dueInDays = dueInDays,
                    builtIn = existing?.builtIn ?: false,
                    usageCount = existing?.usageCount ?: 0,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )
            )
            _message.value = if (id == null) "模板已创建" else "模板已更新"
        }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch {
            templateDao.deleteTemplateById(id)
            _message.value = "模板已删除"
        }
    }

    /** 把一个已有任务连同它的子任务存为模板 */
    fun createFromTodo(todoId: String) {
        viewModelScope.launch {
            val parent = todoDao.getTodoById(todoId) ?: return@launch
            val all = todoDao.getAllTodosList()
            val subs = all.filter { it.parentId == parent.id && !it.isDeleted }
                .sortedBy { it.createdAt }
                .joinToString("\n") { it.title }

            templateDao.upsertTemplate(
                TemplateEntity(
                    id = UUID.randomUUID().toString(),
                    name = parent.title,
                    description = parent.description,
                    categoryId = parent.categoryId,
                    tags = parent.tags,
                    priority = parent.priority,
                    subtasks = subs,
                    recurrenceRule = parent.recurrenceRule
                )
            )
            _message.value = "已存为模板「${parent.title}」"
        }
    }

    /** 内置模板库 */
    private fun builtInTemplates(): List<TemplateEntity> = listOf(
        TemplateEntity(
            id = "builtin_onboarding",
            name = "新员工入职流程",
            description = "从入职当天到第一周需要完成的全部事项",
            emoji = "\uD83C\uDF93",
            priority = "HIGH",
            dueInDays = 7,
            builtIn = true,
            subtasks = listOf(
                "签署劳动合同与保密协议",
                "领取电脑、工牌与门禁",
                "配置邮箱、IM 与内部系统账号",
                "阅读员工手册与团队 Wiki",
                "与直属主管 1:1 对齐目标",
                "认识团队成员，记录分工",
                "完成第一个上手小任务"
            ).joinToString("\n")
        ),
        TemplateEntity(
            id = "builtin_trip",
            name = "出差打包清单",
            description = "出发前一晚逐项核对",
            emoji = "\u2708\uFE0F",
            priority = "MEDIUM",
            dueInDays = 1,
            builtIn = true,
            subtasks = listOf(
                "身份证 / 护照",
                "行程单与酒店预订确认",
                "电脑、充电器、充电宝",
                "换洗衣物与洗漱包",
                "常用药品",
                "现金与备用银行卡",
                "确认往返交通时间"
            ).joinToString("\n")
        ),
        TemplateEntity(
            id = "builtin_weekly_meeting",
            name = "周会准备",
            description = "每周例会前的固定准备动作",
            emoji = "\uD83D\uDCC5",
            priority = "MEDIUM",
            dueInDays = 1,
            recurrenceRule = "WEEKLY",
            builtIn = true,
            subtasks = listOf(
                "回顾上周任务完成情况",
                "整理本周关键进展与数据",
                "列出需要对齐的问题与风险",
                "拟定下周计划",
                "提前发送会议议程"
            ).joinToString("\n")
        ),
        TemplateEntity(
            id = "builtin_release",
            name = "版本发布检查",
            description = "发版前的质量与合规检查",
            emoji = "\uD83D\uDE80",
            priority = "URGENT",
            dueInDays = 2,
            builtIn = true,
            subtasks = listOf(
                "回归核心功能",
                "确认版本号与更新日志",
                "检查崩溃率与关键埋点",
                "准备灰度与回滚方案",
                "同步发布公告"
            ).joinToString("\n")
        ),
        TemplateEntity(
            id = "builtin_weekly_review",
            name = "每周复盘",
            description = "周末花 20 分钟整理这一周",
            emoji = "\uD83E\uDDE0",
            priority = "MEDIUM",
            dueInDays = 0,
            recurrenceRule = "WEEKLY",
            builtIn = true,
            subtasks = listOf(
                "清理过期未完成的任务",
                "总结做成的三件事",
                "记录一个可改进点",
                "规划下周三个重点",
                "整理收集箱到零"
            ).joinToString("\n")
        ),
        TemplateEntity(
            id = "builtin_grocery",
            name = "日常采购",
            description = "去超市前先勾一遍",
            emoji = "\uD83D\uDED2",
            priority = "LOW",
            dueInDays = 2,
            builtIn = true,
            subtasks = listOf(
                "蔬菜水果",
                "肉蛋奶",
                "主食与米面",
                "日用清洁",
                "饮用水与饮料"
            ).joinToString("\n")
        )
    )
}
