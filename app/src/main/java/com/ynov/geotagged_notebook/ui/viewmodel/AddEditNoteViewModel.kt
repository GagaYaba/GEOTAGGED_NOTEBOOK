package com.ynov.geotagged_notebook.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ynov.geotagged_notebook.data.Note
import com.ynov.geotagged_notebook.data.NoteDraftStore
import com.ynov.geotagged_notebook.data.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddEditNoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NoteRepository.getInstance(application)
    private val draftStore = NoteDraftStore(application)

    private val _note = MutableStateFlow<Note?>(null)
    val note: StateFlow<Note?> = _note.asStateFlow()

    private val _restoredDraft = MutableStateFlow<Note?>(null)
    val restoredDraft: StateFlow<Note?> = _restoredDraft.asStateFlow()

    fun loadNoteAndDraft(noteId: Long) {
        viewModelScope.launch {
            val existingNote = if (noteId > 0L) repository.getNoteById(noteId) else null
            _note.value = existingNote
            _restoredDraft.value = draftStore.load(noteId)
        }
    }

    suspend fun saveNote(note: Note): Boolean {
        return try {
            if (note.id == 0L) {
                repository.insertNote(note)
            } else {
                repository.updateNote(note)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteNote(id: Long): Boolean {
        return try {
            repository.deleteNote(id)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun saveDraft(note: Note) {
        draftStore.save(note)
    }

    fun clearDraft(noteId: Long) {
        draftStore.clear(noteId)
    }
}
