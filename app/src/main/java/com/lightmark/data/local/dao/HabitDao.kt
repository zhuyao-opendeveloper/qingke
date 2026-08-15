package com.lightmark.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lightmark.data.local.entity.GoalEntity
import com.lightmark.data.local.entity.HabitCheckEntity
import com.lightmark.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

/**
 * 习惯 / 打卡 / 目标 DAO
 */
@Dao
interface HabitDao {

    // ---------- 习惯 ----------

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY sortOrder ASC, createdAt ASC")
    fun getActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY archived ASC, sortOrder ASC, createdAt ASC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: HabitEntity)

    @Query("UPDATE habits SET archived = :archived WHERE id = :id")
    suspend fun setHabitArchived(id: String, archived: Boolean)

    @Query("UPDATE habits SET paused = :paused WHERE id = :id")
    suspend fun setHabitPaused(id: String, paused: Boolean)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: String)

    // ---------- 打卡 ----------

    @Query("SELECT * FROM habit_checks ORDER BY dayKey DESC")
    fun getAllChecks(): Flow<List<HabitCheckEntity>>

    @Query("SELECT * FROM habit_checks WHERE habitId = :habitId AND dayKey = :dayKey LIMIT 1")
    suspend fun getCheck(habitId: String, dayKey: Int): HabitCheckEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCheck(check: HabitCheckEntity)

    @Query("DELETE FROM habit_checks WHERE habitId = :habitId AND dayKey = :dayKey")
    suspend fun deleteCheck(habitId: String, dayKey: Int)

    @Query("DELETE FROM habit_checks WHERE habitId = :habitId")
    suspend fun deleteChecksForHabit(habitId: String)

    // ---------- 目标 ----------

    @Query("SELECT * FROM goals ORDER BY completed ASC, createdAt DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: String): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: String)

    // 一键清空（#102）
    @Query("DELETE FROM habits")
    suspend fun clearHabits()

    @Query("DELETE FROM habit_checks")
    suspend fun clearChecks()

    @Query("DELETE FROM goals")
    suspend fun clearGoals()
}
