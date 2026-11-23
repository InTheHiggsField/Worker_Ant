package com.inthehiggsfield.workerant.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.inthehiggsfield.workerant.TaskChanges
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Android clears alarms when the phone restarts or the app updates; this sets them again. */
class RestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                TaskChanges.onTasksChanged(context)
            } finally {
                pending.finish()
            }
        }
    }
}
