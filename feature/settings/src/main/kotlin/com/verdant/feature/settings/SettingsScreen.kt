package com.verdant.feature.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import compose.icons.TablerIcons
import compose.icons.tablericons.Bell
import compose.icons.tablericons.Check
import compose.icons.tablericons.Download
import compose.icons.tablericons.InfoCircle
import compose.icons.tablericons.Login
import compose.icons.tablericons.Logout
import compose.icons.tablericons.Settings
import compose.icons.tablericons.Share
import compose.icons.tablericons.ShieldLock
import compose.icons.tablericons.Trash
import compose.icons.tablericons.Upload
import compose.icons.tablericons.User
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdant.core.datastore.NudgeTone
import com.verdant.core.designsystem.theme.ThemeMode

// Preset accent colors
private val accentColors = listOf(
    0xFF5A7A60L to "Muted Sage",
    0xFF7B6B6BL to "Dusty Mauve",
    0xFFE8673CL to "Burnt Orange",
    0xFF6B8E8AL to "Warm Teal",
    0xFF8B7355L to "Warm Bronze",
    0xFF9B6B6BL to "Dusty Rose",
    0xFF5A6B7AL to "Slate",
    0xFF8A7B5AL to "Olive",
)

private val daysOfWeek = listOf(
    1 to "Monday", 2 to "Tuesday", 3 to "Wednesday", 4 to "Thursday",
    5 to "Friday", 6 to "Saturday", 7 to "Sunday",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToOnboarding: () -> Unit = {},
    webClientId: String = "",
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Notification permission request (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            viewModel.setNotificationsEnabled(false)
        }
    }

    // Snackbar
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // File launchers
    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.let { stream ->
                viewModel.exportJson(stream)
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.let { stream ->
                viewModel.exportCsv(stream)
            }
        }
    }

    val importCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importCsv(it) }
    }

    if (uiState.showDeleteConfirmDialog) {
        DeleteAllDataDialog(
            onConfirm = {
                viewModel.deleteAllData {
                    viewModel.dismissDeleteConfirmDialog()
                    onNavigateToOnboarding()
                }
            },
            onDismiss = viewModel::dismissDeleteConfirmDialog,
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Account ───────────────────────────────────────────────────────
            item { SectionHeader("Account", TablerIcons.User) }

            item {
                SettingsGroup {
                    if (uiState.isSignedIn) {
                        // Signed-in: show user info + sign out
                        ListItem(
                            headlineContent = {
                                Text(
                                    uiState.userName ?: "User",
                                    fontWeight = FontWeight.Medium,
                                )
                            },
                            supportingContent = uiState.userEmail?.let { email ->
                                { Text(email) }
                            },
                            leadingContent = {
                                // Initials avatar
                                val initials = (uiState.userName ?: "U")
                                    .split(" ")
                                    .take(2)
                                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                    .joinToString("")
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            },
                            trailingContent = {
                                OutlinedButton(onClick = viewModel::signOut) {
                                    Icon(
                                        TablerIcons.Logout,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Sign out")
                                }
                            },
                        )
                    } else {
                        // Not signed in: show sign-in prompt
                        ListItem(
                            headlineContent = { Text("Sign in") },
                            supportingContent = {
                                Text("Back up your data and sync across devices")
                            },
                            leadingContent = {
                                Icon(TablerIcons.Login, contentDescription = null)
                            },
                            trailingContent = {
                                FilledTonalButton(
                                    onClick = {
                                        viewModel.signInWithGoogle(context, webClientId)
                                    },
                                ) {
                                    Text("Sign in with Google")
                                }
                            },
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // ── Appearance ────────────────────────────────────────────────────
            item { SectionHeader("Appearance", TablerIcons.Settings) }

            item {
                SettingsGroup {
                    ThemeModeRow(
                        current = uiState.themeMode,
                        onSelect = viewModel::setThemeMode,
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    AccentColorRow(
                        current = uiState.accentColor,
                        onSelect = viewModel::setAccentColor,
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    FirstDayRow(
                        monday = uiState.firstDayMonday,
                        onToggle = viewModel::setFirstDayMonday,
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // ── Notifications ─────────────────────────────────────────────────
            item { SectionHeader("Notifications", TablerIcons.Bell) }

            item {
                SettingsGroup {
                    SwitchRow(
                        title = "Enable notifications",
                        subtitle = "Master toggle for all Verdant alerts",
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(
                                    android.Manifest.permission.POST_NOTIFICATIONS,
                                )
                            }
                            viewModel.setNotificationsEnabled(enabled)
                        },
                    )
                    if (uiState.notificationsEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SliderRow(
                            title = "Max per day",
                            subtitle = "${uiState.maxNudgesPerDay} notifications",
                            value = uiState.maxNudgesPerDay.toFloat(),
                            range = 1f..10f,
                            steps = 8,
                            onValueChange = { viewModel.setMaxNudgesPerDay(it.toInt()) },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        QuietHoursRow(
                            startHour = uiState.quietHoursStart,
                            endHour = uiState.quietHoursEnd,
                            onStartChange = viewModel::setQuietHoursStart,
                            onEndChange = viewModel::setQuietHoursEnd,
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        NudgeToneRow(
                            current = uiState.nudgeTone,
                            onSelect = viewModel::setNudgeTone,
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        WeeklySummaryRow(
                            enabled = uiState.weeklySummaryEnabled,
                            day = uiState.weeklySummaryDay,
                            hour = uiState.weeklySummaryHour,
                            onEnabledChange = viewModel::setWeeklySummaryEnabled,
                            onDayChange = viewModel::setWeeklySummaryDay,
                            onHourChange = viewModel::setWeeklySummaryHour,
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text("Send test notification") },
                            trailingContent = {
                                FilledTonalButton(onClick = viewModel::sendTestNotification) {
                                    Text("Test")
                                }
                            },
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // ── Data & Privacy ────────────────────────────────────────────────
            item { SectionHeader("Data & Privacy", TablerIcons.ShieldLock) }

            item {
                SettingsGroup {
                    // Export
                    ListItem(
                        headlineContent = { Text("Export data") },
                        supportingContent = { Text("Save all habits and history") },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { exportCsvLauncher.launch("verdant_export.csv") },
                                    enabled = !uiState.exportInProgress,
                                ) { Text("CSV") }
                                OutlinedButton(
                                    onClick = { exportJsonLauncher.launch("verdant_export.json") },
                                    enabled = !uiState.exportInProgress,
                                ) { Text("JSON") }
                            }
                        },
                        leadingContent = {
                            Icon(TablerIcons.Upload, contentDescription = null)
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    // Import
                    ListItem(
                        headlineContent = { Text("Import data") },
                        supportingContent = { Text("Restore from CSV backup") },
                        trailingContent = {
                            FilledTonalButton(
                                onClick = { importCsvLauncher.launch(arrayOf("text/csv", "text/plain")) },
                                enabled = !uiState.importInProgress,
                            ) {
                                Icon(TablerIcons.Download, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Import")
                            }
                        },
                        leadingContent = {
                            Icon(TablerIcons.Download, contentDescription = null)
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    // Google Drive (Coming soon)
                    ListItem(
                        headlineContent = { Text("Backup to Google Drive") },
                        supportingContent = { Text("Coming soon") },
                        trailingContent = {
                            OutlinedButton(onClick = {}, enabled = false) {
                                Text("Coming soon")
                            }
                        },
                        leadingContent = {
                            Icon(TablerIcons.Share, contentDescription = null)
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    // LLM data sharing
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        SwitchRow(
                            title = "Share data with AI",
                            subtitle = null,
                            checked = uiState.llmDataSharing,
                            onCheckedChange = viewModel::setLlmDataSharing,
                        )
                        if (uiState.llmDataSharing) {
                            Text(
                                text = "When enabled, anonymized habit statistics are sent to our AI for personalized insights. No personal notes or location data are shared.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    // Delete all data
                    ListItem(
                        headlineContent = {
                            Text(
                                "Delete all data",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium,
                            )
                        },
                        supportingContent = {
                            Text(
                                "Permanently removes all habits, history, and settings",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        trailingContent = {
                            Button(
                                onClick = viewModel::showDeleteConfirmDialog,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                ),
                            ) {
                                Icon(TablerIcons.Trash, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Delete")
                            }
                        },
                        leadingContent = {
                            Icon(
                                TablerIcons.Trash,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // ── About ─────────────────────────────────────────────────────────
            item { SectionHeader("About", TablerIcons.InfoCircle) }

            item {
                SettingsGroup {
                    val versionName = try {
                        context.packageManager
                            .getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
                    } catch (_: PackageManager.NameNotFoundException) { "1.0.0" }

                    ListItem(
                        headlineContent = { Text("App version") },
                        trailingContent = {
                            Text(
                                versionName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Open-source licenses") },
                        modifier = Modifier.clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://verdant.app/licenses"),
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        },
                        trailingContent = {
                            Text(
                                "View",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Privacy policy") },
                        modifier = Modifier.clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://verdant.app/privacy"),
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        },
                        trailingContent = {
                            Text(
                                "View",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Send feedback") },
                        modifier = Modifier.clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:feedback@verdant.app")
                                        putExtra(Intent.EXTRA_SUBJECT, "Verdant Feedback")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    },
                                )
                            }
                        },
                        trailingContent = {
                            Text(
                                "Feedback",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Section components ────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        content()
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
    )
}

@Composable
private fun SliderRow(
    title: String,
    subtitle: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
        )
    }
}

@Composable
private fun ThemeModeRow(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Theme", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                val selected = current == mode
                FilledTonalButton(
                    onClick = { onSelect(mode) },
                    colors = if (selected) ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) else ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier.weight(1f),
                ) {
                    if (selected) {
                        Icon(
                            TablerIcons.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentColorRow(
    current: Long,
    onSelect: (Long) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Accent color", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            accentColors.forEach { (colorLong, name) ->
                val color = Color(colorLong)
                val selected = current == colorLong
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (selected) Modifier.border(
                                3.dp,
                                MaterialTheme.colorScheme.onSurface,
                                CircleShape,
                            ) else Modifier
                        )
                        .clickable { onSelect(colorLong) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            TablerIcons.Check,
                            contentDescription = "Selected $name",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FirstDayRow(
    monday: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text("First day of week") },
        supportingContent = { Text(if (monday) "Monday" else "Sunday") },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Sun", style = MaterialTheme.typography.bodySmall)
                Switch(
                    checked = monday,
                    onCheckedChange = onToggle,
                )
                Text("Mon", style = MaterialTheme.typography.bodySmall)
            }
        },
    )
}

@Composable
private fun QuietHoursRow(
    startHour: Int,
    endHour: Int,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit,
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text("Quiet hours") },
        supportingContent = {
            Text("${formatHour(startHour)} – ${formatHour(endHour)}")
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showStartPicker = true }) {
                    Text(formatHour(startHour))
                }
                OutlinedButton(onClick = { showEndPicker = true }) {
                    Text(formatHour(endHour))
                }
            }
        },
    )

    if (showStartPicker) {
        HourPickerDialog(
            title = "Quiet hours start",
            currentHour = startHour,
            onConfirm = { onStartChange(it); showStartPicker = false },
            onDismiss = { showStartPicker = false },
        )
    }
    if (showEndPicker) {
        HourPickerDialog(
            title = "Quiet hours end",
            currentHour = endHour,
            onConfirm = { onEndChange(it); showEndPicker = false },
            onDismiss = { showEndPicker = false },
        )
    }
}

@Composable
private fun NudgeToneRow(
    current: NudgeTone,
    onSelect: (NudgeTone) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Nudge tone", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(4.dp))
        NudgeTone.entries.forEach { tone ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(tone) },
            ) {
                RadioButton(
                    selected = current == tone,
                    onClick = { onSelect(tone) },
                )
                Column {
                    Text(tone.label, style = MaterialTheme.typography.bodyMedium)
                    val subtitle = when (tone) {
                        NudgeTone.GENTLE -> "Warm, supportive messages"
                        NudgeTone.MOTIVATING -> "Balanced, factual reminders"
                        NudgeTone.DIRECT -> "Direct, no-nonsense nudges"
                    }
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklySummaryRow(
    enabled: Boolean,
    day: Int,
    hour: Int,
    onEnabledChange: (Boolean) -> Unit,
    onDayChange: (Int) -> Unit,
    onHourChange: (Int) -> Unit,
) {
    var showDayPicker by remember { mutableStateOf(false) }
    var showHourPicker by remember { mutableStateOf(false) }

    Column {
        SwitchRow(
            title = "Weekly summary",
            subtitle = if (enabled) {
                val dayName = daysOfWeek.find { it.first == day }?.second ?: "Sunday"
                "Sent on $dayName at ${formatHour(hour)}"
            } else "Disabled",
            checked = enabled,
            onCheckedChange = onEnabledChange,
        )
        if (enabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { showDayPicker = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(daysOfWeek.find { it.first == day }?.second ?: "Sunday")
                }
                OutlinedButton(
                    onClick = { showHourPicker = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(formatHour(hour))
                }
            }
        }
    }

    if (showDayPicker) {
        DayPickerDialog(
            currentDay = day,
            onConfirm = { onDayChange(it); showDayPicker = false },
            onDismiss = { showDayPicker = false },
        )
    }
    if (showHourPicker) {
        HourPickerDialog(
            title = "Summary time",
            currentHour = hour,
            onConfirm = { onHourChange(it); showHourPicker = false },
            onDismiss = { showHourPicker = false },
        )
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
private fun HourPickerDialog(
    title: String,
    currentHour: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedHour by remember { mutableStateOf(currentHour.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    formatHour(selectedHour.toInt()),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = selectedHour,
                    onValueChange = { selectedHour = it },
                    valueRange = 0f..23f,
                    steps = 22,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selectedHour.toInt()) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DayPickerDialog(
    currentDay: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(currentDay) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Summary day") },
        text = {
            Column {
                daysOfWeek.forEach { (value, name) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = value },
                    ) {
                        RadioButton(selected = selected == value, onClick = { selected = value })
                        Text(name)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DeleteAllDataDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(TablerIcons.Trash, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Delete all data?") },
        text = {
            Text("This will permanently delete all your habits, history, and settings. This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) { Text("Delete everything") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatHour(hour: Int): String {
    val h = hour % 12
    val display = if (h == 0) 12 else h
    val ampm = if (hour < 12) "AM" else "PM"
    return "$display:00 $ampm"
}
