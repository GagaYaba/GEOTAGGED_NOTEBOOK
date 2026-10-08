package com.ynov.geotagged_notebook.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ynov.geotagged_notebook.data.Note
import com.ynov.geotagged_notebook.data.NoteRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NoteListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NoteRepository.getInstance(application)

    val notes: StateFlow<List<Note>> = repository.notes

    init {
        viewModelScope.launch {
            repository.refreshNotes()
        }
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch {
            repository.updateNote(
                note.copy(
                    isFavorite = !note.isFavorite,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshNotes()
        }
    }
}
