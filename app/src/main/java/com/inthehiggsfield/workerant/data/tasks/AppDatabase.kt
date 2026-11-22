package com.inthehiggsfield.workerant.data.tasks

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** The app's local SQLite database. Bump [version] (and add a migration) if you change an entity's fields. */
@Database(entities = [Task::class, TaskEvent::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

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

        /** One shared database for the whole app. */
        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workerant.db",
                ).addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
            }
    }
}
