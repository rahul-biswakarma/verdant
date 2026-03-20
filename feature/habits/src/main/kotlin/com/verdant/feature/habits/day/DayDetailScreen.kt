package com.verdant.feature.habits.day

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdant.core.model.Habit
import com.verdant.core.model.HabitEntry
import com.verdant.core.model.TrackingType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DayDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedHabit by rememberSaveable { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                title = {
                    Text(
                        text = state.date?.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")) ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )

            // Completion summary
            val done = state.items.count { it.second?.completed == true }
            val total = state.items.size
            if (total > 0) {
                Text(
                    text = "$done / $total habits completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (state.items.isEmpty() && !state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No habits were scheduled for this day.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.items, key = { it.first.id }) { (habit, entry) ->
                        DayHabitRow(
                            habit = habit,
                            entry = entry,
                            onClick = { selectedHabit = habit.id },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                        )
                    }
                }
            }
        }
    }

    // Entry editing bottom sheet
    if (selectedHabit != null) {
        val item = state.items.find { it.first.id == selectedHabit }
        if (item != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedHabit = null },
                sheetState = sheetState,
            ) {
                DayEntrySheet(
                    habit = item.first,
                    existing = item.second,
                    onSaveBinary = { completed ->
                        viewModel.logBinary(item.first, completed)
                        selectedHabit = null
                    },
                    onSaveQuantitative = { value ->
                        viewModel.logQuantitative(item.first, value)
                        selectedHabit = null
                    },
                    onSkip = {
                        viewModel.skip(item.first)
                        selectedHabit = null
                    },
                    onDismiss = { selectedHabit = null },
                )
            }
        }
    }
}

@Composable
private fun DayHabitRow(
    habit: Habit,
    entry: HabitEntry?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val habitColor = Color(habit.color)
    val isCompleted = entry?.completed == true
    val isSkipped = entry?.skipped == true

    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.elevatedCardElevation(1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(habitColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Text(habit.icon.ifEmpty { "🌱" }, style = MaterialTheme.typography.titleMedium) }

            Column(modifier = Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val statusText = when {
                    isSkipped -> "Skipped"
                    isCompleted && habit.trackingType == TrackingType.BINARY -> "Completed"
                    entry?.value != null -> "${entry.value} ${habit.unit.orEmpty()}"
                    isCompleted -> "Done"
                    else -> "Tap to log"
                }
                Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Box(
                modifier = Modifier.size(10.dp).clip(CircleShape).background(
                    when {
                        isSkipped -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        isCompleted -> habitColor
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            )
        }
    }
}

@Composable
private fun DayEntrySheet(
    habit: Habit,
    existing: HabitEntry?,
    onSaveBinary: (Boolean) -> Unit,
    onSaveQuantitative: (Double) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    val habitColor = Color(habit.color)
    var valueInput by rememberSaveable { mutableStateOf(existing?.value?.let {
        if (it == it.toLong().toDouble()) it.toLong().toString() else "%.1f".format(it)
    } ?: "") }

    Column(
        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(habit.icon, style = MaterialTheme.typography.titleLarge)
            Text(habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        when (habit.trackingType) {
            TrackingType.BINARY -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onSaveBinary(true) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (existing?.completed == true) habitColor else MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) { Text("Done ✓") }
                    Button(
                        onClick = { onSaveBinary(false) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) { Text("Not done") }
                }
            }
            else -> {
                OutlinedTextField(
                    value = valueInput,
                    onValueChange = { valueInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Value${habit.unit?.let { " ($it)" } ?: ""}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                )
                Button(
                    onClick = { onSaveQuantitative(valueInput.toDoubleOrNull() ?: 0.0) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = valueInput.toDoubleOrNull() != null,
                    colors = ButtonDefaults.buttonColors(containerColor = habitColor),
                ) {
                    Text("Save", color = if (habitColor.luminance() > 0.4f) Color.Black else Color.White)
                }
            }
        }

        Row {
            TextButton(onClick = onSkip) { Text("Skip") }
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    }
}
