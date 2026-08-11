package com.lightmark.ui.screens.smartlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.local.dao.CategoryDao
import com.lightmark.data.repository.TodoRepository
import com.lightmark.domain.model.Category
import com.lightmark.domain.model.SmartList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 自定义智能清单管理 ViewModel（#28）
 *
 * 保存 / 删除命名筛选条件（快速筛选 / 分类 / 关键词 / 排序）。纯本地。
 */
@HiltViewModel
class SmartListsViewModel @Inject constructor(
    private val repository: TodoRepository,
    private val categoryDao: CategoryDao
) : ViewModel() {

    val smartLists: StateFlow<List<SmartList>> = repository.getSmartLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryDao.getAllCategories()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(list: SmartList) {
        viewModelScope.launch { repository.saveSmartList(list) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.deleteSmartList(id) }
    }
}
