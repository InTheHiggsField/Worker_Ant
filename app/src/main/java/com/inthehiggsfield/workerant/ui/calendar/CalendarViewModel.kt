package com.inthehiggsfield.workerant.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inthehiggsfield.workerant.data.calendar.CalendarEvent
import com.inthehiggsfield.workerant.data.calendar.CalendarRepository
import com.inthehiggsfield.workerant.data.calendar.CalendarSource
import com.inthehiggsfield.workerant.data.calendar.DeviceCalendar
import com.inthehiggsfield.workerant.data.tasks.AppDatabase
import com.inthehiggsfield.workerant.data.tasks.Task
import com.inthehiggsfield.workerant.data.tasks.TaskEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** One row in the agenda. A task scheduled into both calendars becomes a single item with two sources. */
data class AgendaItem(
    val key: String,
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val location: String?,
    val color: Int,
    val sources: List<CalendarSource>,
    val taskId: Long?,
) {
    /** All-day events are stored at UTC midnight, so read their date in UTC. */
    val date: LocalDate
        get() = Instant.ofEpochMilli(begin)
            .atZone(if (allDay) ZoneOffset.UTC else ZoneId.systemDefault())
            .toLocalDate()

    /** True if this event is on [day] (handles multi-day and all-day events). */
    fun occursOn(day: LocalDate): Boolean =
        if (allDay) {
            val start = Instant.ofEpochMilli(begin).atZone(ZoneOffset.UTC).toLocalDate()
            val endExclusive = Instant.ofEpochMilli(end).atZone(ZoneOffset.UTC).toLocalDate()
            !day.isBefore(start) && (day.isBefore(endExclusive) || day == start)
        } else {
            val zone = ZoneId.systemDefault()
            val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            begin < dayEnd && (end > dayStart || begin == dayStart)
        }
}

class CalendarViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).taskDao()
    private val repo = CalendarRepository(app)

    private val _calendars = MutableStateFlow<List<DeviceCalendar>>(emptyList())
    val calendars: StateFlow<List<DeviceCalendar>> = _calendars

    private val events = MutableStateFlow<List<CalendarEvent>>(emptyList())

    val pending: StateFlow<List<Task>> = dao.observePending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Every event in the range the screen is currently showing. */
    val items: StateFlow<List<AgendaItem>> = combine(events, dao.observeTaskEvents(), ::buildAgenda)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var range: Pair<LocalDate, LocalDate>? = null
    private var refreshJob: Job? = null

    /** Called by the screen whenever the visible dates change (new month, next day, etc.). */
    fun showRange(from: LocalDate, toExclusive: LocalDate) {
        if (range == from to toExclusive) return
        range = from to toExclusive
        refresh()
    }

    /** Re-reads calendars and the visible range's events from the phone's calendar database. */
    fun refresh() {
        val (from, to) = range ?: return
        refreshJob?.cancel()  // if you flip months quickly, drop the stale load
        refreshJob = viewModelScope.launch {
            try {
                val cals = repo.calendars()
                _calendars.value = cals
                val zone = ZoneId.systemDefault()
                // One extra day on each side so all-day events (stored in UTC) aren't cut off.
                val start = from.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                events.value = repo.events(start, end, cals.associateBy { it.id })
            } catch (_: SecurityException) {
                // Calendar permission not granted (or revoked); the screen shows the permission prompt.
            }
        }
    }

    /** Creates one event per selected calendar, links them to the task, and marks the task scheduled. */
    fun schedule(task: Task, start: Long, durationMinutes: Int, location: String?, calendarIds: List<Long>) {
        viewModelScope.launch {
            try {
                val end = start + durationMinutes * 60_000L
                for (calendarId in calendarIds) {
                    val eventId = repo.insertEvent(calendarId, task.title, start, end, location) ?: continue
                    dao.insertTaskEvent(TaskEvent(taskId = task.id, calendarId = calendarId, eventId = eventId))
                }
                dao.update(task.copy(scheduledStart = start, durationMinutes = durationMinutes, location = location))
                refresh()
            } catch (_: SecurityException) {
            }
        }
    }

    private fun buildAgenda(events: List<CalendarEvent>, links: List<TaskEvent>): List<AgendaItem> {
        val taskByEvent = links.associate { it.eventId to it.taskId }
        val (linked, unlinked) = events.partition { it.eventId in taskByEvent }

        val plain = unlinked.map {
            AgendaItem("e${it.eventId}-${it.begin}", it.title, it.begin, it.end, it.allDay,
                it.location, it.color, listOf(it.source), taskId = null)
        }
        // Collapse a task's Google + Outlook copies into one row.
        val fromTasks = linked.groupBy { taskByEvent.getValue(it.eventId) to it.begin }
            .map { (key, copies) ->
                val first = copies.first()
                AgendaItem("t${key.first}-${key.second}", first.title, first.begin, first.end, first.allDay,
                    first.location, first.color, copies.map { it.source }.distinct(), taskId = key.first)
            }
        return (plain + fromTasks).sortedBy { it.begin }
    }
}
