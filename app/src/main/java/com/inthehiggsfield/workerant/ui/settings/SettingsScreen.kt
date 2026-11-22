package com.inthehiggsfield.workerant.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inthehiggsfield.workerant.ui.theme.AppTheme
import com.inthehiggsfield.workerant.ui.theme.ThemeCategory
import com.inthehiggsfield.workerant.ui.theme.colorScheme
import com.inthehiggsfield.workerant.ui.theme.shapes
import com.inthehiggsfield.workerant.ui.theme.typography

@Composable
fun SettingsScreen(
    current: AppTheme,
    onSelect: (AppTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    val systemDark = isSystemInDarkTheme()
    // null = show every category
    var filter by rememberSaveable { mutableStateOf<ThemeCategory?>(null) }
    val categories = if (filter == null) ThemeCategory.entries else listOf(filter!!)

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            CategoryFilter(selected = filter, onSelect = { filter = it })
        }
        categories.forEach { category ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "${category.label} (${AppTheme.entries.count { it.category == category }})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(AppTheme.entries.filter { it.category == category }, key = { it.name }) { theme ->
                ThemeCard(theme, selected = theme == current, systemDark = systemDark) { onSelect(theme) }
            }
        }
    }
}

/** Scrollable row of pills: All, Basic colors, Cute & girly, Fandom-inspired, ... */
@Composable
private fun CategoryFilter(selected: ThemeCategory?, onSelect: (ThemeCategory?) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (listOf<ThemeCategory?>(null) + ThemeCategory.entries).forEach { option ->
            val isSelected = option == selected
            Text(
                option?.label ?: "All",
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) colors.primary else colors.surfaceVariant)
                    .border(1.dp, if (isSelected) Color.Transparent else colors.outlineVariant, CircleShape)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

/** A mini preview of a theme, drawn in that theme's own colors, font, and corner shape. */
@Composable
private fun ThemeCard(theme: AppTheme, selected: Boolean, systemDark: Boolean, onClick: () -> Unit) {
    val scheme = theme.colorScheme(systemDark)
    val typography = remember(theme) { theme.typography() }
    val shape = remember(theme) { theme.shapes().medium }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.background)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else scheme.outlineVariant,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(theme.label, style = typography.titleMedium, color = scheme.onBackground,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(theme.tagline, style = typography.bodySmall, color = scheme.onSurfaceVariant,
            maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(scheme.primary, scheme.secondary, scheme.tertiary, scheme.surfaceVariant).forEach { color ->
                Box(Modifier.size(18.dp).background(color, CircleShape).border(1.dp, scheme.outline.copy(alpha = 0.4f), CircleShape))
            }
        }
        if (selected) {
            Spacer(Modifier.height(6.dp))
            Text("✓ In use", style = typography.labelSmall, color = scheme.primary)
        }
    }
}
