package com.verdant.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.verdant.core.designsystem.theme.VerdantTheme
import com.verdant.core.model.Habit
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Shown by the home launcher when the user adds any Verdant widget.
 *
 * Detects the widget type from [AppWidgetManager] to determine whether a habit
 * picker is needed:
 *  - Habit-specific widgets (HabitGrid, MiniHeatmap) → show full habit picker
 *  - All-habits widgets (Checklist, Summary, BarChart, RadialRing, Streak, Quote)
 *    → show simple "Add Widget" confirmation
 */
@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {

    private val viewModel: WidgetConfigViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(RESULT_CANCELED)

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Determine the selection mode based on widget type
        val providerClass = AppWidgetManager.getInstance(this)
            .getAppWidgetInfo(appWidgetId)
            ?.provider?.className ?: ""

        val selectionMode = when {
            providerClass.endsWith("HabitGridWidgetReceiver")    ||
            providerClass.endsWith("MiniHeatmapWidgetReceiver")  ||
            providerClass.endsWith("QuickToggleWidgetReceiver")  ||
            providerClass.endsWith("TimerWidgetReceiver")        ||
            providerClass.endsWith("ProgressWidgetReceiver")     -> SelectionMode.SINGLE
            providerClass.endsWith("MultiHabitWidgetReceiver")   -> SelectionMode.MULTI
            else                                                  -> SelectionMode.NONE
        }

        setContent {
            VerdantTheme {
                val habits          by viewModel.habits.collectAsStateWithLifecycle()
                val selectedHabitId by viewModel.selectedHabitId.collectAsStateWithLifecycle()
                val selectedIds     by viewModel.selectedHabitIds.collectAsStateWithLifecycle()
                val colorTheme      by viewModel.colorTheme.collectAsStateWithLifecycle()
                val gridDensity     by viewModel.gridDensity.collectAsStateWithLifecycle()

                WidgetConfigScreen(
                    selectionMode   = selectionMode,
                    habits          = habits,
                    selectedHabitId = selectedHabitId,
                    selectedIds     = selectedIds,
                    colorTheme      = colorTheme,
                    gridDensity     = gridDensity,
                    onSelectHabit   = viewModel::selectHabit,
                    onToggleHabit   = viewModel::toggleMultiHabit,
                    onColorTheme    = viewModel::setColorTheme,
                    onGridDensity   = viewModel::setGridDensity,
                    onConfirm = {
                        lifecycleScope.launch {
                            val habitId = when (selectionMode) {
                                SelectionMode.SINGLE -> selectedHabitId ?: return@launch
                                else                 -> null
                            }
                            val multiIds = when (selectionMode) {
                                SelectionMode.MULTI -> {
                                    if (selectedIds.isEmpty()) return@launch
                                    selectedIds.joinToString(",")
                                }
                                else -> null
                            }
                            saveWidgetConfig(
                                appWidgetId = appWidgetId,
                                habitId     = habitId,
                                multiIds    = multiIds,
                                colorTheme  = colorTheme,
                                gridDensity = gridDensity,
                            )
                            setResult(
                                RESULT_OK,
                                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                            )
                            finish()
                        }
                    },
                    onCancel = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                )
            }
        }
    }

    private suspend fun saveWidgetConfig(
        appWidgetId: Int,
        habitId: String?,
        multiIds: String?,
        colorTheme: String,
        gridDensity: String,
    ) {
        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)

        updateAppWidgetState(this, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                if (habitId != null)  this[WidgetPreferencesKeys.HABIT_ID]       = habitId
                if (multiIds != null) this[WidgetPreferencesKeys.MULTI_HABIT_IDS] = multiIds
                this[WidgetPreferencesKeys.COLOR_THEME]   = colorTheme
                this[WidgetPreferencesKeys.GRID_DENSITY]  = gridDensity
                this[WidgetPreferencesKeys.APP_WIDGET_ID] = appWidgetId
            }
        }

        WorkManager.getInstance(this).enqueueUniqueWork(
            "widget_init_$appWidgetId",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build(),
        )
    }

    enum class SelectionMode { NONE, SINGLE, MULTI }
}

// ── UI ────────────────────────────────────────────────────────────────────────

@Composable
private fun WidgetConfigScreen(
    selectionMode: WidgetConfigActivity.SelectionMode,
    habits: List<Habit>,
    selectedHabitId: String?,
    selectedIds: Set<String>,
    colorTheme: String,
    gridDensity: String,
    onSelectHabit: (String) -> Unit,
    onToggleHabit: (String) -> Unit,
    onColorTheme: (String) -> Unit,
    onGridDensity: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val canConfirm = when (selectionMode) {
        WidgetConfigActivity.SelectionMode.SINGLE -> selectedHabitId != null
        WidgetConfigActivity.SelectionMode.MULTI  -> selectedIds.isNotEmpty()
        WidgetConfigActivity.SelectionMode.NONE   -> true
    }

    val title = when (selectionMode) {
        WidgetConfigActivity.SelectionMode.SINGLE -> "Add Habit Widget"
        WidgetConfigActivity.SelectionMode.MULTI  -> "Add Multi-Habit Widget"
        WidgetConfigActivity.SelectionMode.NONE   -> "Add Widget"
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column {
            // ── Title bar ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onCancel) { Text("Cancel") }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (selectionMode) {
                    WidgetConfigActivity.SelectionMode.SINGLE -> {
                        // ── Single-habit picker ───────────────────────────────
                        item {
                            Text(
                                "Select a habit",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        if (habits.isEmpty()) {
                            item {
                                Text(
                                    "No active habits. Create one in the app first.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        items(habits, key = { it.id }) { habit ->
                            HabitPickerCard(
                                habit      = habit,
                                isSelected = habit.id == selectedHabitId,
                                onClick    = { onSelectHabit(habit.id) },
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            Text(
                                "Grid density",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("comfortable" to "Comfortable", "compact" to "Compact").forEach { (key, label) ->
                                    FilterChip(
                                        selected = gridDensity == key,
                                        onClick  = { onGridDensity(key) },
                                        label    = { Text(label) },
                                    )
                                }
                            }
                        }
                    }

                    WidgetConfigActivity.SelectionMode.MULTI -> {
                        // ── Multi-habit picker (checkboxes, up to 5) ──────────
                        item {
                            Text(
                                "Select up to 5 habits (${selectedIds.size}/5 selected)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        if (habits.isEmpty()) {
                            item {
                                Text(
                                    "No active habits. Create one in the app first.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        items(habits, key = { it.id }) { habit ->
                            HabitMultiPickerCard(
                                habit      = habit,
                                isChecked  = habit.id in selectedIds,
                                enabled    = habit.id in selectedIds || selectedIds.size < 5,
                                onToggle   = { onToggleHabit(habit.id) },
                            )
                        }
                    }

                    WidgetConfigActivity.SelectionMode.NONE -> {
                        item {
                            Text(
                                "This widget shows data for all your habits automatically. " +
                                "Tap \"Add Widget\" to place it on your home screen.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }

            // ── Confirm button ────────────────────────────────────────────────
            Button(
                onClick  = onConfirm,
                enabled  = canConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Add Widget")
            }
        }
    }
}

@Composable
private fun HabitMultiPickerCard(
    habit: Habit,
    isChecked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val habitColor = Color(habit.color.toInt())
    val alpha      = if (enabled) 1f else 0.4f

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isChecked) Modifier.border(2.dp, habitColor, RoundedCornerShape(20.dp))
                else Modifier
            )
            .clickable(enabled = enabled, onClick = onToggle),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(if (isChecked) 4.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(habitColor.copy(alpha = 0.15f * alpha)),
                contentAlignment = Alignment.Center,
            ) {
                Text(habit.icon.ifEmpty { "🌱" }, style = MaterialTheme.typography.titleMedium)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = habit.name,
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                val habitLabel = habit.label
                if (!habitLabel.isNullOrBlank()) {
                    Text(
                        text  = habitLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = habitColor,
                    )
                }
            }
            Checkbox(
                checked  = isChecked,
                enabled  = enabled,
                onCheckedChange = { onToggle() },
            )
        }
    }
}

@Composable
private fun HabitPickerCard(
    habit: Habit,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val habitColor = Color(habit.color.toInt())

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(2.dp, habitColor, RoundedCornerShape(20.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(if (isSelected) 4.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(habitColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(habit.icon.ifEmpty { "🌱" }, style = MaterialTheme.typography.titleMedium)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = habit.name,
                    style    = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val habitLabel = habit.label
                if (!habitLabel.isNullOrBlank()) {
                    Text(
                        text  = habitLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = habitColor,
                    )
                }
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(habitColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
    }
}
