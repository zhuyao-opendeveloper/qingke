package com.lightmark.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lightmark.data.local.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

/**
 * 任务模板 DAO
 */
@Dao
interface TemplateDao {

    @Query("SELECT * FROM templates ORDER BY usageCount DESC, createdAt DESC")
    fun getAllTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getTemplateById(id: String): TemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplate(template: TemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(templates: List<TemplateEntity>)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteTemplateById(id: String)

    @Query("UPDATE templates SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsage(id: String)

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun countTemplates(): Int
}
