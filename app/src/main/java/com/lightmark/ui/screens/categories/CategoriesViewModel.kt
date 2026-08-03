package com.lightmark.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.local.dao.CategoryDao
import com.lightmark.data.local.dao.TodoDao
import com.lightmark.data.local.entity.CategoryEntity
import com.lightmark.domain.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 分类管理 ViewModel
 *
 * 提供分类列表与每个分类下的待办数量，
 * 支持新增 / 改名 / 改色 / 删除。
 */
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryDao: CategoryDao,
    private val todoDao: TodoDao
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryDao.getAllCategories()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val counts: StateFlow<Map<String, Int>> = todoDao.getAllTodos()
        .map { list ->
            list.mapNotNull { it.categoryId }
                .groupingBy { it }
                .eachCount()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun addCategory(name: String, color: Long) {
        if (name.isBlank()) return
        viewModelScope.launch {
            categoryDao.insertCategory(
                CategoryEntity(
                    id = Category.generateId(),
                    name = name.trim(),
                    color = color
                )
            )
        }
    }

    fun updateCategory(category: Category, name: String, color: Long) {
        viewModelScope.launch {
            categoryDao.updateCategory(
                CategoryEntity.fromDomain(
                    category.copy(name = name.trim(), color = color)
                )
            )
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.deleteCategory(CategoryEntity.fromDomain(category))
        }
    }
}
