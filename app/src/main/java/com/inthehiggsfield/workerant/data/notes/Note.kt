package com.inthehiggsfield.workerant.data.notes

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A personal note in the Notes tab. */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val updatedAt: Long = System.currentTimeMillis(),
)
