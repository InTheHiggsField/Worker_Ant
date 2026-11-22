package com.inthehiggsfield.workerant.ui.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inthehiggsfield.workerant.data.calendar.CalendarSource
import com.inthehiggsfield.workerant.data.tasks.Task
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val CALENDAR_PERMISSIONS = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
private val MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy")
private val DAY_TITLE_FMT = DateTimeFormatter.ofPattern("EEE, MMM d")
private val SHORT_FMT = DateTimeFormatter.ofPattern("MMM d")

/** The view switcher options. [days] is how many columns the time grid shows. */
enum class CalendarViewMode(val label: String, val days: Int) {
    Month("Month", 0), Day("Day", 1), ThreeDay("3 Day", 3)
}

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = viewModel(),
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(hasCalendarPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermission = result.values.all { it }
        if (hasPermission) viewModel.refresh()
    }

    // Re-read the calendars every time you come back to this screen,
    // so events added in Google/Outlook show up.
    LifecycleResumeEffect(hasPermission) {
        if (hasPermission) viewModel.refresh()
        onPauseOrDispose { }
    }

    if (!hasPermission) {
        PermissionPrompt(modifier) { permissionLauncher.launch(CALENDAR_PERMISSIONS) }
    } else {
        CalendarContent(modifier, viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarContent(modifier: Modifier, viewModel: CalendarViewModel) {
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val events by viewModel.items.collectAsStateWithLifecycle()
    val calendars by viewModel.calendars.collectAsStateWithLifecycle()

    var mode by rememberSaveable { mutableStateOf(CalendarViewMode.Month) }
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var showPending by remember { mutableStateOf(false) }
    var scheduling by remember { mutableStateOf<Task?>(null) }

    // Tell the ViewModel which dates are on screen so it loads just those events.
    val range = visibleRange(mode, selectedDate)
    LaunchedEffect(range) { viewModel.showRange(range.first, range.second) }

    // Previous/next: a month in Month view, 1 or 3 days in the time-grid views.
    fun step(direction: Int) {
        selectedDate = when (mode) {
            CalendarViewMode.Month -> selectedDate.plusMonths(direction.toLong())
            else -> selectedDate.plusDays(direction.toLong() * mode.days)
        }
    }

    Column(modifier.fillMaxSize()) {
        // Top bar: title, Today, Pending (with count), Refresh
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title(mode, selectedDate),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { selectedDate = LocalDate.now() }) { Text("Today") }
            IconButton(onClick = { showPending = true }) {
                BadgedBox(badge = { if (pending.isNotEmpty()) Badge { Text("${pending.size}") } }) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Pending tasks")
                }
            }
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        // < [Month | Day | 3 Day] >
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { step(-1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
            }
            ViewSwitcher(mode, onChange = { mode = it }, modifier = Modifier.weight(1f))
            IconButton(onClick = { step(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
            }
        }

        if (calendars.isNotEmpty() && calendars.none { it.source == CalendarSource.Outlook }) {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(
                    "No Outlook calendar found. In the Outlook app: Settings → your account → turn on \"Sync calendars\".",
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // Swipe left/right anywhere on the calendar to go to the next/previous page.
        val swipe = Modifier.pointerInput(mode) {
            var total = 0f
            detectHorizontalDragGestures(
                onDragStart = { total = 0f },
                onDragEnd = {
                    if (total > 120f) step(-1) else if (total < -120f) step(1)
                },
            ) { _, dragAmount -> total += dragAmount }
        }
        Box(Modifier.weight(1f).fillMaxWidth().then(swipe)) {
            when (mode) {
                CalendarViewMode.Month -> MonthView(selectedDate, events, onSelect = { selectedDate = it })
                else -> TimeGridView(selectedDate, mode.days, events)
            }
        }
    }

    if (showPending) {
        ModalBottomSheet(onDismissRequest = { showPending = false }) {
            PendingSheet(pending) { task ->
                showPending = false
                scheduling = task
            }
        }
    }

    scheduling?.let { task ->
        ScheduleTaskDialog(
            task = task,
            calendars = calendars.filter { it.writable },
            initialDate = selectedDate,
            onDismiss = { scheduling = null },
            onConfirm = { start, duration, location, calendarIds ->
                viewModel.schedule(task, start, duration, location, calendarIds)
                scheduling = null
            },
        )
    }
}

/** Outlook-style segmented toggle. */
@Composable
private fun ViewSwitcher(mode: CalendarViewMode, onChange: (CalendarViewMode) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .background(colors.surfaceVariant, RoundedCornerShape(50))
            .padding(3.dp),
    ) {
        CalendarViewMode.entries.forEach { option ->
            val selected = option == mode
            Box(
                Modifier
                    .weight(1f)
                    .background(if (selected) colors.primary else Color.Transparent, RoundedCornerShape(50))
                    .clickable { onChange(option) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) colors.onPrimary else colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PendingSheet(pending: List<Task>, onSchedule: (Task) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text("Pending Scheduled Tasks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        if (pending.isEmpty()) {
            Text(
                "No unscheduled tasks. Add some on the Tasks tab.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn {
                items(pending, key = { it.id }) { task ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSchedule(task) }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(task.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onSchedule(task) }) { Text("Schedule") }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionPrompt(modifier: Modifier, onRequest: () -> Unit) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "WorkerAnt needs calendar access to show your Google and Outlook events " +
                    "and to add scheduled tasks to them.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onRequest) { Text("Grant calendar access") }
        }
    }
}

private fun title(mode: CalendarViewMode, date: LocalDate): String = when (mode) {
    CalendarViewMode.Month -> date.format(MONTH_FMT)
    CalendarViewMode.Day -> date.format(DAY_TITLE_FMT)
    CalendarViewMode.ThreeDay -> "${date.format(SHORT_FMT)} – ${date.plusDays(2).format(SHORT_FMT)}"
}

/** First visible date and the day after the last visible date. */
private fun visibleRange(mode: CalendarViewMode, date: LocalDate): Pair<LocalDate, LocalDate> = when (mode) {
    CalendarViewMode.Month -> monthGridStart(YearMonth.from(date)).let { it to it.plusDays(42) }
    else -> date to date.plusDays(mode.days.toLong())
}

private fun hasCalendarPermission(context: Context): Boolean =
    CALENDAR_PERMISSIONS.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
