package com.inthehiggsfield.workerant.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.inthehiggsfield.workerant.canvas.CanvasSync
import com.inthehiggsfield.workerant.reminders.ReminderScheduler
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** "Remind me 30 minutes before scheduled tasks" switch, plus the exact-alarm permission. */
@Composable
fun RemindersCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(ReminderScheduler.isEnabled(context)) }
    var exact by remember { mutableStateOf(ReminderScheduler.canUseExactAlarms(context)) }
    LifecycleResumeEffect(Unit) {
        exact = ReminderScheduler.canUseExactAlarms(context)
        scope.launch { ReminderScheduler.rescheduleAll(context) }   // switch to on-time alarms if just allowed
        onPauseOrDispose { }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Task reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Notify me ${ReminderScheduler.LEAD_MINUTES} minutes before a scheduled task starts " +
                            "(including each repeat).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = enabled, onCheckedChange = {
                    enabled = it
                    scope.launch { ReminderScheduler.setEnabled(context, it) }
                })
            }
            if (enabled && !exact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Text(
                    "Reminders may arrive a few minutes late until you allow on-time alarms.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }) { Text("Allow on-time alarms") }
            }
        }
    }
}

/** Connect Canvas with your school's Canvas address and a personal access token. */
@Composable
fun CanvasCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var connected by remember { mutableStateOf(CanvasSync.isConnected(context)) }
    var url by remember { mutableStateOf(CanvasSync.baseUrl(context)) }
    var token by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(lastSyncText(CanvasSync.lastSyncAt(context))) }
    var busy by remember { mutableStateOf(false) }

    fun runSync() {
        busy = true
        status = "Syncing…"
        scope.launch {
            val result = CanvasSync.sync(context)
            busy = false
            status = result.fold(
                onSuccess = { added -> "Synced. ${if (added == 1) "1 new to-do" else "$added new to-dos"}." },
                onFailure = { it.message ?: "Sync failed." },
            )
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Canvas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (connected) {
                Text(
                    "Connected to ${CanvasSync.baseUrl(context).removePrefix("https://")}. Assignments, quizzes, " +
                        "discussions, and Canvas to-dos due in the next 4 weeks show up in Tasks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(status, style = MaterialTheme.typography.bodySmall)
                Row {
                    Button(onClick = { runSync() }, enabled = !busy) { Text("Sync now") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        CanvasSync.disconnect(context)
                        connected = false
                        status = ""
                    }) { Text("Disconnect") }
                }
            } else {
                Text(
                    "Import your Canvas to-dos as tasks. In Canvas, go to Account → Settings → " +
                        "\"+ New Access Token\", give it a name like WorkerAnt, and paste the token here. " +
                        "It's stored only on this phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Canvas address") },
                    placeholder = { Text("canvas.tamu.edu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Access token") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall)
                Button(
                    enabled = url.isNotBlank() && token.isNotBlank() && !busy,
                    onClick = {
                        CanvasSync.connect(context, url, token)
                        token = ""
                        connected = true
                        runSync()
                    },
                ) { Text("Connect and sync") }
            }
        }
    }
}

private fun lastSyncText(millis: Long): String =
    if (millis == 0L) "" else "Last synced " +
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, h:mm a")) + "."
