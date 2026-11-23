package com.inthehiggsfield.workerant.data.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Instances
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone

enum class CalendarSource(val label: String) { Google("Google"), Outlook("Outlook"), Other("Other") }

/** A calendar that exists on the phone (synced by the Google or Outlook app). */
data class DeviceCalendar(
    val id: Long,
    val name: String,
    val accountName: String,
    val source: CalendarSource,
    val color: Int,
    val isPrimary: Boolean,
    val writable: Boolean,
)

/** One occurrence of an event (recurring events show up once per occurrence). */
data class CalendarEvent(
    val eventId: Long,
    val calendarId: Long,
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val location: String?,
    val color: Int,
    val source: CalendarSource,
)

/**
 * Talks to Android's shared calendar database (CalendarContract). The Google and
 * Outlook apps sync their calendars into it, so reading/writing here is how
 * WorkerAnt reaches both without logging in to either service.
 * Every call needs the READ_CALENDAR / WRITE_CALENDAR permissions, otherwise it throws SecurityException.
 */
class CalendarRepository(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    suspend fun calendars(): List<DeviceCalendar> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.ACCOUNT_NAME,
            Calendars.ACCOUNT_TYPE,
            Calendars.CALENDAR_COLOR,
            Calendars.IS_PRIMARY,
            Calendars.CALENDAR_ACCESS_LEVEL,
            Calendars.OWNER_ACCOUNT,
        )
        val result = mutableListOf<DeviceCalendar>()
        resolver.query(Calendars.CONTENT_URI, projection, "${Calendars.VISIBLE} = 1", null, null)?.use { c ->
            while (c.moveToNext()) {
                val accountName = c.getString(2) ?: ""
                val accountType = c.getString(3) ?: ""
                val owner = c.getString(7)
                result += DeviceCalendar(
                    id = c.getLong(0),
                    name = c.getString(1) ?: accountName,
                    accountName = accountName,
                    source = sourceOf(accountType, accountName),
                    color = c.getInt(4),
                    isPrimary = c.getInt(5) == 1 || owner == accountName,
                    writable = c.getInt(6) >= Calendars.CAL_ACCESS_CONTRIBUTOR,
                )
            }
        }
        result
    }

    /** All event occurrences between [from] and [to] (epoch millis) across every visible calendar. */
    suspend fun events(from: Long, to: Long, calendarsById: Map<Long, DeviceCalendar>): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            val uri = Instances.CONTENT_URI.buildUpon().also {
                ContentUris.appendId(it, from)
                ContentUris.appendId(it, to)
            }.build()
            val projection = arrayOf(
                Instances.EVENT_ID,
                Instances.CALENDAR_ID,
                Instances.TITLE,
                Instances.BEGIN,
                Instances.END,
                Instances.ALL_DAY,
                Instances.EVENT_LOCATION,
                Instances.DISPLAY_COLOR,
            )
            val result = mutableListOf<CalendarEvent>()
            resolver.query(uri, projection, "${Instances.VISIBLE} = 1", null, "${Instances.BEGIN} ASC")?.use { c ->
                while (c.moveToNext()) {
                    val calendarId = c.getLong(1)
                    result += CalendarEvent(
                        eventId = c.getLong(0),
                        calendarId = calendarId,
                        title = c.getString(2) ?: "",
                        begin = c.getLong(3),
                        end = c.getLong(4),
                        allDay = c.getInt(5) == 1,
                        location = c.getString(6)?.takeIf { it.isNotBlank() },
                        color = c.getInt(7),
                        source = calendarsById[calendarId]?.source ?: CalendarSource.Other,
                    )
                }
            }
            result
        }

    /**
     * Creates an event and returns its id, or null if the insert failed.
     * Pass [rrule] (e.g. "FREQ=WEEKLY") to make it a repeating series; Android requires
     * repeating events to use a DURATION instead of an end time.
     */
    suspend fun insertEvent(
        calendarId: Long,
        title: String,
        start: Long,
        end: Long,
        location: String?,
        rrule: String? = null,
    ): Long? =
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(Events.CALENDAR_ID, calendarId)
                put(Events.TITLE, title)
                put(Events.DTSTART, start)
                if (rrule == null) {
                    put(Events.DTEND, end)
                } else {
                    put(Events.RRULE, rrule)
                    put(Events.DURATION, "PT${(end - start) / 60_000}M")
                }
                put(Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                if (location != null) put(Events.EVENT_LOCATION, location)
            }
            resolver.insert(Events.CONTENT_URI, values)?.let { ContentUris.parseId(it) }
        }

    suspend fun deleteEvent(eventId: Long) {
        withContext(Dispatchers.IO) {
            resolver.delete(ContentUris.withAppendedId(Events.CONTENT_URI, eventId), null, null)
        }
    }

    private fun sourceOf(accountType: String, accountName: String): CalendarSource {
        val type = accountType.lowercase()
        val name = accountName.lowercase()
        return when {
            "google" in type -> CalendarSource.Google
            "microsoft" in type || "outlook" in type || "exchange" in type -> CalendarSource.Outlook
            name.endsWith("@outlook.com") || name.endsWith("@hotmail.com") || name.endsWith("@live.com") ->
                CalendarSource.Outlook
            else -> CalendarSource.Other
        }
    }
}
