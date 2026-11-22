package com.inthehiggsfield.workerant.ui.blocker

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.inthehiggsfield.workerant.blocker.BlockNotifier
import com.inthehiggsfield.workerant.blocker.BlockTarget
import com.inthehiggsfield.workerant.blocker.BlockerPrefs
import com.inthehiggsfield.workerant.blocker.isBlockerServiceEnabled

@Composable
fun BlockerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { BlockerPrefs(context) }
    var serviceOn by remember { mutableStateOf(isBlockerServiceEnabled(context)) }
    val enabled = remember {
        mutableStateMapOf<BlockTarget, Boolean>().apply {
            BlockTarget.entries.forEach { put(it, prefs.isEnabled(it)) }
        }
    }
    var logIds by remember { mutableStateOf(prefs.logViewIds) }
    var canNotify by remember { mutableStateOf(BlockNotifier.canNotify(context)) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> canNotify = granted }

    // Re-check when you come back from Accessibility settings.
    LifecycleResumeEffect(Unit) {
        serviceOn = isBlockerServiceEnabled(context)
        canNotify = BlockNotifier.canNotify(context)
        onPauseOrDispose { }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Doomscroll Blocker", style = MaterialTheme.typography.headlineMedium)

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (serviceOn) MaterialTheme.colorScheme.primaryContainer
                                 else MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (serviceOn) "Blocker is ON" else "Blocker is OFF",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (serviceOn) "Pick what to block below. Changes apply immediately."
                    else "Android requires you to switch it on yourself: Accessibility → Installed apps → WorkerAnt → On.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!serviceOn) {
                    Button(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }) { Text("Open Accessibility settings") }
                }
            }
        }

        if (!canNotify && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Get a notification when something is blocked", style = MaterialTheme.typography.titleSmall)
                    Button(onClick = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                        Text("Allow notifications")
                    }
                }
            }
        }

        Text("Block", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Column {
            BlockTarget.entries.forEach { target ->
                ToggleRow(
                    title = target.label,
                    subtitle = target.description,
                    checked = enabled[target] == true,
                    onCheckedChange = {
                        enabled[target] = it
                        prefs.setEnabled(target, it)
                    },
                )
                HorizontalDivider()
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Developer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        ToggleRow(
            title = "Log screen IDs",
            subtitle = "If an app update breaks detection, turn this on, open the feature, and check " +
                "Logcat (filter: WorkerAntBlocker) for the new view ID to add in BlockTarget.kt.",
            checked = logIds,
            onCheckedChange = {
                logIds = it
                prefs.logViewIds = it
            },
        )
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
