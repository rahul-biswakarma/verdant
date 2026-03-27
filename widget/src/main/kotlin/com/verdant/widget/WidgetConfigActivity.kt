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

        // Determine if this widget type requires a specific habit selection
        val providerClass = AppWidgetManager.getInstance(this)
            .getAppWidgetInfo(appWidgetId)
            ?.provider?.className ?: ""
        val needsHabit = providerClass.endsWith("HabitGridWidgetReceiver") ||
                         providerClass.endsWith("MiniHeatmapWidgetReceiver") ||
                         providerClass.endsWith("QuickToggleWidgetReceiver") ||
                         providerClass.endsWith("TimerWidgetReceiver") ||
                         providerClass.endsWith("HabitStreakWidgetReceiver")
        val needsMultiHabit = providerClass.endsWith("MultiHabitWidgetReceiver")

        setContent {
            VerdantTheme {
                val habits           by viewModel.habits.collectAsStateWithLifecycle()
                val selectedHabitId  by viewModel.selectedHabitId.collectAsStateWithLifecycle()
                val selectedHabitIds by viewModel.selectedHabitIds.collectAsStateWithLifecycle()
                val colorTheme       by viewModel.colorTheme.collectAsStateWithLifecycle()
                val gridDensity      by viewModel.gridDensity.collectAsStateWithLifecycle()

                WidgetConfigScreen(
                    needsHabit       = needsHabit,
                    needsMultiHabit  = needsMultiHabit,
                    habits           = habits,
                    selectedHabitId  = selectedHabitId,
                    selectedHabitIds = selectedHabitIds,
                    colorTheme       = colorTheme,
                    gridDensity      = gridDensity,
                    onSelectHabit    = viewModel::selectHabit,
                    onToggleMulti    = viewModel::toggleMultiHabit,
                    onColorTheme     = viewModel::setColorTheme,
                    onGridDensity    = viewModel::setGridDensity,
                    onConfirm = {
                        when {
                            needsHabit -> {
                                val habitId = selectedHabitId ?: return@WidgetConfigScreen
                                lifecycleScope.launch {
                                    saveWidgetConfig(appWidgetId, habitId, null, colorTheme, gridDensity)
                                    setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                                    finish()
                                }
                            }
                            needsMultiHabit -> {
                                if (selectedHabitIds.isEmpty()) return@WidgetConfigScreen
                                lifecycleScope.launch {
                                    saveWidgetConfig(appWidgetId, null, selectedHabitIds, colorTheme, gridDensity)
                                    setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                                    finish()
                                }
                            }
                            else -> {
                                lifecycleScope.launch {
                                    saveWidgetConfig(appWidgetId, null, null, colorTheme, gridDensity)
                                    setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                                    finish()
                                }
                            }
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
        multiHabitIds: Set<String>?,
        colorTheme: String,
        gridDensity: String,
    ) {
        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)

        updateAppWidgetState(this, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                if (habitId != null) this[WidgetPreferencesKeys.HABIT_ID] = habitId
                if (multiHabitIds != null) {
                    this[WidgetPreferencesKeys.MULTI_HABIT_IDS] = multiHabitIds.joinToString(",")
                }
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
}

// ── UI ────────────────────────────────────────────────────────────────────────

@Composable
private fun WidgetConfigScreen(
    needsHabit: Boolean,
    needsMultiHabit: Boolean,
    habits: List<Habit>,
    selectedHabitId: String?,
    selectedHabitIds: Set<String>,
    colorTheme: String,
    gridDensity: String,
    onSelectHabit: (String) -> Unit,
    onToggleMulti: (String) -> Unit,
    onColorTheme: (String) -> Unit,
    onGridDensity: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
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
                    when {
                        needsHabit      -> "Add Habit Widget"
                        needsMultiHabit -> "Add Multi-Habit Widget"
                        else            -> "Add Widget"
                    },
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
                when {
                    needsHabit -> {
                        // ── Single habit picker ───────────────────────────────
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

                        // ── Grid density ──────────────────────────────────────
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

                    needsMultiHabit -> {
                        // ── Multi-habit picker (3–5 habits) ──────────────────
                        item {
                            Text(
                                "Select 3–5 habits",
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
                                isSelected = habit.id in selectedHabitIds,
                                onClick    = { onToggleMulti(habit.id) },
                            )
                        }
                    }

                    else -> {
                        // ── No-config widgets: just describe what will be added ───
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
                enabled  = when {
                    needsHabit      -> selectedHabitId != null
                    needsMultiHabit -> selectedHabitIds.size in 1..5
                    else            -> true
                },
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
