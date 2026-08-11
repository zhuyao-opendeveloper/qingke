package com.lightmark.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 习惯定义
 * periodDays: 1 = 每日习惯，7 = 每周习惯
 * targetPerPeriod: 每个周期内需要完成的次数
 */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String = "\u2705",
    val color: Long = 0xFF4CAF50,
    val periodDays: Int = 1,
    val targetPerPeriod: Int = 1,
    val note: String = "",
    val archived: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 习惯打卡记录
 * dayKey 采用 yyyyMMdd 整数，便于范围查询与排序
 */
@Entity(tableName = "habit_checks")
data class HabitCheckEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val dayKey: Int,
    val count: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 长期目标
 * milestones 使用简单的文本行存储：每行 "1|里程碑名称"（1 表示已完成）
 */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val targetValue: Double = 100.0,
    val currentValue: Double = 0.0,
    val unit: String = "%",
    val dueDate: Long? = null,
    val completed: Boolean = false,
    val milestones: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
