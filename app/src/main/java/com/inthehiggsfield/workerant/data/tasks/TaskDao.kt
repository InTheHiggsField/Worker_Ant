package com.inthehiggsfield.workerant.data.tasks

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** The queries the app can run against the database. Room writes the implementation at build time. */
@Dao
interface TaskDao {
    // Returns a Flow, so the UI updates automatically whenever the table changes.
    // Unfinished tasks first, newest first.
    @Query("SELECT * FROM tasks ORDER BY isDone ASC, createdAt DESC")
    fun observeAll(): Flow<List<Task>>

    /** Unfinished tasks that haven't been given a time yet. */
    @Query("SELECT * FROM tasks WHERE scheduledStart IS NULL AND isDone = 0 ORDER BY createdAt DESC")
    fun observePending(): Flow<List<Task>>

    @Insert
    suspend fun insert(task: Task)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    // --- task <-> calendar event links ---

    @Insert
    suspend fun insertTaskEvent(link: TaskEvent)

    @Query("SELECT * FROM task_events")
    fun observeTaskEvents(): Flow<List<TaskEvent>>

    @Query("SELECT * FROM task_events WHERE taskId = :taskId")
    suspend fun eventsForTask(taskId: Long): List<TaskEvent>

    @Query("DELETE FROM task_events WHERE taskId = :taskId")
    suspend fun deleteEventsForTask(taskId: Long)
}
