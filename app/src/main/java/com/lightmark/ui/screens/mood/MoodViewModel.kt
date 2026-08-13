package com.lightmark.ui.screens.mood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightmark.data.local.dao.MoodDao
import com.lightmark.data.local.entity.MoodEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 心情记录 ViewModel（#122）
 */
@HiltViewModel
class MoodViewModel @Inject constructor(
    private val moodDao: MoodDao
) : ViewModel() {

    val moods: StateFlow<List<MoodEntity>> = moodDao.getAllMoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addMood(score: Int, note: String) {
        viewModelScope.launch {
            moodDao.insertMood(
                MoodEntity(
                    id = "mood_${System.currentTimeMillis()}_${(0..9999).random()}",
                    score = score,
                    note = note.trim(),
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteMood(mood: MoodEntity) {
        viewModelScope.launch { moodDao.deleteMood(mood) }
    }
}
