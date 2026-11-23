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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.inthehiggsfield.workerant.MainActivity
import com.inthehiggsfield.workerant.R
import com.inthehiggsfield.workerant.data.tasks.AppDatabase
import com.inthehiggsfield.workerant.data.tasks.occurrenceEndingAfter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** One line in the widget's "Up next" list. */
private data class UpNext(val whenText: String, val title: String)

private data class WidgetData(val upNext: List<UpNext>, val unscheduled: Int, val canvasDue: Int)

/**
 * Home screen widget: the next few scheduled tasks (including repeating ones), plus counts of
 * unscheduled tasks and Canvas items due this week. Tap anywhere to open WorkerAnt.
 * Colors follow your wallpaper (Material You) on Android 12+.
 */
class WorkerAntWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = load(context)
        provideContent {
            GlanceTheme { Content(data) }
        }
    }

    private suspend fun load(context: Context): WidgetData {
        val now = System.currentTimeMillis()
        val weekAhead = now + 7 * 24 * 60 * 60 * 1000L
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val tasks = AppDatabase.get(context).taskDao().openTasksNow()

        val upNext = tasks.mapNotNull { task -> task.occurrenceEndingAfter(now)?.let { it to task } }
            .sortedBy { it.first }
            .take(4)
            .map { (start, task) ->
                val time = Instant.ofEpochMilli(start).atZone(zone)
                val day = when (time.toLocalDate()) {
                    today -> "Today"
                    today.plusDays(1) -> "Tmrw"
                    else -> time.format(DateTimeFormatter.ofPattern("EEE"))
                }
                val whenText = if (start <= now) "Now" else "$day ${time.format(DateTimeFormatter.ofPattern("h:mm a"))}"
                UpNext(whenText, task.title)
            }
        return WidgetData(
            upNext = upNext,
            unscheduled = tasks.count { it.scheduledStart == null && it.canvasId == null },
            canvasDue = tasks.count { it.canvasId != null && (it.dueAt ?: Long.MAX_VALUE) <= weekAhead },
        )
    }

    @Composable
    private fun Content(data: WidgetData) {
        val colors = GlanceTheme.colors
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(colors.widgetBackground)
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
                Text("Up next", style = TextStyle(color = colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold))
            }
            Spacer(GlanceModifier.height(8.dp))

            if (data.upNext.isEmpty()) {
                Text("Nothing scheduled. Nice.", style = TextStyle(color = colors.onSurfaceVariant, fontSize = 13.sp))
            } else {
                data.upNext.forEach { item ->
                    Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(
                            item.whenText,
                            style = TextStyle(color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                            modifier = GlanceModifier.width(84.dp),
                        )
                        Text(item.title, style = TextStyle(color = colors.onSurface, fontSize = 13.sp), maxLines = 1)
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

    companion object {
        /** Redraw every placed WorkerAnt widget with fresh data. */
        suspend fun refresh(context: Context) = WorkerAntWidget().updateAll(context)
    }
}

/** Connects the widget to Android's home screen widget system (declared in AndroidManifest.xml). */
class WorkerAntWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WorkerAntWidget()
}
