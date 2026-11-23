package com.inthehiggsfield.workerant.canvas

import android.content.Context
import androidx.core.content.edit
import com.inthehiggsfield.workerant.TaskChanges
import com.inthehiggsfield.workerant.data.tasks.AppDatabase
import com.inthehiggsfield.workerant.data.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Imports your Canvas to-dos (assignments, quizzes, graded discussions, and your own Canvas
 * planner notes due in the next 4 weeks) as WorkerAnt tasks, using Canvas's Planner API
 * with a personal access token you generate in Canvas.
 *
 * - New Canvas items become tasks in the "Canvas" section.
 * - Changed titles/due dates are updated; anything you did in WorkerAnt (scheduling it) is kept.
 * - Items you submit or mark done in Canvas are checked off here automatically.
 *
 * The token is stored only on this phone (excluded from backups: see backup_rules.xml and
 * data_extraction_rules.xml in res/xml).
 */
object CanvasSync {
    private const val DAYS_AHEAD = 28L

    private fun prefs(context: Context) = context.getSharedPreferences("canvas", Context.MODE_PRIVATE)

    fun baseUrl(context: Context): String = prefs(context).getString("baseUrl", "") ?: ""
    fun isConnected(context: Context): Boolean =
        baseUrl(context).isNotBlank() && !prefs(context).getString("token", null).isNullOrBlank()
    fun lastSyncAt(context: Context): Long = prefs(context).getLong("lastSync", 0L)

    fun connect(context: Context, baseUrl: String, token: String) {
        prefs(context).edit {
            putString("baseUrl", normalize(baseUrl))
            putString("token", token.trim())
        }
    }

    /** Forgets the token. Imported tasks stay, so nothing you scheduled is lost. */
    fun disconnect(context: Context) {
        prefs(context).edit { remove("token"); remove("lastSync") }
    }

    /** "canvas.tamu.edu" -> "https://canvas.tamu.edu" */
    private fun normalize(url: String): String {
        val trimmed = url.trim().trimEnd('/')
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
    }

    /** Syncs only if the last sync was more than [minutes] ago (used when the Tasks tab opens). */
    suspend fun syncIfStale(context: Context, minutes: Long = 15): Result<Int> =
        if (isConnected(context) && System.currentTimeMillis() - lastSyncAt(context) > minutes * 60_000) sync(context)
        else Result.success(0)

    /** Returns how many new tasks were imported, or a failure with a readable message. */
    suspend fun sync(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        val base = baseUrl(context)
        val token = prefs(context).getString("token", null)
        if (base.isBlank() || token.isNullOrBlank()) return@withContext Result.failure(IOException("Canvas isn't connected."))

        try {
            val items = fetchPlannerItems(base, token)
            val dao = AppDatabase.get(context).taskDao()
            val existing = dao.canvasTasks().associateBy { it.canvasId }
            var added = 0

            for (item in items) {
                val current = existing[item.id]
                when {
                    current == null && !item.complete -> {
                        dao.insert(Task(title = item.title, canvasId = item.id, dueAt = item.dueAt, course = item.course, url = item.url))
                        added++
                    }
                    current != null -> {
                        val updated = current.copy(
                            title = item.title, dueAt = item.dueAt, course = item.course, url = item.url,
                            isDone = current.isDone || item.complete,
                        )
                        if (updated != current) dao.update(updated)
                    }
                }
            }
            prefs(context).edit { putLong("lastSync", System.currentTimeMillis()) }
            TaskChanges.onTasksChanged(context)
            Result.success(added)
        } catch (e: CanvasAuthException) {
            Result.failure(IOException("Canvas rejected the access token. Generate a new one and reconnect."))
        } catch (e: Exception) {
            Result.failure(IOException("Couldn't reach Canvas: ${e.message ?: e.javaClass.simpleName}"))
        }
    }

    private class CanvasAuthException : Exception()

    private data class CanvasItem(
        val id: String, val title: String, val dueAt: Long?, val course: String?, val url: String?, val complete: Boolean,
    )

    private fun fetchPlannerItems(base: String, token: String): List<CanvasItem> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().minusDays(7).atStartOfDay(zone).toInstant().toString()
        val end = LocalDate.now().plusDays(DAYS_AHEAD).atStartOfDay(zone).toInstant().toString()
        var next: String? = "$base/api/v1/planner/items?per_page=100" +
            "&start_date=${URLEncoder.encode(start, "UTF-8")}&end_date=${URLEncoder.encode(end, "UTF-8")}"
        val items = mutableListOf<CanvasItem>()
        var pages = 0
        while (next != null && pages++ < 10) {
            val connection = (URL(next).openConnection() as HttpURLConnection).apply {
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            try {
                when (connection.responseCode) {
                    401, 403 -> throw CanvasAuthException()
                    in 200..299 -> Unit
                    else -> throw IOException("HTTP ${connection.responseCode}")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(body)
                for (i in 0 until array.length()) parse(array.getJSONObject(i), base)?.let(items::add)
                next = nextPageLink(connection.getHeaderField("Link"))
            } finally {
                connection.disconnect()
            }
        }
        return items
    }

    private val TYPES = setOf("assignment", "quiz", "discussion_topic", "planner_note")

    private fun parse(json: JSONObject, base: String): CanvasItem? {
        val type = json.optString("plannable_type")
        if (type !in TYPES) return null
        val plannable = json.optJSONObject("plannable") ?: return null
        val title = plannable.optString("title").ifBlank { plannable.optString("name") }.ifBlank { return null }
        val dueText = plannable.optString("due_at").takeIf { it.isNotBlank() && it != "null" }
            ?: plannable.optString("todo_date").takeIf { it.isNotBlank() && it != "null" }
            ?: json.optString("plannable_date").takeIf { it.isNotBlank() && it != "null" }
        val submitted = json.optJSONObject("submissions")?.optBoolean("submitted", false) ?: false
        val markedDone = json.optJSONObject("planner_override")?.optBoolean("marked_complete", false) ?: false
        val path = json.optString("html_url").takeIf { it.isNotBlank() && it != "null" }
        return CanvasItem(
            id = "$type:${json.optString("plannable_id")}",
            title = title,
            dueAt = dueText?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() },
            course = json.optString("context_name").takeIf { it.isNotBlank() && it != "null" },
            url = path?.let { if (it.startsWith("http")) it else base + it },
            complete = submitted || markedDone,
        )
    }

    /** Canvas pages results with a Link header: <https://...>; rel="next" */
    private fun nextPageLink(header: String?): String? =
        header?.split(",")?.firstOrNull { it.contains("rel=\"next\"") }
            ?.substringAfter("<")?.substringBefore(">")
}
