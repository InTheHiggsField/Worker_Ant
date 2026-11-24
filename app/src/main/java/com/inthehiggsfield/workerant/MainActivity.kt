package com.inthehiggsfield.workerant

import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.inthehiggsfield.workerant.ui.blocker.BlockerScreen
import com.inthehiggsfield.workerant.ui.calendar.CalendarScreen
import com.inthehiggsfield.workerant.ui.notes.NotesScreen
import com.inthehiggsfield.workerant.ui.settings.SettingsScreen
import com.inthehiggsfield.workerant.ui.tasks.TasksScreen
import com.inthehiggsfield.workerant.ui.theme.AppTheme
import com.inthehiggsfield.workerant.ui.theme.ThemePrefs
import com.inthehiggsfield.workerant.widget.WorkerAntWidget
import com.inthehiggsfield.workerant.ui.theme.WorkerAntTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Re-arm task reminders and refresh the home screen widget on every launch.
        lifecycleScope.launch { TaskChanges.onTasksChanged(this@MainActivity) }
        setContent {
            var theme by remember { mutableStateOf(ThemePrefs.load(this@MainActivity)) }

            // Dark themes need light status-bar icons and vice versa.
            val dark = theme.forcedDark ?: isSystemInDarkTheme()
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                            else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }

            WorkerAntTheme(theme) {
                WorkerAntApp(
                    theme = theme,
                    onThemeChange = {
                        theme = it
                        ThemePrefs.save(this@MainActivity, it)
                        lifecycleScope.launch { WorkerAntWidget.refresh(this@MainActivity) }
                    },
                )
            }
        }
    }
}

/** The bottom-bar tabs. `route` is the name the navigator uses for each screen. */
private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Tasks("tasks", "Tasks", Icons.Default.CheckCircle),
    Calendar("calendar", "Calendar", Icons.Default.DateRange),
    Notes("notes", "Notes", Icons.Default.Edit),
    Blocker("blocker", "Blocker", Icons.Default.Lock),
    Settings("settings", "Settings", Icons.Default.Settings),
}

@Composable
fun WorkerAntApp(theme: AppTheme, onThemeChange: (AppTheme) -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                // Standard bottom-nav behavior: don't stack up copies of tabs,
                                // and remember each tab's scroll/state when switching.
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Tasks.route,
            // consumeWindowInsets: screens that add keyboard padding (Notes) don't also
            // re-add the space already taken by the bottom bar.
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            composable(Tab.Tasks.route) { TasksScreen(theme = theme) }
            composable(Tab.Calendar.route) { CalendarScreen() }
            composable(Tab.Notes.route) { NotesScreen() }
            composable(Tab.Blocker.route) { BlockerScreen() }
            composable(Tab.Settings.route) { SettingsScreen(current = theme, onSelect = onThemeChange) }
        }
    }
}
