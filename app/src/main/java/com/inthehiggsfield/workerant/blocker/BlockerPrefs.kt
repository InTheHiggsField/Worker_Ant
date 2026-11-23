package com.inthehiggsfield.workerant.blocker

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.core.content.edit
import java.time.LocalDate

/** When the blocker is allowed to block. */
enum class BlockMode {
    /** Block the selected feeds any time they open. */
    Always,
    /**
     * Reactive mode: only block while you're busy — during an Outlook calendar event
     * (Google is skipped because that's where holiday calendars come from) or one of
     * WorkerAnt's scheduled tasks.
     */
    Reactive,
}

/**
 * Blocker settings, shared between the Blocker screen (which writes them)
 * and the accessibility service (which reads them on every event).
 */
class BlockerPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("blocker", Context.MODE_PRIVATE)

    fun isEnabled(target: BlockTarget): Boolean = prefs.getBoolean("block_${target.name}", false)

    fun setEnabled(target: BlockTarget, enabled: Boolean) {
        prefs.edit { putBoolean("block_${target.name}", enabled) }
    }

    /** Counts today's blocks for [target] and returns the new total (resets each day). */
    fun recordBlock(target: BlockTarget): Int {
        val key = "count_${LocalDate.now()}_${target.name}"
        val count = prefs.getInt(key, 0) + 1
        prefs.edit { putInt(key, count) }
        return count
    }

    var mode: BlockMode
        get() = when (val saved = prefs.getString("mode", null)) {
            "DuringScheduledTasks" -> BlockMode.Reactive   // name used by an earlier build
            else -> BlockMode.entries.firstOrNull { it.name == saved } ?: BlockMode.Always
        }
        set(value) = prefs.edit { putString("mode", value.name) }

    /** Developer option: log every view ID on the screen to Logcat (tag "WorkerAntBlocker"). */
    var logViewIds: Boolean
        get() = prefs.getBoolean("log_view_ids", false)
        set(value) = prefs.edit { putBoolean("log_view_ids", value) }
}

/** True if you've switched WorkerAnt on under Settings → Accessibility. */
fun isBlockerServiceEnabled(context: Context): Boolean {
    val expected = ComponentName(context, DoomscrollBlockerService::class.java)
    val enabled = Settings.Secure.getString(
        context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
}
