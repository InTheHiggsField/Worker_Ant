package com.inthehiggsfield.workerant.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.inthehiggsfield.workerant.MainActivity
import com.inthehiggsfield.workerant.R
import com.inthehiggsfield.workerant.TaskChanges
import com.inthehiggsfield.workerant.data.tasks.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Fires 30 minutes before a task starts: shows the reminder, then arms the next one. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1L)
        val start = intent.getLongExtra(ReminderScheduler.EXTRA_START, 0L)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = AppDatabase.get(context).taskDao().byId(taskId)
                if (task != null && !task.isDone) show(context, task.id, task.title, task.location, start)
                TaskChanges.onTasksChanged(context)   // arms the next occurrence for repeating tasks
            } finally {
                pending.finish()
            }
        }
    }

    private fun show(context: Context, taskId: Long, title: String, location: String?, start: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Task reminders", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "30 minutes before a scheduled task starts" }
        )
        val time = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("h:mm a"))
        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_worker_ant)
            .setContentTitle("In ${ReminderScheduler.LEAD_MINUTES} minutes: $title")
            .setContentText(listOfNotNull("Starts at $time", location).joinToString(" · "))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            // IDs 1000+ so they never collide with the blocker's notifications.
            NotificationManagerCompat.from(context).notify(1000 + taskId.toInt(), notification)
        } catch (_: SecurityException) {
        }
    }

    private companion object {
        const val CHANNEL_ID = "reminders"
    }
}
