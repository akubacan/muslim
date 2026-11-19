package com.personal.doalibrary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.doalibrary.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dao: PrayerDao,
    private val repo: SettingsRepo
) : ViewModel() {
    val prayers = dao.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val trash = dao.observeTrash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings = repo.flow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiSettings())

    fun save(id: String?, title: String, arabic: String, translation: String, done: () -> Unit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val old = id?.let { dao.get(it) }
            if (old == null) {
                dao.upsert(PrayerEntity(title = title, arabicText = arabic, translation = translation,
                    sortOrder = dao.maxOrder() + 1, createdAt = now, updatedAt = now))
            } else {
                // ID dan posisi urutan tidak berubah saat edit.
                dao.upsert(old.copy(title = title, arabicText = arabic, translation = translation, updatedAt = now))
            }
            done()
        }
    }

    fun toggleFav(p: PrayerEntity) { viewModelScope.launch { dao.setFav(p.id, !p.isFavorite) } }
    fun moveToTrash(id: String) { viewModelScope.launch { dao.setDeleted(id, System.currentTimeMillis()) } }
    fun restore(id: String) { viewModelScope.launch { dao.restore(id) } }
    fun purge(id: String) { viewModelScope.launch { dao.purge(id) } }
    fun purgeAll() { viewModelScope.launch { dao.purgeAll() } }
    fun reorder(ids: List<String>) { viewModelScope.launch { dao.reorder(ids) } }
    fun setSettings(s: UiSettings) { viewModelScope.launch { repo.save(s) } }
}
