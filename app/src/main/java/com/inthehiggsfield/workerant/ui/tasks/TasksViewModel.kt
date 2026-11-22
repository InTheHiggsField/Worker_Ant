package com.inthehiggsfield.workerant.ui.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inthehiggsfield.workerant.data.calendar.CalendarRepository
import com.inthehiggsfield.workerant.data.tasks.AppDatabase
import com.inthehiggsfield.workerant.data.tasks.Task
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Sits between the screen and the database. It survives screen rotation,
 * and runs database work off the main thread via viewModelScope.
 */
class TasksViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).taskDao()
    private val calendarRepo = CalendarRepository(app)

    val tasks: StateFlow<List<Task>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { dao.insert(Task(title = trimmed)) }
    }

    fun toggle(task: Task) {
        viewModelScope.launch { dao.update(task.copy(isDone = !task.isDone)) }
    }

    /** Deletes the task and any Google/Outlook events it created. */
    fun delete(task: Task) {
        viewModelScope.launch {
            dao.eventsForTask(task.id).forEach { link ->
                // runCatching: if calendar permission was revoked, still delete the task itself.
                runCatching { calendarRepo.deleteEvent(link.eventId) }
            }
            dao.deleteEventsForTask(task.id)
            dao.delete(task)
        }
    }
}
