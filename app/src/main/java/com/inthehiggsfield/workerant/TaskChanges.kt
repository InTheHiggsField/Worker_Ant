package com.inthehiggsfield.workerant

import android.content.Context
import com.inthehiggsfield.workerant.reminders.ReminderScheduler
import com.inthehiggsfield.workerant.widget.WorkerAntWidget

/**
 * Call after anything that changes tasks (add, schedule, check off, delete, Canvas sync).
 * Re-arms the 30-minute reminders and refreshes the home screen widget.
 */
object TaskChanges {
    suspend fun onTasksChanged(context: Context) {
        val app = context.applicationContext
        runCatching { ReminderScheduler.rescheduleAll(app) }
        runCatching { WorkerAntWidget.refresh(app) }
    }
}
