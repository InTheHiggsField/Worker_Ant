package com.inthehiggsfield.workerant.ui.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inthehiggsfield.workerant.data.notes.Note
import com.inthehiggsfield.workerant.data.tasks.AppDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).noteDao()

    val notes: StateFlow<List<Note>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Saves when you leave the editor. A new note that's still empty isn't saved,
     * and an existing note you've cleared out is deleted.
     */
    fun save(existing: Note?, title: String, body: String) {
        val empty = title.isBlank() && body.isBlank()
        viewModelScope.launch {
            when {
                existing == null && empty -> Unit
                existing == null -> dao.insert(Note(title = title.trim(), body = body))
                empty -> dao.delete(existing)
                existing.title != title.trim() || existing.body != body ->
                    dao.update(existing.copy(title = title.trim(), body = body, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun delete(note: Note) {
        viewModelScope.launch { dao.delete(note) }
    }
}
