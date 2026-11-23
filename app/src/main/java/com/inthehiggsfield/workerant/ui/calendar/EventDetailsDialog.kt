package com.inthehiggsfield.workerant.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DAY_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d")
private val TIME_FMT = DateTimeFormatter.ofPattern("h:mm a")

/**
 * Shown when you tap an event. "Hide on this day" hides only this occurrence in WorkerAnt
 * (handy when one week of a recurring class is canceled); the rest of the series still shows,
 * and nothing changes in Google or Outlook.
 */
@Composable
fun EventDetailsDialog(
    item: AgendaItem,
    onHide: () -> Unit,
    onUnhide: () -> Unit,
    onDismiss: () -> Unit,
) {
    val zone = if (item.allDay) ZoneOffset.UTC else ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(item.begin).atZone(zone)
    val end = Instant.ofEpochMilli(item.end).atZone(zone)
    val whenText = if (item.allDay) "${start.format(DAY_FMT)} · All day"
                   else "${start.format(DAY_FMT)} · ${start.format(TIME_FMT)} – ${end.format(TIME_FMT)}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.title.ifBlank { "(No title)" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(whenText, style = MaterialTheme.typography.bodyMedium)
                item.location?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Text(
                    "Calendar: " + item.sources.joinToString(" + ") { it.label },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.hidden) {
                    Text(
                        "Hidden on this day in WorkerAnt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            if (item.hidden) {
                TextButton(onClick = { onUnhide(); onDismiss() }) { Text("Unhide") }
            } else {
                TextButton(onClick = { onHide(); onDismiss() }) { Text("Hide on this day") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
