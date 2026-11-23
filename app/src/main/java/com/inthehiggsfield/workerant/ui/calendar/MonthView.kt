package com.inthehiggsfield.workerant.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val DAY_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d")
private val WEEKDAYS = listOf("S", "M", "T", "W", "T", "F", "S")

/** First cell of the 6x7 month grid: the Sunday on or before the 1st. */
internal fun monthGridStart(month: YearMonth): LocalDate {
    val first = month.atDay(1)
    return first.minusDays((first.dayOfWeek.value % 7).toLong())
}

/**
 * Outlook-style month view: a fixed 6-week grid with colored dots on busy days,
 * and the selected day's events listed underneath.
 */
@Composable
fun MonthView(
    selectedDate: LocalDate,
    events: List<AgendaItem>,
    onSelect: (LocalDate) -> Unit,
    onEventClick: (AgendaItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val month = YearMonth.from(selectedDate)
    val gridStart = monthGridStart(month)
    val today = LocalDate.now()
    val byDay = remember(events, gridStart) {
        (0 until 42).associate { offset ->
            val date = gridStart.plusDays(offset.toLong())
            date to events.filter { it.occursOn(date) }
        }
    }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            WEEKDAYS.forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        for (week in 0 until 6) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                for (dow in 0 until 7) {
                    val date = gridStart.plusDays((week * 7 + dow).toLong())
                    DayCell(
                        date = date,
                        inMonth = date.month == month.month,
                        isToday = date == today,
                        isSelected = date == selectedDate,
                        dayEvents = byDay[date].orEmpty(),
                        onClick = { onSelect(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        HorizontalDivider(Modifier.padding(top = 4.dp))
        Text(
            if (selectedDate == today) "Today · ${selectedDate.format(DAY_FMT)}" else selectedDate.format(DAY_FMT),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        val selectedEvents = byDay[selectedDate] ?: events.filter { it.occursOn(selectedDate) }
        if (selectedEvents.isEmpty()) {
            Text(
                "No events",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                items(selectedEvents, key = { it.key }) { AgendaRow(it) { onEventClick(it) } }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    dayEvents: List<AgendaItem>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val circle = when {
        isSelected -> colors.primary
        isToday -> colors.primaryContainer
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> colors.onPrimary
        isToday -> colors.onPrimaryContainer
        !inMonth -> colors.onSurface.copy(alpha = 0.35f)
        else -> colors.onSurface
    }
    Column(
        modifier = modifier.height(48.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(32.dp).background(circle, CircleShape), contentAlignment = Alignment.Center) {
            Text("${date.dayOfMonth}", style = MaterialTheme.typography.bodyMedium, color = textColor)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 2.dp)) {
            dayEvents.take(3).forEach { item ->
                Box(Modifier.size(5.dp).background(eventColor(item), CircleShape))
            }
        }
    }
}
