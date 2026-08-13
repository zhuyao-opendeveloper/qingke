package com.lightmark.data.local.dao

import androidx.room.*
import com.lightmark.data.local.entity.MoodEntity
import kotlinx.coroutines.flow.Flow

/**
 * 心情记录 DAO（#122）
 */
@Dao
interface MoodDao {
    @Query("SELECT * FROM moods ORDER BY created_at DESC")
    fun getAllMoods(): Flow<List<MoodEntity>>

    @Query("SELECT * FROM moods WHERE created_at >= :since ORDER BY created_at DESC")
    suspend fun getSince(since: Long): List<MoodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMood(mood: MoodEntity)

    @Delete
    suspend fun deleteMood(mood: MoodEntity)
}
