package com.lightmark.ui.screens.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.repository.TodoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 全局标签管理 ViewModel（#30）
 *
 * 列出所有标签，支持重命名 / 合并 / 删除（跨所有待办统一改写）。纯本地。
 */
@HiltViewModel
class TagManageViewModel @Inject constructor(
    private val repository: TodoRepository
) : ViewModel() {

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch { _tags.value = repository.getAllTags() }
    }

    /** 重命名标签（若新名为已有标签则等效于合并） */
    fun renameTag(oldTag: String, newTag: String) {
        viewModelScope.launch {
            repository.renameTag(oldTag, newTag.trim())
            reload()
        }
    }

    fun deleteTag(tag: String) {
        viewModelScope.launch {
            repository.deleteTag(tag)
            reload()
        }
    }
}
