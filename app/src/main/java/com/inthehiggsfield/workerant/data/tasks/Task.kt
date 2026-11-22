package com.inthehiggsfield.workerant.data.tasks

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row in the "tasks" table. Room generates the SQL table from this class.
 * A task with scheduledStart == null is "pending" (not on the calendar yet).
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val scheduledStart: Long? = null,   // epoch millis
    val durationMinutes: Int? = null,
    val location: String? = null,
)
