package com.inthehiggsfield.workerant.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME_FMT = DateTimeFormatter.ofPattern("h:mm a")

/** The calendar's color, forced opaque (some providers store it without alpha). */
@Composable
internal fun eventColor(item: AgendaItem): Color =
    if (item.color == 0) MaterialTheme.colorScheme.primary else Color(item.color or 0xFF000000.toInt())

/** One event as a list row: color bar, title, time/place, and which calendar(s) it's in. */
@Composable
internal fun AgendaRow(item: AgendaItem, onClick: () -> Unit = {}) {
    val zone = ZoneId.systemDefault()
    val timeLabel = if (item.allDay) "All day" else
        "${Instant.ofEpochMilli(item.begin).atZone(zone).format(TIME_FMT)} – " +
            Instant.ofEpochMilli(item.end).atZone(zone).format(TIME_FMT)
    val subtitle = listOfNotNull(timeLabel, item.location, if (item.hidden) "Hidden" else null).joinToString(" · ")

    Row(
        Modifier.fillMaxWidth().alpha(if (item.hidden) 0.45f else 1f).clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).height(40.dp).background(eventColor(item), RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title.ifBlank { "(No title)" }, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            item.sources.joinToString(" + ") { it.label },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
