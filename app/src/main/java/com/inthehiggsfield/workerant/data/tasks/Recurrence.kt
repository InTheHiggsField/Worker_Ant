package com.inthehiggsfield.workerant.data.tasks

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** How often a scheduled task repeats. */
enum class Repeat(val label: String, private val rrule: String) {
    Daily("Every day", "FREQ=DAILY"),
    Weekdays("Every weekday (Mon–Fri)", "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"),
    Weekly("Every week", "FREQ=WEEKLY"),
    Biweekly("Every 2 weeks", "FREQ=WEEKLY;INTERVAL=2"),
    Monthly("Every month", "FREQ=MONTHLY");

    /** Short label for the task list, e.g. "Weekly". */
    val shortLabel: String get() = when (this) {
        Daily -> "Daily"; Weekdays -> "Weekdays"; Weekly -> "Weekly"; Biweekly -> "Every 2 weeks"; Monthly -> "Monthly"
    }

    /**
     * The iCalendar repeat rule Google and Outlook understand,
     * e.g. "FREQ=WEEKLY;UNTIL=20270515T045959Z".
     */
    fun toRRule(until: Long?): String {
        if (until == null) return rrule
        val utc = Instant.ofEpochMilli(until).atZone(ZoneOffset.UTC)
        return rrule + ";UNTIL=" + utc.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))
    }

    /** The occurrence after [time]. */
    fun next(time: ZonedDateTime): ZonedDateTime = when (this) {
        Daily -> time.plusDays(1)
        Weekdays -> {
            var t = time.plusDays(1)
            while (t.dayOfWeek == DayOfWeek.SATURDAY || t.dayOfWeek == DayOfWeek.SUNDAY) t = t.plusDays(1)
            t
        }
        Weekly -> time.plusWeeks(1)
        Biweekly -> time.plusWeeks(2)
        Monthly -> time.plusMonths(1)
    }
}

/**
 * Start time of this task's first occurrence that ends after [from] (epoch millis) —
 * i.e. the one happening now or the next one. Null if it's one-time and already over,
 * or the repeat has ended.
 */
fun Task.occurrenceEndingAfter(from: Long): Long? {
    val start = scheduledStart ?: return null
    val durationMs = (durationMinutes ?: 60) * 60_000L
    val rule = repeatRule ?: return start.takeIf { it + durationMs > from }
    val zone = ZoneId.systemDefault()
    var t = Instant.ofEpochMilli(start).atZone(zone)
    var guard = 0
    while (t.toInstant().toEpochMilli() + durationMs <= from && guard++ < 5000) t = rule.next(t)
    val millis = t.toInstant().toEpochMilli()
    return millis.takeIf { repeatUntil == null || it <= repeatUntil }
}

/** True if an occurrence of this (unfinished) task is happening at [now]. */
fun Task.isInProgress(now: Long = System.currentTimeMillis()): Boolean {
    if (isDone) return false
    val start = occurrenceEndingAfter(now) ?: return false
    return start <= now
}
