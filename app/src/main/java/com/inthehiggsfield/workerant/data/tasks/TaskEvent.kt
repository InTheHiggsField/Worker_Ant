package com.inthehiggsfield.workerant.data.tasks

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Links a task to a calendar event it created. A task scheduled into both
 * Google and Outlook has two rows here (one per calendar event).
 */
@Entity(tableName = "task_events")
data class TaskEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val calendarId: Long,
    val eventId: Long,
)
