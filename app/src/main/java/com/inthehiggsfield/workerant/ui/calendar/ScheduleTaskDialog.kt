package com.inthehiggsfield.workerant.ui.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.inthehiggsfield.workerant.data.calendar.CalendarSource
import com.inthehiggsfield.workerant.data.calendar.DeviceCalendar
import com.inthehiggsfield.workerant.data.tasks.Task
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val DATE_FMT = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")
private val TIME_FMT = DateTimeFormatter.ofPattern("h:mm a")

/**
 * Pick a time (required), duration, place (optional), and which calendars to add the task to.
 * By default, your main Google calendar and main Outlook calendar are both checked.
 */
@Composable
fun ScheduleTaskDialog(
    task: Task,
    calendars: List<DeviceCalendar>,
    initialDate: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onConfirm: (start: Long, durationMinutes: Int, location: String?, calendarIds: List<Long>) -> Unit,
) {
    val context = LocalContext.current
    var date by remember { mutableStateOf(initialDate) }
    var time by remember { mutableStateOf<LocalTime?>(null) }
    var durationText by remember { mutableStateOf("60") }
    var location by remember { mutableStateOf("") }
    var selected by remember(calendars) { mutableStateOf(defaultSelection(calendars)) }

    val duration = durationText.toIntOrNull()?.takeIf { it > 0 }
    val canSave = time != null && duration != null && selected.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule \"${task.title}\"") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
                            date.year, date.monthValue - 1, date.dayOfMonth,
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(date.format(DATE_FMT)) }

                OutlinedButton(
                    onClick = {
                        val initial = time ?: LocalTime.now().withMinute(0)
                        TimePickerDialog(
                            context,
                            { _, h, min -> time = LocalTime.of(h, min) },
                            initial.hour, initial.minute, DateFormat.is24HourFormat(context),
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(time?.format(TIME_FMT) ?: "Pick a time (required)") }

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter(Char::isDigit) },
                    label = { Text("Duration (minutes)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Place (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Add to calendars", style = MaterialTheme.typography.labelLarge)
                if (calendars.isEmpty()) {
                    Text("No calendars you can write to were found on this phone.")
                }
                calendars.forEach { cal ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selected = selected.toggle(cal.id) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = cal.id in selected, onCheckedChange = { selected = selected.toggle(cal.id) })
                        Text("${cal.source.label} · ${cal.name}")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val start = ZonedDateTime.of(date, time!!, ZoneId.systemDefault()).toInstant().toEpochMilli()
                    onConfirm(start, duration!!, location.trim().ifEmpty { null }, selected.toList())
                },
            ) { Text("Schedule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Main Google calendar + main Outlook calendar, if present. */
private fun defaultSelection(calendars: List<DeviceCalendar>): Set<Long> =
    listOf(CalendarSource.Google, CalendarSource.Outlook).mapNotNull { source ->
        val ofSource = calendars.filter { it.source == source }
        (ofSource.firstOrNull { it.isPrimary } ?: ofSource.firstOrNull())?.id
    }.toSet()

private fun Set<Long>.toggle(id: Long): Set<Long> = if (id in this) this - id else this + id
