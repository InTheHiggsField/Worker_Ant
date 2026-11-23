package com.inthehiggsfield.workerant.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HOUR_HEIGHT = 56.dp
private val GUTTER = 44.dp
private val DOW_FMT = DateTimeFormatter.ofPattern("EEE")
private val HOUR_FMT = DateTimeFormatter.ofPattern("h a")

/**
 * Outlook-style Day / 3-Day view: an hour-by-hour grid where each event is a block
 * sized by its duration. Opens scrolled to 7 AM; all-day events sit in a strip on top.
 */
@Composable
fun TimeGridView(
    startDate: LocalDate,
    days: Int,
    events: List<AgendaItem>,
    onEventClick: (AgendaItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val dates = (0 until days).map { startDate.plusDays(it.toLong()) }
    val today = LocalDate.now()
    val density = LocalDensity.current
    val scroll = rememberScrollState(initial = with(density) { (HOUR_HEIGHT * 7).roundToPx() })

    Column(modifier.fillMaxSize()) {
        // Day headers: "Sat / 21"
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Spacer(Modifier.width(GUTTER))
            dates.forEach { date ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(date.format(DOW_FMT), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${date.dayOfMonth}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (date == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // All-day strip (only shown if there are any)
        val allDay = dates.map { d -> events.filter { it.allDay && it.occursOn(d) } }
        if (allDay.any { it.isNotEmpty() }) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(GUTTER))
                allDay.forEach { list ->
                    Column(Modifier.weight(1f).padding(horizontal = 1.dp)) {
                        list.forEach { item -> EventBlock(item, Modifier.fillMaxWidth().padding(vertical = 1.dp)) { onEventClick(item) } }
                    }
                }
            }
        }
        HorizontalDivider()

        // Hour grid
        Row(Modifier.fillMaxWidth().weight(1f).verticalScroll(scroll)) {
            Column(Modifier.width(GUTTER)) {
                for (hour in 0 until 24) {
                    Box(Modifier.height(HOUR_HEIGHT)) {
                        if (hour > 0) {
                            Text(
                                LocalTime.of(hour, 0).format(HOUR_FMT),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.offset(y = (-7).dp).padding(start = 4.dp),
                            )
                        }
                    }
                }
            }
            dates.forEach { date ->
                DayColumn(
                    date = date,
                    events = events.filter { !it.allDay && it.occursOn(date) },
                    isToday = date == today,
                    onEventClick = onEventClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DayColumn(
    date: LocalDate,
    events: List<AgendaItem>,
    isToday: Boolean,
    onEventClick: (AgendaItem) -> Unit,
    modifier: Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val placed = remember(events) { layoutOverlaps(events) }

    BoxWithConstraints(
        modifier
            .height(HOUR_HEIGHT * 24)
            .drawBehind {
                for (hour in 1 until 24) {
                    val y = HOUR_HEIGHT.toPx() * hour
                    drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                }
                drawLine(lineColor, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 1f)
            }
    ) {
        placed.forEach { p ->
            val startMin = ((p.item.begin - dayStart) / 60_000).coerceIn(0, 1440)
            val endMin = ((p.item.end - dayStart) / 60_000).coerceIn(0, 1440)
            val durationMin = maxOf(endMin - startMin, 20)   // keep tiny events tappable/readable
            val width = maxWidth / p.columns
            EventBlock(
                p.item,
                Modifier
                    .offset(x = width * p.column, y = HOUR_HEIGHT * (startMin / 60f))
                    .width(width)
                    .height(HOUR_HEIGHT * (durationMin / 60f))
                    .padding(1.dp),
            ) { onEventClick(p.item) }
        }
        if (isToday) {
            // Red "now" line, like Outlook
            val nowMin = (System.currentTimeMillis() - dayStart) / 60_000f
            Box(
                Modifier
                    .offset(y = HOUR_HEIGHT * (nowMin / 60f))
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0xFFE53935))
            )
        }
    }
}

@Composable
private fun EventBlock(item: AgendaItem, modifier: Modifier, onClick: () -> Unit) {
    val background = eventColor(item)
    // Dark text on light calendar colors, white text on dark ones.
    val textColor = if (background.luminance() > 0.45f) Color(0xFF1A1A1A) else Color.White
    Box(
        modifier
            .alpha(if (item.hidden) 0.4f else 1f)
            .background(background.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Column {
            Text(
                item.title.ifBlank { "(No title)" },
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            item.location?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.85f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private data class Placed(val item: AgendaItem, val column: Int, val columns: Int)

/**
 * Side-by-side layout for overlapping events: each group of overlapping events is
 * split into columns, and every event in the group gets 1/N of the day's width.
 */
private fun layoutOverlaps(events: List<AgendaItem>): List<Placed> {
    val result = mutableListOf<Placed>()
    val group = mutableListOf<Pair<AgendaItem, Int>>()
    val columnEnds = mutableListOf<Long>()
    var groupEnd = Long.MIN_VALUE

    fun flushGroup() {
        val n = columnEnds.size
        group.forEach { (item, col) -> result += Placed(item, col, n) }
        group.clear()
        columnEnds.clear()
        groupEnd = Long.MIN_VALUE
    }

    for (event in events.sortedBy { it.begin }) {
        if (group.isNotEmpty() && event.begin >= groupEnd) flushGroup()
        var col = columnEnds.indexOfFirst { it <= event.begin }
        if (col == -1) {
            columnEnds += event.end
            col = columnEnds.lastIndex
        } else {
            columnEnds[col] = event.end
        }
        group += event to col
        groupEnd = maxOf(groupEnd, event.end)
    }
    flushGroup()
    return result
}
