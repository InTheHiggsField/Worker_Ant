package com.inthehiggsfield.workerant.data.calendar

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * One occurrence of a calendar event that you've hidden in WorkerAnt — e.g. this week's
 * class that got canceled. Identified by the event's id plus that occurrence's start time,
 * so the rest of a recurring series still shows. Only hidden in WorkerAnt; the real
 * Google/Outlook calendar isn't changed.
 */
@Entity(tableName = "hidden_instances", primaryKeys = ["eventId", "begin"])
data class HiddenInstance(
    val eventId: Long,
    val begin: Long,
)

@Dao
interface HiddenInstanceDao {
    @Query("SELECT * FROM hidden_instances")
    fun observeAll(): Flow<List<HiddenInstance>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun hide(instance: HiddenInstance)

    @Query("DELETE FROM hidden_instances WHERE eventId = :eventId AND begin = :begin")
    suspend fun unhide(eventId: Long, begin: Long)
}
