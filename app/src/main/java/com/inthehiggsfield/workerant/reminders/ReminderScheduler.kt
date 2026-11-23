package com.inthehiggsfield.workerant.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import com.inthehiggsfield.workerant.data.tasks.AppDatabase
import com.inthehiggsfield.workerant.data.tasks.Task
import com.inthehiggsfield.workerant.data.tasks.occurrenceEndingAfter

/**
 * Schedules a "starts in 30 minutes" notification for each upcoming scheduled task
 * (for repeating tasks, the next occurrence). Uses AlarmManager so it fires even when
 * WorkerAnt is closed.
 */
object ReminderScheduler {
    const val LEAD_MINUTES = 30
    private const val LEAD_MS = LEAD_MINUTES * 60_000L
    const val EXTRA_TASK_ID = "taskId"
    const val EXTRA_START = "start"

    private fun prefs(context: Context) = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)

    /** On/off switch in Settings (on by default). */
    fun isEnabled(context: Context) = prefs(context).getBoolean("enabled", true)

    suspend fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean("enabled", enabled) }
        rescheduleAll(context)
    }

    /** Android 12+ asks the user to allow on-time ("exact") alarms; otherwise they may run a few minutes late. */
    fun canUseExactAlarms(context: Context): Boolean {
        val alarms = context.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
    }

    /** Cancels every reminder WorkerAnt set, then sets one per upcoming task. */
    suspend fun rescheduleAll(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val previous = prefs(context).getStringSet("ids", emptySet()).orEmpty()
        previous.forEach { id -> alarms.cancel(pendingIntent(context, id.toLong(), 0L)) }

        if (!isEnabled(context)) {
            prefs(context).edit { putStringSet("ids", emptySet()) }
            return
        }

        val now = System.currentTimeMillis()
        val armed = mutableSetOf<String>()
        for (task in AppDatabase.get(context).taskDao().scheduledNow()) {
            val start = nextRemindableStart(task, now) ?: continue
            val intent = pendingIntent(context, task.id, start)
            val at = start - LEAD_MS
            if (canUseExactAlarms(context)) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            }
            armed += task.id.toString()
        }
        prefs(context).edit { putStringSet("ids", armed) }
    }

    /** The next start time that's still more than 30 minutes away (null if none). */
    private fun nextRemindableStart(task: Task, now: Long): Long? {
        val durationMs = (task.durationMinutes ?: 60) * 60_000L
        var start = task.occurrenceEndingAfter(now) ?: return null
        var guard = 0
        while (start - LEAD_MS <= now && guard++ < 1000) {
            if (task.repeatRule == null) return null   // one-time task already within 30 minutes
            start = task.occurrenceEndingAfter(start + durationMs) ?: return null
        }
        return start
    }

    private fun pendingIntent(context: Context, taskId: Long, start: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_TASK_ID, taskId)
            .putExtra(EXTRA_START, start)
        return PendingIntent.getBroadcast(
            context, taskId.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
