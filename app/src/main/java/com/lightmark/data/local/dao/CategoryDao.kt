package com.lightmark.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lightmark.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 分类 DAO
 */
@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY createdAt DESC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)

    @Query("DELETE FROM categories")
    suspend fun clearAll()

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesList(): List<CategoryEntity>
}
