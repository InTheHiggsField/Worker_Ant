package com.inthehiggsfield.workerant.data.tasks

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.inthehiggsfield.workerant.data.calendar.HiddenInstance
import com.inthehiggsfield.workerant.data.calendar.HiddenInstanceDao
import com.inthehiggsfield.workerant.data.notes.Note
import com.inthehiggsfield.workerant.data.notes.NoteDao

/** The app's local SQLite database. Bump [version] (and add a migration) if you change an entity's fields. */
@Database(
    entities = [Task::class, TaskEvent::class, Note::class, HiddenInstance::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao
    abstract fun hiddenInstanceDao(): HiddenInstanceDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /**
         * v1 -> v2: adds scheduling columns to tasks and the task_events table.
         * Keeps the tasks you already entered instead of wiping the database.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN scheduledStart INTEGER")
                db.execSQL("ALTER TABLE tasks ADD COLUMN durationMinutes INTEGER")
                db.execSQL("ALTER TABLE tasks ADD COLUMN location TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `task_events` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`taskId` INTEGER NOT NULL, " +
                        "`calendarId` INTEGER NOT NULL, " +
                        "`eventId` INTEGER NOT NULL)"
                )
            }
        }

        /** v2 -> v3: adds the notes table and the hidden calendar occurrences table. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notes` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`body` TEXT NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `hidden_instances` (" +
                        "`eventId` INTEGER NOT NULL, " +
                        "`begin` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`eventId`, `begin`))"
                )
            }
        }

        /** v3 -> v4: repeating tasks and Canvas import fields. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN repeat TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN repeatUntil INTEGER")
                db.execSQL("ALTER TABLE tasks ADD COLUMN canvasId TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN dueAt INTEGER")
                db.execSQL("ALTER TABLE tasks ADD COLUMN course TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN url TEXT")
            }
        }

        /** One shared database for the whole app. */
        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workerant.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build().also { instance = it }
            }
    }
}
