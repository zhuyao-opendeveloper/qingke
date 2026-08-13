package com.lightmark.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 心情记录实体（#122）
 */
@Entity(tableName = "moods")
data class MoodEntity(
    @PrimaryKey val id: String,
    val score: Int, // 1-5，分数越高心情越好
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
