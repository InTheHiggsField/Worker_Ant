package com.inthehiggsfield.workerant.blocker

import android.accessibilityservice.AccessibilityService
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.inthehiggsfield.workerant.data.calendar.CalendarEvent
import com.inthehiggsfield.workerant.data.calendar.CalendarRepository
import com.inthehiggsfield.workerant.data.calendar.CalendarSource
import com.inthehiggsfield.workerant.data.tasks.AppDatabase
import com.inthehiggsfield.workerant.data.tasks.Task
import com.inthehiggsfield.workerant.data.tasks.isInProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Watches the apps listed in res/xml/blocker_accessibility_config.xml.
 * When an enabled target's doomscroll screen is showing, it presses Back
 * (or Home, for apps that are entirely a feed) and posts a notification.
 * In reactive mode it only blocks while you're busy: during a timed Outlook calendar event
 * (that you haven't hidden) or one of WorkerAnt's scheduled tasks.
 *
 * You turn this on yourself in Settings → Accessibility → WorkerAnt.
 */
class DoomscrollBlockerService : AccessibilityService() {
    private lateinit var prefs: BlockerPrefs
    private var lastBlockAt = 0L
    private var lastLoggedAt = 0L

    // Kept up to date from the database so the check below never waits on disk.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var scheduledTasks: List<Task> = emptyList()
    private var outlookEvents: List<CalendarEvent> = emptyList()
    private var hiddenOccurrences: Set<Pair<Long, Long>> = emptySet()
    private val calendarRepo by lazy { CalendarRepository(this) }

    /** Re-reads the calendar whenever Google/Outlook sync changes it. */
    private val calendarObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            scope.launch { refreshCalendarEvents() }
        }
    }

    override fun onServiceConnected() {
        prefs = BlockerPrefs(this)
        val db = AppDatabase.get(this)
        scope.launch { db.taskDao().observeScheduled().collect { scheduledTasks = it } }
        scope.launch {
            db.hiddenInstanceDao().observeAll().collect { list -> hiddenOccurrences = list.map { it.eventId to it.begin }.toSet() }
        }
        // Also refresh once a minute, so the "now" window keeps moving.
        scope.launch {
            while (isActive) {
                refreshCalendarEvents()
                delay(60_000)
            }
        }
        contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, calendarObserver)
    }

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(calendarObserver)
        scope.cancel()
        super.onDestroy()
    }

    /**
     * Loads Outlook events from 12 hours ago to 12 hours ahead. Google events are skipped
     * (that's where holiday calendars come from), and so are all-day events, which would
     * otherwise block for the entire day.
     */
    private suspend fun refreshCalendarEvents() {
        outlookEvents = try {
            val now = System.currentTimeMillis()
            val calendars = calendarRepo.calendars()
            calendarRepo.events(now - HALF_DAY_MS, now + HALF_DAY_MS, calendars.associateBy { it.id })
                .filter { it.source == CalendarSource.Outlook && !it.allDay }
        } catch (_: SecurityException) {
            emptyList()  // calendar permission not granted
        }
    }

    /** What you're busy with right now (its title), or null if you're free. */
    private fun busyWith(): String? {
        taskInProgress()?.let { return it.title }
        val now = System.currentTimeMillis()
        val event = outlookEvents.firstOrNull {
            now >= it.begin && now < it.end && (it.eventId to it.begin) !in hiddenOccurrences
        } ?: return null
        return event.title.ifBlank { "a calendar event" }
    }

    /** The unfinished scheduled task happening right now, if any. */
    private fun taskInProgress(): Task? {
        val now = System.currentTimeMillis()
        // isInProgress() also handles repeating tasks (any occurrence happening now).
        return scheduledTasks.firstOrNull { it.isInProgress(now) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!::prefs.isInitialized) return
        val pkg = event.packageName?.toString() ?: return
        val now = SystemClock.uptimeMillis()

        if (prefs.logViewIds && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            now - lastLoggedAt > 1_000
        ) {
            lastLoggedAt = now
            rootInActiveWindow?.let { logViewIds(it) }
        }

        // Cooldown: after pressing Back, give the app a moment to leave the screen
        // so we don't press Back repeatedly and close the whole app.
        if (now - lastBlockAt < COOLDOWN_MS) return

        // Reactive mode: when you're not busy, let everything through.
        val reactive = prefs.mode == BlockMode.Reactive
        val busy = if (reactive) busyWith() else null
        if (reactive && busy == null) return

        val targets = BlockTarget.entries.filter { pkg in it.packages && prefs.isEnabled(it) }
        for (target in targets) {
            if (target.blocksWholeApp || isShowing(target, pkg)) {
                block(target, busy)
                return
            }
        }
    }

    private fun isShowing(target: BlockTarget, pkg: String): Boolean {
        val root = rootInActiveWindow ?: return false
        if (root.packageName?.toString() != pkg) return false
        return target.fullViewIds(pkg).any { id ->
            root.findAccessibilityNodeInfosByViewId(id).any { it.isVisibleToUser }
        }
    }

    private fun block(target: BlockTarget, duringTask: String?) {
        lastBlockAt = SystemClock.uptimeMillis()
        performGlobalAction(if (target.blocksWholeApp) GLOBAL_ACTION_HOME else GLOBAL_ACTION_BACK)
        val countToday = prefs.recordBlock(target)
        if (!BlockNotifier.notifyBlocked(this, target, countToday, duringTask)) {
            // Notifications not allowed: fall back to a quick on-screen message.
            Toast.makeText(this, "Worker Ant blocked ${target.label}", Toast.LENGTH_SHORT).show()
        }
        Log.i(TAG, "Blocked ${target.label}")
    }

    /** Developer aid: prints every view ID on screen so you can find new markers after app updates. */
    private fun logViewIds(root: AccessibilityNodeInfo) {
        val ids = sortedSetOf<String>()
        fun walk(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || depth > 50) return
            node.viewIdResourceName?.let { ids += it }
            for (i in 0 until node.childCount) walk(node.getChild(i), depth + 1)
        }
        walk(root, 0)
        Log.d(TAG, "${root.packageName} screen IDs: ${ids.joinToString()}")
    }

    override fun onInterrupt() {}

    private companion object {
        const val TAG = "WorkerAntBlocker"
        const val COOLDOWN_MS = 1_500L
        const val HALF_DAY_MS = 12 * 60 * 60 * 1000L
    }
}
