package com.inthehiggsfield.workerant.data.tasks

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row in the "tasks" table. Room generates the SQL table from this class.
 * A task with scheduledStart == null is "pending" (not on the calendar yet).
 * For a repeating task, scheduledStart is its next (or current) occurrence.
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
    /** A [Repeat] name, or null for a one-time task. */
    val repeat: String? = null,
    /** Last day a repeating task can occur (epoch millis, end of that day), or null for forever. */
    val repeatUntil: Long? = null,
    /** Set for tasks imported from Canvas: the Canvas assignment's unique key. */
    val canvasId: String? = null,
    /** Canvas due date (epoch millis). */
    val dueAt: Long? = null,
    /** Canvas course name, e.g. "CSCE 120". */
    val course: String? = null,
    /** Link to the assignment in Canvas. */
    val url: String? = null,
) {
    val repeatRule: Repeat? get() = Repeat.entries.firstOrNull { it.name == repeat }
}
