package com.inthehiggsfield.workerant.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inthehiggsfield.workerant.data.calendar.CalendarEvent
import com.inthehiggsfield.workerant.data.calendar.CalendarRepository
import com.inthehiggsfield.workerant.data.calendar.CalendarSource
import com.inthehiggsfield.workerant.data.calendar.DeviceCalendar
import com.inthehiggsfield.workerant.data.calendar.HiddenInstance
import com.inthehiggsfield.workerant.TaskChanges
import com.inthehiggsfield.workerant.data.tasks.AppDatabase
import com.inthehiggsfield.workerant.data.tasks.Repeat
import com.inthehiggsfield.workerant.data.tasks.Task
import com.inthehiggsfield.workerant.data.tasks.TaskEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    /** (eventId, start time) of every calendar occurrence behind this row — used to hide it. */
    val instances: List<Pair<Long, Long>> = emptyList(),
    /** True if you've hidden this occurrence (only visible when "show hidden" is on). */
    val hidden: Boolean = false,
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

/**
 * Outlook's synced calendars often come through with a pale beige color that white text
 * can't be read on, so Outlook events use Outlook's own blue instead.
 */
private val OUTLOOK_BLUE = 0xFF0F6CBD.toInt()

class CalendarViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).taskDao()
    private val hiddenDao = AppDatabase.get(app).hiddenInstanceDao()
    private val repo = CalendarRepository(app)

    private val _calendars = MutableStateFlow<List<DeviceCalendar>>(emptyList())
    val calendars: StateFlow<List<DeviceCalendar>> = _calendars

    private val events = MutableStateFlow<List<CalendarEvent>>(emptyList())

    val pending: StateFlow<List<Task>> = dao.observePending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val showHidden = MutableStateFlow(false)
    val showingHidden: StateFlow<Boolean> = showHidden

    /** Every event in range, with hidden occurrences marked. */
    private val allItems = combine(events, dao.observeTaskEvents(), hiddenDao.observeAll()) { e, links, hidden ->
        buildAgenda(e, links, hidden.map { it.eventId to it.begin }.toSet())
    }

    /** What the screen shows: hidden occurrences are left out unless "show hidden" is on. */
    val items: StateFlow<List<AgendaItem>> = combine(allItems, showHidden) { list, show ->
        if (show) list else list.filterNot { it.hidden }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** How many occurrences in the visible range are hidden. */
    val hiddenCount: StateFlow<Int> = allItems.map { list -> list.count { it.hidden } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setShowHidden(show: Boolean) {
        showHidden.value = show
    }

    /** Hide just this occurrence (e.g. this week's canceled class). The real calendar isn't changed. */
    fun hide(item: AgendaItem) {
        viewModelScope.launch { item.instances.forEach { (id, begin) -> hiddenDao.hide(HiddenInstance(id, begin)) } }
    }

    fun unhide(item: AgendaItem) {
        viewModelScope.launch { item.instances.forEach { (id, begin) -> hiddenDao.unhide(id, begin) } }
    }

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

    /**
     * Creates one event (or one repeating event series) per selected calendar, links them to
     * the task, and marks the task scheduled. Then re-arms reminders and refreshes the widget.
     */
    fun schedule(
        task: Task,
        start: Long,
        durationMinutes: Int,
        location: String?,
        calendarIds: List<Long>,
        repeat: Repeat? = null,
        repeatUntil: Long? = null,
    ) {
        viewModelScope.launch {
            try {
                val end = start + durationMinutes * 60_000L
                val rrule = repeat?.toRRule(repeatUntil)
                for (calendarId in calendarIds) {
                    val eventId = repo.insertEvent(calendarId, task.title, start, end, location, rrule) ?: continue
                    dao.insertTaskEvent(TaskEvent(taskId = task.id, calendarId = calendarId, eventId = eventId))
                }
                dao.update(
                    task.copy(
                        scheduledStart = start, durationMinutes = durationMinutes, location = location,
                        repeat = repeat?.name, repeatUntil = repeatUntil,
                    )
                )
                TaskChanges.onTasksChanged(getApplication())
                refresh()
            } catch (_: SecurityException) {
            }
        }
    }

    private fun buildAgenda(
        events: List<CalendarEvent>,
        links: List<TaskEvent>,
        hidden: Set<Pair<Long, Long>>,
    ): List<AgendaItem> {
        val taskByEvent = links.associate { it.eventId to it.taskId }
        val (linked, unlinked) = events.partition { it.eventId in taskByEvent }
        fun colorOf(e: CalendarEvent) = if (e.source == CalendarSource.Outlook) OUTLOOK_BLUE else e.color

        val plain = unlinked.map {
            val instance = it.eventId to it.begin
            AgendaItem("e${it.eventId}-${it.begin}", it.title, it.begin, it.end, it.allDay,
                it.location, colorOf(it), listOf(it.source), taskId = null,
                instances = listOf(instance), hidden = instance in hidden)
        }
        // Collapse a task's Google + Outlook copies into one row.
        val fromTasks = linked.groupBy { taskByEvent.getValue(it.eventId) to it.begin }
            .map { (key, copies) ->
                val first = copies.first()
                val instances = copies.map { it.eventId to it.begin }
                AgendaItem("t${key.first}-${key.second}", first.title, first.begin, first.end, first.allDay,
                    first.location, colorOf(first), copies.map { it.source }.distinct(), taskId = key.first,
                    instances = instances, hidden = instances.all { it in hidden })
            }
        return (plain + fromTasks).sortedBy { it.begin }
    }
}
