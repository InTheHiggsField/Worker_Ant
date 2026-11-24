package com.inthehiggsfield.workerant.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.inthehiggsfield.workerant.MainActivity
import com.inthehiggsfield.workerant.R
import com.inthehiggsfield.workerant.data.calendar.CalendarRepository
import com.inthehiggsfield.workerant.data.calendar.CalendarSource
import com.inthehiggsfield.workerant.data.tasks.AppDatabase
import com.inthehiggsfield.workerant.ui.theme.AppTheme
import com.inthehiggsfield.workerant.ui.theme.FontChoice
import com.inthehiggsfield.workerant.ui.theme.ThemePrefs
import com.inthehiggsfield.workerant.ui.theme.colorSchemeFor
import kotlinx.coroutines.flow.first
import com.inthehiggsfield.workerant.data.tasks.occurrenceEndingAfter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** One line in the widget's "Up next" list. */
private data class UpNext(val whenText: String, val title: String)

private data class WidgetData(val upNext: List<UpNext>, val unscheduled: Int, val canvasDue: Int, val theme: AppTheme)

/**
 * Home screen widget: the next few things on your plate — scheduled tasks (including repeating
 * ones) and timed Outlook events — plus counts of unscheduled tasks and Canvas items due this week.
 * Tap anywhere to open WorkerAnt. Uses your WorkerAnt theme's colors (and a matching font style).
 */
class WorkerAntWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = load(context)
        val theme = data.theme
        provideContent {
            if (theme == AppTheme.Wallpaper) {
                GlanceTheme { Content(data) }   // Material You colors straight from the wallpaper
            } else {
                GlanceTheme(
                    colors = ColorProviders(
                        light = theme.colorSchemeFor(context, dark = false),
                        dark = theme.colorSchemeFor(context, dark = true),
                    )
                ) { Content(data) }
            }
        }
    }

    private suspend fun load(context: Context): WidgetData {
        val now = System.currentTimeMillis()
        val weekAhead = now + 7 * 24 * 60 * 60 * 1000L
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val db = AppDatabase.get(context)
        val tasks = db.taskDao().openTasksNow()

        // Scheduled WorkerAnt tasks (next/current occurrence)...
        val fromTasks = tasks.mapNotNull { task -> task.occurrenceEndingAfter(now)?.let { it to task.title } }
        // ...plus timed Outlook events. Skipped: all-day events, occurrences you've hidden, and
        // events WorkerAnt created for a task (that task is already listed).
        val fromOutlook = runCatching {
            val linked = db.taskDao().observeTaskEvents().first().map { it.eventId }.toSet()
            val hidden = db.hiddenInstanceDao().observeAll().first().map { it.eventId to it.begin }.toSet()
            val repo = CalendarRepository(context)
            val calendars = repo.calendars().associateBy { it.id }
            repo.events(now - 12 * 60 * 60 * 1000L, weekAhead, calendars)
                .filter {
                    it.source == CalendarSource.Outlook && !it.allDay && it.end > now &&
                        it.eventId !in linked && (it.eventId to it.begin) !in hidden
                }
                .map { it.begin to it.title.ifBlank { "(No title)" } }
        }.getOrDefault(emptyList())   // no calendar permission: tasks only

        val upNext = (fromTasks + fromOutlook)
            .sortedBy { it.first }
            .take(4)
            .map { (start, title) ->
                val time = Instant.ofEpochMilli(start).atZone(zone)
                val day = when (time.toLocalDate()) {
                    today -> "Today"
                    today.plusDays(1) -> "Tmrw"
                    else -> time.format(DateTimeFormatter.ofPattern("EEE"))
                }
                val whenText = if (start <= now) "Now" else "$day ${time.format(DateTimeFormatter.ofPattern("h:mm a"))}"
                UpNext(whenText, title)
            }
        return WidgetData(
            upNext = upNext,
            unscheduled = tasks.count { it.scheduledStart == null && it.canvasId == null },
            canvasDue = tasks.count { it.canvasId != null && (it.dueAt ?: Long.MAX_VALUE) <= weekAhead },
            theme = ThemePrefs.load(context),
        )
    }

    @Composable
    private fun Content(data: WidgetData) {
        val colors = GlanceTheme.colors
        // Widgets can't load custom fonts, so each theme gets the closest built-in style.
        val titleFont = glanceFont(data.theme.spec?.display)
        val bodyFont = glanceFont(data.theme.spec?.body)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(if (data.theme == AppTheme.Wallpaper) colors.widgetBackground else colors.background)
                .cornerRadius(20.dp)
                .padding(14.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.ic_stat_worker_ant),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colors.primary),
                    modifier = GlanceModifier.size(20.dp),
                )
                Spacer(GlanceModifier.width(8.dp))
                Text("Up next", style = TextStyle(color = colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = titleFont))
            }
            Spacer(GlanceModifier.height(8.dp))

            if (data.upNext.isEmpty()) {
                Text("Nothing scheduled. Nice.", style = TextStyle(color = colors.onSurfaceVariant, fontSize = 13.sp, fontFamily = bodyFont))
            } else {
                data.upNext.forEach { item ->
                    Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(
                            item.whenText,
                            style = TextStyle(color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                            modifier = GlanceModifier.width(84.dp),
                        )
                        Text(item.title, style = TextStyle(color = colors.onSurface, fontSize = 13.sp, fontFamily = bodyFont), maxLines = 1)
                    }
                }
            }

            val footer = listOfNotNull(
                if (data.unscheduled > 0) "${data.unscheduled} unscheduled" else null,
                if (data.canvasDue > 0) "${data.canvasDue} due on Canvas this week" else null,
            )
            if (footer.isNotEmpty()) {
                Spacer(GlanceModifier.defaultWeight())
                Text(footer.joinToString(" · "), style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp), maxLines = 1)
            }
        }
    }

    /** Closest built-in widget font to a theme's font (null = the launcher's default). */
    private fun glanceFont(choice: FontChoice?): FontFamily? = when (choice) {
        FontChoice.Rye, FontChoice.Cinzel, FontChoice.Playfair, FontChoice.Fraktur,
        FontChoice.Christmas, FontChoice.Serif -> FontFamily.Serif
        FontChoice.Orbitron, FontChoice.PressStart, FontChoice.Michroma, FontChoice.Audiowide,
        FontChoice.SpecialElite, FontChoice.Mono -> FontFamily.Monospace
        FontChoice.Pacifico -> FontFamily.Cursive
        FontChoice.BebasNeue, FontChoice.Bangers, FontChoice.Righteous, FontChoice.Fredoka,
        FontChoice.Creepster, FontChoice.Sans -> FontFamily.SansSerif
        FontChoice.Default, null -> null
    }

    companion object {
        /** Redraw every placed WorkerAnt widget with fresh data. */
        suspend fun refresh(context: Context) = WorkerAntWidget().updateAll(context)
    }
}

/** Connects the widget to Android's home screen widget system (declared in AndroidManifest.xml). */
class WorkerAntWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WorkerAntWidget()
}
