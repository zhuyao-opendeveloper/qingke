package com.lightmark.ui.screens.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.local.dao.InboxDao
import com.lightmark.data.local.entity.InboxEntity
import com.lightmark.domain.model.InboxItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val inboxDao: InboxDao
) : ViewModel() {

    val items: StateFlow<List<InboxItem>> = inboxDao.observeAll()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(content: String) {
        val text = content.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            inboxDao.insert(
                InboxEntity(id = UUID.randomUUID().toString(), content = text)
            )
        }
    }

    fun toggle(item: InboxItem) {
        viewModelScope.launch {
            inboxDao.update(InboxEntity.fromDomain(item.copy(isDone = !item.isDone)))
        }
    }

    fun remove(item: InboxItem) {
        viewModelScope.launch {
            inboxDao.delete(InboxEntity.fromDomain(item))
        }
    }
}
