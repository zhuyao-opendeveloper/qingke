package com.lightmark.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lightmark.data.local.entity.SmartListEntity
import kotlinx.coroutines.flow.Flow

/**
 * 自定义智能清单 DAO（#28）
 */
@Dao
interface SmartListDao {

    @Query("SELECT * FROM smart_lists ORDER BY `order` ASC, name ASC")
    fun getAll(): Flow<List<SmartListEntity>>

    @Query("SELECT * FROM smart_lists")
    suspend fun getAllList(): List<SmartListEntity>

    @Query("SELECT * FROM smart_lists WHERE id = :id")
    suspend fun getById(id: String): SmartListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: SmartListEntity)

    @Update
    suspend fun update(list: SmartListEntity)

    @Delete
    suspend fun delete(list: SmartListEntity)

    @Query("DELETE FROM smart_lists WHERE id = :id")
    suspend fun deleteById(id: String)
}
