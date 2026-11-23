package com.inthehiggsfield.workerant.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inthehiggsfield.workerant.data.notes.Note
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME_FMT = DateTimeFormatter.ofPattern("h:mm a")
private val DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy")

/** Personal notepad: a list of notes, and a simple editor that saves when you go back. */
@Composable
fun NotesScreen(modifier: Modifier = Modifier, viewModel: NotesViewModel = viewModel()) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    // Which note is open: null = list view, -1 = a brand-new note, otherwise the note's id.
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }

    val open = openId
    if (open == null) {
        NoteList(notes, modifier, onOpen = { openId = it.id }, onNew = { openId = -1L })
    } else {
        val existing = notes.firstOrNull { it.id == open }
        NoteEditor(
            existing = existing,
            modifier = modifier,
            onDone = { title, body ->
                viewModel.save(existing, title, body)
                openId = null
            },
            onDelete = {
                existing?.let(viewModel::delete)
                openId = null
            },
        )
    }
}

@Composable
private fun NoteList(notes: List<Note>, modifier: Modifier, onOpen: (Note) -> Unit, onNew: () -> Unit) {
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text("Notes", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            if (notes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No notes yet. Tap + to write one.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    items(notes, key = { it.id }) { note -> NoteCard(note) { onOpen(note) } }
                }
            }
        }
        FloatingActionButton(
            onClick = onNew,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Default.Add, contentDescription = "New note") }
    }
}

@Composable
private fun NoteCard(note: Note, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(14.dp)) {
            Text(
                note.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (note.body.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    note.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                editedLabel(note.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoteEditor(
    existing: Note?,
    modifier: Modifier,
    onDone: (title: String, body: String) -> Unit,
    onDelete: () -> Unit,
) {
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var body by rememberSaveable(existing?.id) { mutableStateOf(existing?.body ?: "") }

    // The phone's Back gesture also saves and closes.
    BackHandler { onDone(title, body) }

    val clear = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )
    // imePadding: when the keyboard opens, the editor shrinks to the space above it,
    // so the line you're typing on stays visible.
    Column(modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onDone(title, body) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Save and go back")
            }
            Spacer(Modifier.weight(1f))
            if (existing != null) {
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete note") }
            }
        }
        TextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Title", style = MaterialTheme.typography.headlineSmall) },
            textStyle = MaterialTheme.typography.headlineSmall,
            singleLine = true,
            colors = clear,
            modifier = Modifier.fillMaxWidth(),
        )
        TextField(
            value = body,
            onValueChange = { body = it },
            placeholder = { Text("Start writing…") },
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = clear,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
}

private fun editedLabel(millis: Long): String {
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    return if (time.toLocalDate() == LocalDate.now()) "Edited today, ${time.format(TIME_FMT)}"
           else "Edited ${time.format(DATE_FMT)}"
}
