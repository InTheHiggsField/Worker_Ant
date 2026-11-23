package com.inthehiggsfield.workerant.ui.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inthehiggsfield.workerant.TaskChanges
import com.inthehiggsfield.workerant.canvas.CanvasSync
import com.inthehiggsfield.workerant.data.calendar.CalendarRepository
import com.inthehiggsfield.workerant.data.tasks.AppDatabase
import com.inthehiggsfield.workerant.data.tasks.Task
import com.inthehiggsfield.workerant.data.tasks.occurrenceEndingAfter
import kotlinx.coroutines.flow.MutableStateFlow
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

    /** Last Canvas sync problem to show on screen, or null. */
    val canvasError = MutableStateFlow<String?>(null)

    fun add(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            dao.insert(Task(title = trimmed))
            TaskChanges.onTasksChanged(getApplication())
        }
    }

    /**
     * Checks a task off (or un-checks it). For a repeating task, checking it off completes
     * this occurrence and moves it to the next one; it's only marked done when the repeat ends.
     */
    fun toggle(task: Task) {
        viewModelScope.launch {
            val rule = task.repeatRule
            if (rule != null && !task.isDone) {
                val durationMs = (task.durationMinutes ?: 60) * 60_000L
                val current = task.occurrenceEndingAfter(System.currentTimeMillis())
                val next = current?.let { task.occurrenceEndingAfter(it + durationMs) }
                dao.update(if (next != null) task.copy(scheduledStart = next) else task.copy(isDone = true))
            } else {
                dao.update(task.copy(isDone = !task.isDone))
            }
            TaskChanges.onTasksChanged(getApplication())
        }
    }

    /** Deletes the task and any Google/Outlook events (or event series) it created. */
    fun delete(task: Task) {
        viewModelScope.launch {
            dao.eventsForTask(task.id).forEach { link ->
                // runCatching: if calendar permission was revoked, still delete the task itself.
                runCatching { calendarRepo.deleteEvent(link.eventId) }
            }
            dao.deleteEventsForTask(task.id)
            dao.delete(task)
            TaskChanges.onTasksChanged(getApplication())
        }
    }

    /** Pull new Canvas to-dos when the Tasks tab opens (at most every 15 minutes). */
    fun syncCanvasIfStale() {
        viewModelScope.launch {
            canvasError.value = CanvasSync.syncIfStale(getApplication()).exceptionOrNull()?.message
        }
    }
}
