package com.inthehiggsfield.workerant.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.inthehiggsfield.workerant.data.tasks.Task
import com.inthehiggsfield.workerant.data.tasks.occurrenceEndingAfter
import com.inthehiggsfield.workerant.ui.theme.AppTheme
import com.inthehiggsfield.workerant.ui.theme.ThemeMascot
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val SCHEDULED_FMT = DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a")

@Composable
fun TasksScreen(
    theme: AppTheme,
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = viewModel(),
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    var newTitle by rememberSaveable { mutableStateOf("") }
    var showDone by rememberSaveable { mutableStateOf(false) }

    val canvasError by viewModel.canvasError.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Pull new Canvas to-dos whenever this tab opens (at most every 15 minutes).
    LaunchedEffect(Unit) { viewModel.syncCanvasIfStale() }

    fun submit() {
        viewModel.add(newTitle)
        newTitle = ""
    }

    fun open(task: Task) {
        task.url?.let { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Tasks", style = MaterialTheme.typography.headlineMedium)

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                placeholder = { Text("Add a task") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { submit() }) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
            }
        }

        canvasError?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        if (tasks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing to do. Nice.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            // Bottom padding lets the last task scroll up past the mascot.
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                // Sections: not yet on the calendar, Canvas to-dos (soonest due first),
                // on the calendar (soonest first, repeating tasks by their next occurrence), and done.
                val now = System.currentTimeMillis()
                val unscheduled = tasks.filter { !it.isDone && it.scheduledStart == null && it.canvasId == null }
                val canvas = tasks.filter { !it.isDone && it.scheduledStart == null && it.canvasId != null }
                    .sortedBy { it.dueAt ?: Long.MAX_VALUE }
                val scheduled = tasks.filter { !it.isDone && it.scheduledStart != null }
                    .sortedBy { it.occurrenceEndingAfter(now) ?: it.scheduledStart }
                val done = tasks.filter { it.isDone }

                fun section(title: String, list: List<Task>) {
                    if (list.isEmpty()) return
                    item(key = "header-$title") { SectionHeader("$title (${list.size})") }
                    items(list, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onToggle = { viewModel.toggle(task) },
                            onDelete = { viewModel.delete(task) },
                            onOpen = { open(task) },
                        )
                        HorizontalDivider()
                    }
                }
                section("Unscheduled", unscheduled)
                section("Canvas", canvas)
                section("Scheduled", scheduled)
                if (done.isNotEmpty()) {
                    item(key = "header-done") {
                        SectionHeader(
                            "Completed (${done.size}) " + if (showDone) "▾" else "▸",
                            Modifier.clickable { showDone = !showDone },
                        )
                    }
                    if (showDone) {
                        items(done, key = { it.id }) { task ->
                            TaskRow(task = task, onToggle = { viewModel.toggle(task) }, onDelete = { viewModel.delete(task) })
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

        // The theme's little animated mascot, bottom-right.
        ThemeMascot(
            theme = theme,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun TaskRow(task: Task, onToggle: () -> Unit, onDelete: () -> Unit, onOpen: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = task.isDone, onCheckedChange = { onToggle() })
        // Canvas tasks open the assignment in Canvas when you tap the text.
        Column(modifier = Modifier.weight(1f).clickable(enabled = task.url != null, onClick = onOpen)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
            )
            val zone = ZoneId.systemDefault()
            // For repeating tasks, show the occurrence happening now or next.
            val start = if (task.isDone) task.scheduledStart
                        else task.occurrenceEndingAfter(System.currentTimeMillis()) ?: task.scheduledStart
            val details = listOfNotNull(
                start?.let { Instant.ofEpochMilli(it).atZone(zone).format(SCHEDULED_FMT) },
                task.repeatRule?.let { "↻ ${it.shortLabel}" },
                task.dueAt?.let { "Due " + Instant.ofEpochMilli(it).atZone(zone).format(SCHEDULED_FMT) },
                task.course,
                task.location,
            )
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete task")
        }
    }
}
