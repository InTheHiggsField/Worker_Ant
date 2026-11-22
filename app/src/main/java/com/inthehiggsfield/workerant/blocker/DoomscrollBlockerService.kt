package com.inthehiggsfield.workerant.blocker

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * Watches the apps listed in res/xml/blocker_accessibility_config.xml.
 * When an enabled target's doomscroll screen is showing, it presses Back
 * (or Home, for apps that are entirely a feed) and shows a short toast.
 *
 * You turn this on yourself in Settings → Accessibility → WorkerAnt.
 */
class DoomscrollBlockerService : AccessibilityService() {
    private lateinit var prefs: BlockerPrefs
    private var lastBlockAt = 0L
    private var lastLoggedAt = 0L

    override fun onServiceConnected() {
        prefs = BlockerPrefs(this)
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

        val targets = BlockTarget.entries.filter { pkg in it.packages && prefs.isEnabled(it) }
        for (target in targets) {
            if (target.blocksWholeApp || isShowing(target, pkg)) {
                block(target)
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

    private fun block(target: BlockTarget) {
        lastBlockAt = SystemClock.uptimeMillis()
        performGlobalAction(if (target.blocksWholeApp) GLOBAL_ACTION_HOME else GLOBAL_ACTION_BACK)
        val countToday = prefs.recordBlock(target)
        if (!BlockNotifier.notifyBlocked(this, target, countToday)) {
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
    }
}
