package com.inthehiggsfield.workerant.blocker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.inthehiggsfield.workerant.MainActivity
import com.inthehiggsfield.workerant.R

/** Posts the "Worker Ant blocked …" notification. */
object BlockNotifier {
    private const val CHANNEL_ID = "blocker"

    /** On Android 13+ the user has to allow notifications; older versions allow them by default. */
    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Returns false if notifications aren't allowed (the service falls back to a toast). */
    fun notifyBlocked(context: Context, target: BlockTarget, countToday: Int): Boolean {
        if (!canNotify(context)) return false
        ensureChannel(context)

        // Tapping the notification opens WorkerAnt.
        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val times = if (countToday == 1) "1 time" else "$countToday times"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_worker_ant)
            .setContentTitle("Worker Ant blocked ${target.label}")
            .setContentText("Back to work. Blocked $times today.")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        try {
            // One notification per target: a new block replaces the old one instead of piling up.
            NotificationManagerCompat.from(context).notify(target.ordinal + 1, notification)
        } catch (_: SecurityException) {
            return false
        }
        return true
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Doomscroll blocks", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Shown when WorkerAnt blocks a doomscrolling feature" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
