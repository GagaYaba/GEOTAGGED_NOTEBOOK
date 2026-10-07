package com.ynov.geotagged_notebook.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class NoteRepository private constructor(context: Context) {

    private val dbHelper = NoteDatabaseHelper(context.applicationContext)
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: NoteRepository? = null

        fun getInstance(context: Context): NoteRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = NoteRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun refreshNotes() {
        withContext(Dispatchers.IO) {
            val list = dbHelper.getAllNotes()
            _notes.value = list
        }
    }

    suspend fun getNoteById(id: Long): Note? {
        return withContext(Dispatchers.IO) {
            dbHelper.getNoteById(id)
        }
    }

    suspend fun insertNote(note: Note): Long {
        return withContext(Dispatchers.IO) {
            val id = dbHelper.insertNote(note)
            refreshNotes()
            id
        }
    }

    suspend fun updateNote(note: Note) {
        withContext(Dispatchers.IO) {
            dbHelper.updateNote(note)
            refreshNotes()
        }
    }

    suspend fun deleteNote(id: Long) {
        withContext(Dispatchers.IO) {
            dbHelper.deleteNote(id)
            refreshNotes()
        }
    }
}
