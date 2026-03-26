package com.verdant.feature.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import compose.icons.TablerIcons
import compose.icons.tablericons.Check
import compose.icons.tablericons.MapPin
import compose.icons.tablericons.PlayerPause
import compose.icons.tablericons.PlayerPlay
import compose.icons.tablericons.Send
import compose.icons.tablericons.Stars
import compose.icons.tablericons.X
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdant.core.ai.BrainDumpAction
import com.verdant.core.designsystem.component.CompletionRing
import com.verdant.core.designsystem.component.OrbitalDecayChart
import com.verdant.core.designsystem.component.OrbitalHabitData
import com.verdant.core.designsystem.component.StreakBadge
import com.verdant.core.designsystem.component.toDaysAgoLabel
import com.verdant.core.model.TrackingType

@Composable
fun HomeScreen(
    onNavigateToHabitDetail: (String) -> Unit = {},
    onCreateHabit: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val brainDump by viewModel.brainDumpState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var customValueDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var financialDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var financialAmount by rememberSaveable { mutableStateOf("") }
    var financialCategory by rememberSaveable { mutableStateOf("") }

    // Pending location check-in item — set when user taps check-in without permission
    var pendingLocationItem by rememberSaveable { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            pendingLocationItem?.let { habitId ->
                state.todayItems.find { it.habit.id == habitId }?.let { item ->
                    viewModel.checkIn(item)
                }
            }
        }
        pendingLocationItem = null
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            // Only show header and insight card when user has habits
            if (state.hasAnyHabits) {
                item {
                    HomeHeader(
                        greeting = state.greeting,
                        formattedDate = state.formattedDate,
                        completedCount = state.completedCount,
                        totalCount = state.totalCount,
                    )
                }

                item {
                    AiInsightCard(
                        insightText = state.aiInsight,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                item {
                    BrainDumpBar(
                        text = brainDump.text,
                        isLoading = brainDump.isLoading,
                        onTextChange = viewModel::onBrainDumpTextChange,
                        onSubmit = viewModel::onBrainDumpSubmit,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                item {
                    AnimatedVisibility(
                        visible = brainDump.result != null || brainDump.error != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        if (brainDump.result != null) {
                            BrainDumpResultCard(
                                result = brainDump.result!!,
                                onConfirm = viewModel::onBrainDumpConfirm,
                                onDismiss = viewModel::onBrainDumpDismiss,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        } else if (brainDump.error != null) {
                            Card(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Row(
                                    Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        brainDump.error!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(onClick = viewModel::onBrainDumpDismiss) {
                                        Icon(TablerIcons.X, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                if (state.todayItems.isNotEmpty()) {
                    Text(
                        text = "Today's habits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                    )
                }
            }

            if (state.todayItems.isEmpty() && !state.isLoading) {
                item {
                    if (!state.hasAnyHabits) {
                        WelcomeEmptyState(onCreateHabit = onCreateHabit)
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("🌱", style = MaterialTheme.typography.displayMedium)
                                Text(
                                    "No habits scheduled for today",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // ── Orbital View (EVENT_DRIVEN habits) ──────────────────────────
            if (state.eventDrivenItems.isNotEmpty()) {
                item {
                    OrbitalViewSection(
                        habits = state.eventDrivenItems,
                        onLogDone = { habitId -> viewModel.logEventDriven(habitId) },
                        onNavigateToDetail = onNavigateToHabitDetail,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            items(state.todayItems, key = { it.habit.id }) { item ->
                val cardMod = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                when (item.habit.trackingType) {
                    TrackingType.BINARY -> BinaryHabitCard(
                        item = item, onToggle = { viewModel.toggleBinary(item) },
                        onTap = { onNavigateToHabitDetail(item.habit.id) }, modifier = cardMod,
                    )
                    TrackingType.QUANTITATIVE -> QuantHabitCard(
                        item = item, onAdd = { viewModel.addQuantitative(item, it) },
                        onCustom = { customValueDialog = item.habit.id },
                        onTap = { onNavigateToHabitDetail(item.habit.id) }, modifier = cardMod,
                    )
                    TrackingType.DURATION -> DurationHabitCard(
                        item = item,
                        timerRunning = item.habit.id in state.timerRunning,
                        elapsedSeconds = state.timerSeconds[item.habit.id] ?: 0,
                        onStartStop = {
                            if (item.habit.id in state.timerRunning) viewModel.stopAndLogTimer(item)
                            else viewModel.startTimer(item.habit.id)
                        },
                        onManual = { customValueDialog = item.habit.id },
                        onTap = { onNavigateToHabitDetail(item.habit.id) }, modifier = cardMod,
                    )
                    TrackingType.FINANCIAL -> FinancialHabitCard(
                        item = item, onLogExpense = { financialDialog = item.habit.id },
                        onTap = { onNavigateToHabitDetail(item.habit.id) }, modifier = cardMod,
                    )
                    TrackingType.LOCATION -> LocationHabitCard(
                        item = item,
                        onCheckIn = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                viewModel.checkIn(item)
                            } else {
                                pendingLocationItem = item.habit.id
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ),
                                )
                            }
                        },
                        onTap = { onNavigateToHabitDetail(item.habit.id) }, modifier = cardMod,
                    )
                }
            }
        }
    }

    // Custom value dialog
    if (customValueDialog != null) {
        val item = state.todayItems.find { it.habit.id == customValueDialog }
        if (item != null) {
            var inputText by rememberSaveable(customValueDialog) { mutableStateOf("") }
            val isDuration = item.habit.trackingType == TrackingType.DURATION
            AlertDialog(
                onDismissRequest = { customValueDialog = null },
                title = { Text(if (isDuration) "Enter duration" else "Enter value") },
                text = {
                    OutlinedTextField(
                        value = inputText, onValueChange = { inputText = it },
                        label = { Text(if (isDuration) "Minutes" else item.habit.unit?.let { "Value ($it)" } ?: "Value") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val v = inputText.toDoubleOrNull() ?: 0.0
                        if (isDuration) viewModel.setDurationManually(item, v)
                        else viewModel.setQuantitative(item, v)
                        customValueDialog = null
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { customValueDialog = null }) { Text("Cancel") } },
            )
        }
    }

    // Financial dialog
    if (financialDialog != null) {
        val item = state.todayItems.find { it.habit.id == financialDialog }
        if (item != null) {
            AlertDialog(
                onDismissRequest = { financialDialog = null; financialAmount = ""; financialCategory = "" },
                title = { Text("Log expense") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = financialAmount, onValueChange = { financialAmount = it },
                            label = { Text("Amount") }, prefix = { Text("₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = financialCategory, onValueChange = { financialCategory = it },
                            label = { Text("Category (optional)") },
                            placeholder = { Text("e.g. Food, Transport") }, singleLine = true,
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.logFinancial(item, financialAmount.toDoubleOrNull() ?: 0.0, financialCategory.takeIf { it.isNotBlank() })
                            financialDialog = null; financialAmount = ""; financialCategory = ""
                        },
                        enabled = financialAmount.toDoubleOrNull() != null,
                    ) { Text("Log") }
                },
                dismissButton = {
                    TextButton(onClick = { financialDialog = null; financialAmount = ""; financialCategory = "" }) { Text("Cancel") }
                },
            )
        }
    }
}

// ── Orbital View Section ──────────────────────────────────────────────────────

@Composable
private fun OrbitalViewSection(
    habits: List<OrbitalHabitData>,
    onLogDone: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🪐", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Recurring habits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
            }
            OrbitalDecayChart(
                habits = habits,
                modifier = Modifier.fillMaxWidth().height(260.dp),
            )
            // Habit list with "Done" buttons below the chart
            habits.forEach { habit ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(habit.habitIcon.ifEmpty { "🌀" }, style = MaterialTheme.typography.titleMedium)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            habit.habitName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            habit.daysSinceLast.toDaysAgoLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalButton(
                        onClick = { onLogDone(habit.habitId) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Done", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(
    greeting: String,
    formattedDate: String,
    completedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(600), label = "progress")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
            )
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$completedCount / $totalCount done today",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            CompletionRing(
                progress = animatedProgress,
                color = MaterialTheme.colorScheme.primary,
                size = 72.dp,
                strokeWidth = 6.dp,
            )
        }
    }
}

@Composable
private fun AiInsightCard(
    insightText: String?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(TablerIcons.Stars, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
            Column {
                Text("AI Insight", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                Text(
                    insightText ?: "Start tracking habits to get personalized insights!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Card shell ────────────────────────────────────────────────────────────────

@Composable
private fun HabitCardShell(
    icon: String,
    name: String,
    label: String?,
    habitColor: Color,
    streak: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit,
) {
    ElevatedCard(onClick = onTap, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.elevatedCardElevation(1.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(habitColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Text(icon.ifEmpty { "🌱" }, style = MaterialTheme.typography.titleLarge) }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!label.isNullOrBlank()) LabelPill(label = label, color = habitColor)
                }
                StreakBadge(count = streak)
            }
            actions()
        }
    }
}

// ── Binary ────────────────────────────────────────────────────────────────────

@Composable
private fun BinaryHabitCard(item: TodayHabitItem, onToggle: () -> Unit, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val habitColor = Color(item.habit.color)
    val isCompleted = item.entry?.completed == true
    val animatedColor by animateColorAsState(
        targetValue = if (isCompleted) habitColor else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(300), label = "check_color",
    )
    HabitCardShell(icon = item.habit.icon, name = item.habit.name, label = item.habit.label, habitColor = habitColor, streak = item.streak, onTap = onTap, modifier = modifier) {
        Button(
            onClick = onToggle, modifier = Modifier.fillMaxWidth().height(44.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = animatedColor,
                contentColor = if (isCompleted) (if (habitColor.luminance() > 0.4f) Color.Black else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(TablerIcons.Check, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (isCompleted) "Done ✓" else "Mark done")
        }
    }
}

// ── Quantitative ──────────────────────────────────────────────────────────────

@Composable
private fun QuantHabitCard(item: TodayHabitItem, onAdd: (Double) -> Unit, onCustom: () -> Unit, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val habitColor = Color(item.habit.color)
    val current = item.entry?.value ?: 0.0
    val target = item.habit.targetValue
    val progress = if (target != null && target > 0) (current / target).toFloat().coerceIn(0f, 1f) else 0f
    val unit = item.habit.unit?.let { " $it" } ?: ""

    HabitCardShell(icon = item.habit.icon, name = item.habit.name, label = item.habit.label, habitColor = habitColor, streak = item.streak, onTap = onTap, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${current.fmt()}$unit", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = habitColor)
                if (target != null) Text("/ ${target.fmt()}$unit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(
                progress = { progress }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                color = habitColor, trackColor = habitColor.copy(alpha = 0.15f), strokeCap = StrokeCap.Round,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(1.0, 5.0, 10.0).forEach { delta ->
                FilledTonalButton(onClick = { onAdd(delta) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp, 8.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("+${delta.fmt()}", style = MaterialTheme.typography.labelMedium)
                }
            }
            FilledTonalButton(onClick = onCustom, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp, 8.dp), shape = RoundedCornerShape(12.dp)) {
                Text("Custom", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ── Duration ──────────────────────────────────────────────────────────────────

@Composable
private fun DurationHabitCard(item: TodayHabitItem, timerRunning: Boolean, elapsedSeconds: Int, onStartStop: () -> Unit, onManual: () -> Unit, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val habitColor = Color(item.habit.color)
    val current = item.entry?.value ?: 0.0
    val target = item.habit.targetValue
    val progress = if (target != null && target > 0) (current / target).toFloat().coerceIn(0f, 1f) else 0f

    HabitCardShell(icon = item.habit.icon, name = item.habit.name, label = item.habit.label, habitColor = habitColor, streak = item.streak, onTap = onTap, modifier = modifier) {
        if (target != null) {
            LinearProgressIndicator(
                progress = { progress }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                color = habitColor, trackColor = habitColor.copy(alpha = 0.15f), strokeCap = StrokeCap.Round,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = if (timerRunning) elapsedSeconds.toTimerStr() else current.toMinuteStr(),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    color = if (timerRunning) habitColor else MaterialTheme.colorScheme.onSurface,
                )
            }
            FilledIconButton(
                onClick = onStartStop, modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (timerRunning) habitColor else MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Icon(
                    if (timerRunning) TablerIcons.PlayerPause else TablerIcons.PlayerPlay,
                    if (timerRunning) "Stop" else "Start",
                    tint = if (timerRunning) (if (habitColor.luminance() > 0.4f) Color.Black else Color.White) else MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            TextButton(onClick = onManual) { Text("Manual") }
        }
    }
}

// ── Financial ─────────────────────────────────────────────────────────────────

@Composable
private fun FinancialHabitCard(item: TodayHabitItem, onLogExpense: () -> Unit, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val habitColor = Color(item.habit.color)
    val spent = item.entry?.value ?: 0.0
    val budget = item.habit.targetValue
    val progress = if (budget != null && budget > 0) (spent / budget).toFloat().coerceIn(0f, 1f) else 0f
    val overBudget = progress >= 1f

    HabitCardShell(icon = item.habit.icon, name = item.habit.name, label = item.habit.label, habitColor = habitColor, streak = item.streak, onTap = onTap, modifier = modifier) {
        if (budget != null) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("₹${spent.fmt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = if (overBudget) MaterialTheme.colorScheme.error else habitColor)
                    Text("/ ₹${budget.fmt()} budget", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LinearProgressIndicator(
                    progress = { progress }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                    color = if (overBudget) MaterialTheme.colorScheme.error else habitColor,
                    trackColor = habitColor.copy(alpha = 0.15f), strokeCap = StrokeCap.Round,
                )
            }
        }
        Button(onClick = onLogExpense, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = habitColor), shape = RoundedCornerShape(12.dp)) {
            Text("Log expense", color = if (habitColor.luminance() > 0.4f) Color.Black else Color.White)
        }
    }
}

// ── Location ──────────────────────────────────────────────────────────────────

@Composable
private fun LocationHabitCard(item: TodayHabitItem, onCheckIn: () -> Unit, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val habitColor = Color(item.habit.color)
    val checkedIn = item.entry?.completed == true

    HabitCardShell(icon = item.habit.icon, name = item.habit.name, label = item.habit.label, habitColor = habitColor, streak = item.streak, onTap = onTap, modifier = modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            item.entry?.value?.let { Text("${it.fmt()} km today", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)) } ?: Spacer(Modifier.weight(1f))
            Button(
                onClick = onCheckIn,
                colors = ButtonDefaults.buttonColors(containerColor = if (checkedIn) habitColor else MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(TablerIcons.MapPin, null, Modifier.size(16.dp), tint = if (checkedIn) (if (habitColor.luminance() > 0.4f) Color.Black else Color.White) else MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(4.dp))
                Text(if (checkedIn) "Checked in ✓" else "Check in", color = if (checkedIn) (if (habitColor.luminance() > 0.4f) Color.Black else Color.White) else MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

// ── Welcome Empty State ──────────────────────────────────────────────────

@Composable
private fun WelcomeEmptyState(onCreateHabit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("🌱", style = MaterialTheme.typography.displayLarge)
        Text(
            text = "Your habit garden is empty",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(
            text = "Start with one small habit. You can always add more later.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onCreateHabit,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text("Create your first habit", style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
    }
}

// ── Brain Dump ────────────────────────────────────────────────────────────────

@Composable
private fun BrainDumpBar(
    text: String,
    isLoading: Boolean,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "What did you do today? Just type it out...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                textStyle = MaterialTheme.typography.bodySmall,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(onSend = {
                    keyboard?.hide()
                    onSubmit()
                }),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(12.dp),
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                FilledIconButton(
                    onClick = {
                        keyboard?.hide()
                        onSubmit()
                    },
                    modifier = Modifier.size(36.dp),
                    enabled = text.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Icon(TablerIcons.Send, "Log", Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun BrainDumpResultCard(
    result: com.verdant.core.ai.ParsedBrainDump,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(TablerIcons.Stars, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Ready to log",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(TablerIcons.X, "Dismiss", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (result.entries.isEmpty()) {
                Text(
                    "No matching habits found. Try mentioning your habit names more explicitly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                result.entries.forEach { entry ->
                    val isSkipped = entry.action == BrainDumpAction.SKIPPED
                    val entryColor = if (isSkipped) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(entryColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (isSkipped) TablerIcons.X else TablerIcons.Check,
                                null,
                                Modifier.size(14.dp),
                                tint = entryColor,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                entry.habitName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            val detail = buildString {
                                if (isSkipped) {
                                    append("Skipped")
                                    entry.skipReason?.let { append(" — $it") }
                                } else {
                                    append("Logged")
                                    if (entry.value != null) {
                                        val v = entry.value
                                        val vStr = if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v)
                                        append(" $vStr")
                                        entry.unit?.let { append(" $it") }
                                    }
                                }
                            }
                            Text(
                                detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(entryColor.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                if (isSkipped) "Skip" else "Done",
                                style = MaterialTheme.typography.labelSmall,
                                color = entryColor,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            if (result.unmatchedMentions.isNotEmpty()) {
                Text(
                    "Not matched: ${result.unmatchedMentions.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }

            if (result.entries.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Discard")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(TablerIcons.Check, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Confirm & Log")
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun LabelPill(label: String, color: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Medium)
    }
}

private fun Double.fmt(): String = if (this == toLong().toDouble()) toLong().toString() else "%.1f".format(this)
private fun Int.toTimerStr(): String = "%02d:%02d".format(this / 60, this % 60)
private fun Double.toMinuteStr(): String {
    val total = (this * 60).toInt()
    return if (total % 60 == 0) "${total / 60}m" else "%dm %02ds".format(total / 60, total % 60)
}
