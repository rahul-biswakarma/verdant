package com.verdant.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.common.auth.AuthRepository
import com.verdant.core.common.auth.AuthUser
import com.verdant.core.database.repository.HabitEntryRepository
import com.verdant.core.database.repository.HabitRepository
import com.verdant.core.database.usecase.DatabaseCleaner
import com.verdant.core.database.usecase.ExportUseCase
import com.verdant.core.datastore.NudgeTone
import com.verdant.core.datastore.UserPreferencesDataStore
import com.verdant.core.designsystem.theme.ThemeMode
import com.verdant.core.model.Habit
import com.verdant.core.model.HabitEntry
import com.verdant.core.model.HabitFrequency
import com.verdant.core.model.TrackingType
import com.verdant.core.model.defaultVisualization
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.time.LocalDate
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: Long = 0xFF5A7A60L,
    val firstDayMonday: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val maxNudgesPerDay: Int = 5,
    val quietHoursStart: Int = 22,
    val quietHoursEnd: Int = 7,
    val nudgeTone: NudgeTone = NudgeTone.MOTIVATING,
    val weeklySummaryEnabled: Boolean = true,
    val weeklySummaryDay: Int = 7,
    val weeklySummaryHour: Int = 19,
    val llmDataSharing: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val exportInProgress: Boolean = false,
    val importInProgress: Boolean = false,
    val snackbarMessage: String? = null,
    // Auth
    val isSignedIn: Boolean = false,
    val userName: String? = null,
    val userEmail: String? = null,
    val userPhotoUrl: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
    private val authRepository: AuthRepository,
    private val exportUseCase: ExportUseCase,
    private val databaseCleaner: DatabaseCleaner,
    private val habitRepository: HabitRepository,
    private val entryRepository: HabitEntryRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _extra = MutableStateFlow(
        SettingsUiState()
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        prefs.themeMode,
        prefs.accentColor,
        prefs.firstDayOfWeek,
        prefs.notificationsEnabled,
        prefs.maxNudgesPerDay,
        prefs.quietHoursStart,
        prefs.quietHoursEnd,
        prefs.nudgeTone,
        prefs.weeklySummaryEnabled,
        prefs.weeklySummaryDay,
        prefs.weeklySummaryHour,
        prefs.llmDataSharing,
        combine(_extra, authRepository.currentUser) { extra, user -> extra to user },
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val themeMode        = ThemeMode.valueOf(args[0] as String)
        val accentColor      = args[1] as Long
        val firstDayMonday   = (args[2] as String) == "MONDAY"
        val notifEnabled     = args[3] as Boolean
        val maxNudges        = args[4] as Int
        val qStart           = args[5] as Int
        val qEnd             = args[6] as Int
        val nudgeTone        = NudgeTone.fromKey(args[7] as String)
        val weeklySumEnabled = args[8] as Boolean
        val weeklySumDay     = args[9] as Int
        val weeklySumHour    = args[10] as Int
        val llmSharing       = args[11] as Boolean
        val (extra, user)    = args[12] as Pair<SettingsUiState, AuthUser?>

        extra.copy(
            themeMode = themeMode,
            accentColor = accentColor,
            firstDayMonday = firstDayMonday,
            notificationsEnabled = notifEnabled,
            maxNudgesPerDay = maxNudges,
            quietHoursStart = qStart,
            quietHoursEnd = qEnd,
            nudgeTone = nudgeTone,
            weeklySummaryEnabled = weeklySumEnabled,
            weeklySummaryDay = weeklySumDay,
            weeklySummaryHour = weeklySumHour,
            llmDataSharing = llmSharing,
            isSignedIn = user != null,
            userName = user?.displayName,
            userEmail = user?.email,
            userPhotoUrl = user?.photoUrl,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    // ── Appearance ────────────────────────────────────────────────────────────

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        prefs.setThemeMode(mode.name)
    }

    fun setAccentColor(color: Long) = viewModelScope.launch {
        prefs.setAccentColor(color)
    }

    fun setFirstDayMonday(monday: Boolean) = viewModelScope.launch {
        prefs.setFirstDayOfWeek(if (monday) "MONDAY" else "SUNDAY")
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        prefs.setNotificationsEnabled(enabled)
    }

    fun setMaxNudgesPerDay(value: Int) = viewModelScope.launch {
        prefs.setMaxNudgesPerDay(value)
    }

    fun setQuietHoursStart(hour: Int) = viewModelScope.launch {
        val current = uiState.value
        prefs.setQuietHours(hour, current.quietHoursEnd)
    }

    fun setQuietHoursEnd(hour: Int) = viewModelScope.launch {
        val current = uiState.value
        prefs.setQuietHours(current.quietHoursStart, hour)
    }

    fun setNudgeTone(tone: NudgeTone) = viewModelScope.launch {
        prefs.setNudgeTone(tone)
    }

    fun setWeeklySummaryEnabled(enabled: Boolean) = viewModelScope.launch {
        prefs.setWeeklySummaryEnabled(enabled)
    }

    fun setWeeklySummaryDay(dayValue: Int) = viewModelScope.launch {
        prefs.setWeeklySummaryDay(dayValue)
    }

    fun setWeeklySummaryHour(hour: Int) = viewModelScope.launch {
        prefs.setWeeklySummaryHour(hour)
    }

    fun sendTestNotification() {
        // Post a test notification via the existing notification channel
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager
        val notification = android.app.Notification.Builder(context, "verdant_nudges")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Verdant Test")
            .setContentText("Notifications are working correctly!")
            .setAutoCancel(true)
            .build()
        nm.notify(9999, notification)
    }

    // ── Auth ───────────────────────────────────────────────────────────────────

    fun signInWithGoogle(activityContext: Context, webClientId: String) = viewModelScope.launch {
        val result = authRepository.signInWithGoogle(activityContext, webClientId)
        if (result.isFailure) {
            _extra.update {
                it.copy(snackbarMessage = "Sign-in failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun signOut() = viewModelScope.launch {
        authRepository.signOut()
    }

    // ── Data & Privacy ────────────────────────────────────────────────────────

    fun setLlmDataSharing(enabled: Boolean) = viewModelScope.launch {
        prefs.setLlmDataSharing(enabled)
    }

    fun exportJson(outputStream: OutputStream) = viewModelScope.launch {
        _extra.update { it.copy(exportInProgress = true) }
        try {
            val data = exportUseCase.collectData()
            outputStream.bufferedWriter().use { writer ->
                writer.write(exportUseCase.toJson(data))
            }
            _extra.update { it.copy(snackbarMessage = "Exported ${data.habits.size} habits successfully") }
        } catch (e: Exception) {
            _extra.update { it.copy(snackbarMessage = "Export failed: ${e.message}") }
        } finally {
            _extra.update { it.copy(exportInProgress = false) }
        }
    }

    fun exportCsv(outputStream: OutputStream) = viewModelScope.launch {
        _extra.update { it.copy(exportInProgress = true) }
        try {
            val data = exportUseCase.collectData()
            outputStream.bufferedWriter().use { writer ->
                writer.write(exportUseCase.toCsv(data))
            }
            _extra.update { it.copy(snackbarMessage = "Exported ${data.habits.size} habits successfully") }
        } catch (e: Exception) {
            _extra.update { it.copy(snackbarMessage = "Export failed: ${e.message}") }
        } finally {
            _extra.update { it.copy(exportInProgress = false) }
        }
    }

    fun importCsv(uri: Uri) = viewModelScope.launch {
        _extra.update { it.copy(importInProgress = true) }
        try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                val lines = reader.readLines()
                var inEntries = false
                var inHabits = false
                var habitsImported = 0
                var entriesImported = 0
                lines.forEach { line ->
                    when {
                        line.startsWith("# Habits") -> { inHabits = true; inEntries = false }
                        line.startsWith("# Entries") -> { inEntries = true; inHabits = false }
                        line.startsWith("id,") -> { /* header row — skip */ }
                        line.startsWith("#") || line.isBlank() -> { /* comment or empty */ }
                        inHabits -> {
                            parseHabitCsvRow(line)?.let { habit ->
                                habitRepository.insert(habit)
                                habitsImported++
                            }
                        }
                        inEntries -> {
                            parseEntryCsvRow(line)?.let { entry ->
                                entryRepository.upsert(entry)
                                entriesImported++
                            }
                        }
                    }
                }
                _extra.update {
                    it.copy(snackbarMessage = "Imported $habitsImported habits and $entriesImported entries")
                }
            }
        } catch (e: Exception) {
            _extra.update { it.copy(snackbarMessage = "Import failed: ${e.message}") }
        } finally {
            _extra.update { it.copy(importInProgress = false) }
        }
    }

    /**
     * Parses a CSV row matching the export format:
     * id,name,description,icon,color,label,trackingType,unit,targetValue,frequency,scheduleDays,isArchived,reminderEnabled,reminderTime,sortOrder,createdAt
     */
    private fun parseHabitCsvRow(line: String): Habit? = runCatching {
        val cols = parseCsvLine(line)
        if (cols.size < 16) return null
        val type = TrackingType.valueOf(cols[6])
        Habit(
            id = cols[0],
            name = cols[1],
            description = cols[2],
            icon = cols[3],
            color = cols[4].toLong(),
            label = cols[5].ifBlank { null },
            trackingType = type,
            visualizationType = runCatching {
                com.verdant.core.model.VisualizationType.valueOf(cols.getOrElse(16) { "" })
            }.getOrElse { type.defaultVisualization() },
            unit = cols[7].ifBlank { null },
            targetValue = cols[8].toDoubleOrNull(),
            checkpointSteps = cols.getOrElse(17) { "" }
                .split("|").filter { it.isNotBlank() },
            frequency = HabitFrequency.valueOf(cols[9]),
            scheduleDays = cols[10].toInt(),
            isArchived = cols[11].toBooleanStrict(),
            reminderEnabled = cols[12].toBooleanStrict(),
            reminderTime = cols[13].ifBlank { null },
            reminderDays = 0,
            sortOrder = cols[14].toInt(),
            createdAt = cols[15].toLong(),
        )
    }.getOrNull()

    /**
     * Parses a CSV row matching the export format:
     * id,habitId,date,completed,value,note,category,skipped,createdAt,updatedAt
     */
    private fun parseEntryCsvRow(line: String): HabitEntry? = runCatching {
        val cols = parseCsvLine(line)
        if (cols.size < 10) return null
        HabitEntry(
            id = cols[0],
            habitId = cols[1],
            date = LocalDate.parse(cols[2]),
            completed = cols[3].toBooleanStrict(),
            value = cols[4].toDoubleOrNull(),
            latitude = null,
            longitude = null,
            note = cols[5].ifBlank { null },
            category = cols[6].ifBlank { null },
            skipped = cols[7].toBooleanStrict(),
            createdAt = cols[8].toLong(),
            updatedAt = cols[9].toLong(),
        )
    }.getOrNull()

    /** Simple CSV line parser that handles quoted fields with escaped double-quotes. */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        while (i <= line.length) {
            if (i == line.length) { result.add(""); break }
            if (line[i] == '"') {
                // Quoted field
                val sb = StringBuilder()
                i++ // skip opening quote
                while (i < line.length) {
                    if (line[i] == '"') {
                        if (i + 1 < line.length && line[i + 1] == '"') {
                            sb.append('"'); i += 2
                        } else {
                            i++ // skip closing quote
                            break
                        }
                    } else {
                        sb.append(line[i]); i++
                    }
                }
                result.add(sb.toString())
                if (i < line.length && line[i] == ',') i++ // skip comma
            } else {
                // Unquoted field
                val next = line.indexOf(',', i)
                if (next == -1) {
                    result.add(line.substring(i)); break
                } else {
                    result.add(line.substring(i, next)); i = next + 1
                }
            }
        }
        return result
    }

    fun showDeleteConfirmDialog() = _extra.update { it.copy(showDeleteConfirmDialog = true) }
    fun dismissDeleteConfirmDialog() = _extra.update { it.copy(showDeleteConfirmDialog = false) }

    fun deleteAllData(onComplete: () -> Unit) = viewModelScope.launch {
        try {
            databaseCleaner.clearAll()
            prefs.resetAll()
            onComplete()
        } catch (e: Exception) {
            _extra.update { it.copy(snackbarMessage = "Delete failed: ${e.message}") }
        }
    }

    fun clearSnackbar() = _extra.update { it.copy(snackbarMessage = null) }
}
