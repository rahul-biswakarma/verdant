package com.verdant.widget

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.ai.MotivationContext
import com.verdant.core.ai.VerdantAI
import com.verdant.core.database.repository.HabitEntryRepository
import com.verdant.core.database.repository.HabitRepository
import com.verdant.core.database.usecase.CalculateStreakUseCase
import com.verdant.core.database.usecase.LogEntryUseCase
import com.verdant.core.model.Habit
import com.verdant.core.model.HabitEntry
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Updates all widget types in a single pass.
 *
 * Pre-loads shared data (active habits, today's entries) once, then dispatches
 * per-widget-class update functions.
 *
 * Input data keys:
 *  - [KEY_QUICK_LOG_HABIT_ID] + [KEY_QUICK_LOG_DATE] → log binary entry before refresh
 *  - [KEY_TOGGLE_HABIT_ID] + [KEY_TOGGLE_DATE] → toggle checklist item before refresh
 */
@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val habitRepository: HabitRepository,
    private val entryRepository: HabitEntryRepository,
    private val logEntryUseCase: LogEntryUseCase,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val verdantAI: VerdantAI,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        handlePreActions()

        val today        = LocalDate.now()
        val allHabits    = habitRepository.observeActiveHabits().first()
        val todayEntries = entryRepository.observeAllEntries(today, today).first()
        val entryByHabit = todayEntries.associateBy { it.habitId }

        val manager = GlanceAppWidgetManager(context)

        updateHabitGridWidgets(manager, today)
        updateChecklistWidgets(manager, allHabits, entryByHabit, today)
        updateSummaryWidgets(manager, allHabits, entryByHabit, today)
        updateBarChartWidgets(manager, allHabits, today)
        updateRadialRingWidgets(manager, allHabits, entryByHabit, today)
        updateMiniHeatmapWidgets(manager, today)
        updateStreakWidgets(manager, allHabits)
        updateQuoteWidgets(manager, today, allHabits, entryByHabit)

        return Result.success()
    }

    // ── Pre-action: log / toggle before data refresh ─────────────────────────

    private suspend fun handlePreActions() {
        val quickHabitId = inputData.getString(KEY_QUICK_LOG_HABIT_ID)
        if (quickHabitId != null) {
            val date = inputData.getString(KEY_QUICK_LOG_DATE)
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
            logEntryUseCase.logBinary(quickHabitId, date, completed = true)
        }

        val toggleHabitId = inputData.getString(KEY_TOGGLE_HABIT_ID)
        if (toggleHabitId != null) {
            val date = inputData.getString(KEY_TOGGLE_DATE)
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
            val existing = entryRepository.getByHabitAndDate(toggleHabitId, date)
            logEntryUseCase.logBinary(toggleHabitId, date, completed = !(existing?.completed ?: false))
        }
    }

    // ── HabitGridWidget ───────────────────────────────────────────────────────

    private suspend fun updateHabitGridWidgets(manager: GlanceAppWidgetManager, today: LocalDate) {
        val ids = manager.getGlanceIds(HabitGridWidget::class.java)
        for (glanceId in ids) {
            runCatching {
                val prefs   = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
                val habitId = prefs[WidgetPreferencesKeys.HABIT_ID] ?: return@runCatching
                val habit   = habitRepository.getById(habitId) ?: return@runCatching

                val startDate = today.minusWeeks(20)
                val entries   = entryRepository.observeEntries(habitId, startDate, today).first()
                val entryMap  = entries.associateBy { it.date }

                val gridCells = buildList {
                    var d = startDate
                    while (!d.isAfter(today)) {
                        add(d.toString() to entryIntensity(entryMap[d], habit.targetValue))
                        d = d.plusDays(1)
                    }
                }

                val todayDow  = today.dayOfWeek.value
                val weekStart = today.minusDays((todayDow - 1).toLong())
                val weekTotal = todayDow
                val weekDone  = entries.count { it.date >= weekStart && it.completed }
                val streak    = calculateStreakUseCase.currentStreak(habitId)

                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { p ->
                    p.toMutablePreferences().apply {
                        this[WidgetPreferencesKeys.HABIT_NAME]    = habit.name
                        this[WidgetPreferencesKeys.HABIT_ICON]    = habit.icon.ifEmpty { "🌱" }
                        this[WidgetPreferencesKeys.HABIT_COLOR]   = habit.color
                        this[WidgetPreferencesKeys.TRACKING_TYPE] = habit.trackingType.name
                        this[WidgetPreferencesKeys.STREAK]        = streak
                        this[WidgetPreferencesKeys.GRID_JSON]     = buildGridJson(gridCells)
                        this[WidgetPreferencesKeys.WEEK_DONE]     = weekDone
                        this[WidgetPreferencesKeys.WEEK_TOTAL]    = weekTotal
                    }
                }
                HabitGridWidget().update(context, glanceId)
            }
        }
    }

    // ── ChecklistWidget ───────────────────────────────────────────────────────

    private suspend fun updateChecklistWidgets(
        manager: GlanceAppWidgetManager,
        allHabits: List<Habit>,
        entryByHabit: Map<String, HabitEntry>,
        today: LocalDate,
    ) {
        val ids = manager.getGlanceIds(ChecklistWidget::class.java)
        if (ids.isEmpty()) return

        val todayDowBit = 1 shl (today.dayOfWeek.value - 1)
        val todayHabits = allHabits.filter { it.scheduleDays and todayDowBit != 0 }

        val jsonArray = JSONArray()
        for (habit in todayHabits) {
            val entry     = entryByHabit[habit.id]
            val completed = entry?.completed ?: false
            val skipped   = entry?.skipped ?: false
            jsonArray.put(JSONObject().apply {
                put("id",        habit.id)
                put("icon",      habit.icon.ifEmpty { "🌱" })
                put("name",      habit.name)
                put("colorL",    habit.color)
                put("completed", completed)
                put("status",    when { skipped -> "⏭ Skipped"; completed -> "✓ Done"; else -> "" })
                put("binary",    habit.trackingType.name == "BINARY")
            })
        }

        val done  = todayHabits.count { entryByHabit[it.id]?.completed == true }
        val total = todayHabits.size

        for (glanceId in ids) {
            runCatching {
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { p ->
                    p.toMutablePreferences().apply {
                        this[WidgetPreferencesKeys.CHECKLIST_JSON] = jsonArray.toString()
                        this[WidgetPreferencesKeys.TODAY_DONE]     = done
                        this[WidgetPreferencesKeys.TODAY_TOTAL]    = total
                    }
                }
                ChecklistWidget().update(context, glanceId)
            }
        }
    }

    // ── SummaryWidget ─────────────────────────────────────────────────────────

    private suspend fun updateSummaryWidgets(
        manager: GlanceAppWidgetManager,
        allHabits: List<Habit>,
        entryByHabit: Map<String, HabitEntry>,
        today: LocalDate,
    ) {
        val ids = manager.getGlanceIds(SummaryWidget::class.java)
        if (ids.isEmpty()) return

        val todayDowBit = 1 shl (today.dayOfWeek.value - 1)
        val todayHabits = allHabits.filter { it.scheduleDays and todayDowBit != 0 }
        val done        = todayHabits.count { entryByHabit[it.id]?.completed == true }
        val total       = todayHabits.size
        val ringColor   = allHabits.firstOrNull()?.color?.toInt()
            ?: AndroidColor.parseColor("#4CAF50")

        val bmp  = WidgetBitmapUtils.generateSummaryRing(done, total, ringColor, 400)
        val path = WidgetBitmapUtils.saveBitmap(context, bmp, "summary_ring.png")
        bmp.recycle()

        for (glanceId in ids) {
            runCatching {
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { p ->
                    p.toMutablePreferences().apply {
                        this[WidgetPreferencesKeys.BITMAP_PATH]  = path
                        this[WidgetPreferencesKeys.TODAY_DONE]   = done
                        this[WidgetPreferencesKeys.TODAY_TOTAL]  = total
                    }
                }
                SummaryWidget().update(context, glanceId)
            }
        }
    }

    // ── BarChartWidget ────────────────────────────────────────────────────────

    private suspend fun updateBarChartWidgets(
        manager: GlanceAppWidgetManager,
        allHabits: List<Habit>,
        today: LocalDate,
    ) {
        val ids = manager.getGlanceIds(BarChartWidget::class.java)
        if (ids.isEmpty()) return

        val weekStart  = today.minusDays(6)
        val weekEntries = entryRepository.observeAllEntries(weekStart, today).first()
        val byDate      = weekEntries.groupBy { it.date }

        val jsonArray = JSONArray()
        for (offset in 6 downTo 0) {
            val date    = today.minusDays(offset.toLong())
            val dayBit  = 1 shl (date.dayOfWeek.value - 1)
            val sched   = allHabits.filter { it.scheduleDays and dayBit != 0 }
            val total   = sched.size
            val done    = (byDate[date] ?: emptyList()).count { it.completed }
            val fraction = if (total > 0) done.toFloat() / total else 0f
            val label   = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3)

            jsonArray.put(JSONObject().apply {
                put("label",    label)
                put("done",     done)
                put("total",    total)
                put("fraction", fraction.toDouble())
                put("today",    date == today)
            })
        }

        for (glanceId in ids) {
            runCatching {
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { p ->
                    p.toMutablePreferences().apply {
                        this[WidgetPreferencesKeys.BAR_CHART_JSON] = jsonArray.toString()
                    }
                }
                BarChartWidget().update(context, glanceId)
            }
        }
    }

    // ── RadialRingWidget ──────────────────────────────────────────────────────

    private suspend fun updateRadialRingWidgets(
        manager: GlanceAppWidgetManager,
        allHabits: List<Habit>,
        entryByHabit: Map<String, HabitEntry>,
        today: LocalDate,
    ) {
        val ids = manager.getGlanceIds(RadialRingWidget::class.java)
        if (ids.isEmpty()) return

        val todayDowBit = 1 shl (today.dayOfWeek.value - 1)
        val todayHabits = allHabits.filter { it.scheduleDays and todayDowBit != 0 }.take(5)
        val done        = todayHabits.count { entryByHabit[it.id]?.completed == true }
        val total       = todayHabits.size

        val ringTriples = todayHabits.map { habit ->
            val entry    = entryByHabit[habit.id]
            val progress = entryIntensity(entry, habit.targetValue)
            Triple(habit.name, habit.color.toInt(), progress)
        }

        val bmp  = WidgetBitmapUtils.generateRadialRings(ringTriples, done, total, 400)
        val path = WidgetBitmapUtils.saveBitmap(context, bmp, "radial_rings.png")
        bmp.recycle()

        for (glanceId in ids) {
            runCatching {
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { p ->
                    p.toMutablePreferences().apply {
                        this[WidgetPreferencesKeys.BITMAP_PATH]  = path
                        this[WidgetPreferencesKeys.TODAY_DONE]   = done
                        this[WidgetPreferencesKeys.TODAY_TOTAL]  = total
                    }
                }
                RadialRingWidget().update(context, glanceId)
            }
        }
    }

    // ── MiniHeatmapWidget ─────────────────────────────────────────────────────

    private suspend fun updateMiniHeatmapWidgets(
        manager: GlanceAppWidgetManager,
        today: LocalDate,
    ) {
        val ids = manager.getGlanceIds(MiniHeatmapWidget::class.java)
        for (glanceId in ids) {
            runCatching {
                val prefs   = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
                val habitId = prefs[WidgetPreferencesKeys.HABIT_ID] ?: return@runCatching
                val habit   = habitRepository.getById(habitId) ?: return@runCatching

                val startDate = today.minusWeeks(8)
                val entries   = entryRepository.observeEntries(habitId, startDate, today).first()
                val entryMap  = entries.associateBy { it.date }

                val gridCells = buildList {
                    var d = startDate
                    while (!d.isAfter(today)) {
                        add(d.toString() to entryIntensity(entryMap[d], habit.targetValue))
                        d = d.plusDays(1)
                    }
                }

                val todayDow  = today.dayOfWeek.value
                val weekStart = today.minusDays((todayDow - 1).toLong())
                val weekDone  = entries.count { it.date >= weekStart && it.completed }
                val weekTotal = todayDow

                val streak    = calculateStreakUseCase.currentStreak(habitId)
                val best      = calculateStreakUseCase.longestStreak(habitId)
                val rate30    = calculateStreakUseCase.completionRate(habitId, 30)
                val rate60    = calculateStreakUseCase.completionRate(habitId, 60)
                val trend     = (rate30 - rate60) * 100f

                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { p ->
                    p.toMutablePreferences().apply {
                        this[WidgetPreferencesKeys.HABIT_NAME]      = habit.name
                        this[WidgetPreferencesKeys.HABIT_ICON]      = habit.icon.ifEmpty { "🌱" }
                        this[WidgetPreferencesKeys.HABIT_COLOR]     = habit.color
                        this[WidgetPreferencesKeys.TRACKING_TYPE]   = habit.trackingType.name
                        this[WidgetPreferencesKeys.STREAK]          = streak
                        this[WidgetPreferencesKeys.BEST_STREAK]     = best
                        this[WidgetPreferencesKeys.COMPLETION_RATE] = rate30
                        this[WidgetPreferencesKeys.TREND_PCT]       = trend
                        this[WidgetPreferencesKeys.GRID_JSON]       = buildGridJson(gridCells)
                        this[WidgetPreferencesKeys.WEEK_DONE]       = weekDone
                        this[WidgetPreferencesKeys.WEEK_TOTAL]      = weekTotal
                    }
                }
                MiniHeatmapWidget().update(context, glanceId)
            }
        }
    }

    // ── StreakWidget ──────────────────────────────────────────────────────────

    private suspend fun updateStreakWidgets(
        manager: GlanceAppWidgetManager,
        allHabits: List<Habit>,
    ) {
        val ids = manager.getGlanceIds(StreakWidget::class.java)
        if (ids.isEmpty()) return

        val streakMap = calculateStreakUseCase.currentStreaks(allHabits.map { it.id })
        val top3      = allHabits.sortedByDescending { streakMap[it.id] ?: 0 }.take(3)

        var bestName   = ""
        var bestStreak = 0
        for (habit in allHabits) {
            val s = calculateStreakUseCase.longestStreak(habit.id)
            if (s > bestStreak) { bestStreak = s; bestName = habit.name }
        }

        val jsonArray = JSONArray()
        for (habit in top3) {
            val s = streakMap[habit.id] ?: 0
            if (s == 0) continue
            jsonArray.put(JSONObject().apply {
                put("icon",   habit.icon.ifEmpty { "🌱" })
                put("name",   habit.name)
                put("colorL", habit.color)
                put("streak", s)
            })
        }

        for (glanceId in ids) {
            runCatching {
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { p ->
                    p.toMutablePreferences().apply {
                        this[WidgetPreferencesKeys.TOP_STREAKS_JSON]  = jsonArray.toString()
                        this[WidgetPreferencesKeys.BEST_EVER_NAME]    = bestName
                        this[WidgetPreferencesKeys.BEST_EVER_STREAK]  = bestStreak
                    }
                }
                StreakWidget().update(context, glanceId)
            }
        }
    }

    // ── QuoteWidget ───────────────────────────────────────────────────────────

    /**
     * Generates the daily quote card for all [QuoteWidget] instances.
     *
     * If Gemini Nano is available the quote is personalised via
     * [VerdantAI.generateMotivation]; otherwise we fall back to the curated
     * [QUOTES] list (which itself is what [com.verdant.core.ai.FallbackAI] would
     * produce for a motivation request).
     *
     * The bitmap is cached on disk and only regenerated once per calendar day.
     */
    private suspend fun updateQuoteWidgets(
        manager: GlanceAppWidgetManager,
        today: LocalDate,
        allHabits: List<Habit>,
        entryByHabit: Map<String, HabitEntry>,
    ) {
        val ids = manager.getGlanceIds(QuoteWidget::class.java)
        if (ids.isEmpty()) return

        val todayStr = today.toString()

        // All QuoteWidget instances share a single quote_card.png bitmap.
        // Check the first instance to see if today's card is already on disk.
        val firstPrefs  = runCatching {
            getAppWidgetState(context, PreferencesGlanceStateDefinition, ids.first())
        }.getOrNull()
        val cachedDate  = firstPrefs?.get(WidgetPreferencesKeys.QUOTE_DATE)
        val cachedPath  = firstPrefs?.get(WidgetPreferencesKeys.BITMAP_PATH)
        val alreadyCached = cachedDate == todayStr &&
                cachedPath != null && java.io.File(cachedPath).exists()

        if (alreadyCached) {
            // Just ping each widget to re-render from existing state.
            for (glanceId in ids) {
                runCatching { QuoteWidget().update(context, glanceId) }
            }
            return
        }

        // Build MotivationContext and resolve quote text — once for all instances.
        val motivationContext = buildMotivationContext(allHabits, entryByHabit, today)
        val quote = resolveQuote(today, motivationContext)

        val paletteIdx = (today.dayOfYear / QUOTES.size) % 5
        val bmp  = WidgetBitmapUtils.generateQuoteCard(quote.first, quote.second, paletteIdx, 400, 400)
        val path = WidgetBitmapUtils.saveBitmap(context, bmp, "quote_card.png")
        bmp.recycle()

        for (glanceId in ids) {
            runCatching {
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { p ->
                    p.toMutablePreferences().apply {
                        this[WidgetPreferencesKeys.QUOTE_TEXT]   = quote.first
                        this[WidgetPreferencesKeys.QUOTE_AUTHOR] = quote.second
                        this[WidgetPreferencesKeys.QUOTE_DATE]   = todayStr
                        this[WidgetPreferencesKeys.BITMAP_PATH]  = path
                    }
                }
                QuoteWidget().update(context, glanceId)
            }
        }
    }

    /**
     * Asks [VerdantAI] for a personalised motivation message.
     * Falls back gracefully to the curated [QUOTES] list if AI is unavailable
     * or returns blank text.
     */
    private suspend fun resolveQuote(
        today: LocalDate,
        motivationContext: MotivationContext,
    ): Pair<String, String> {
        val aiText = runCatching {
            verdantAI.generateMotivation(motivationContext).trim()
        }.getOrNull()

        return if (!aiText.isNullOrBlank()) {
            aiText to "Verdant AI"
        } else {
            QUOTES[today.dayOfYear % QUOTES.size]
        }
    }

    /**
     * Builds the [MotivationContext] from available data:
     *  - today's scheduled habits
     *  - active streaks per habit
     *  - yesterday's completion fraction
     *  - this week's completion fraction
     */
    private suspend fun buildMotivationContext(
        allHabits: List<Habit>,
        entryByHabit: Map<String, HabitEntry>,
        today: LocalDate,
    ): MotivationContext {
        val todayDowBit = 1 shl (today.dayOfWeek.value - 1)
        val todayHabits = allHabits.filter { it.scheduleDays and todayDowBit != 0 }

        // Active streaks (non-zero only)
        val activeStreaks = runCatching {
            calculateStreakUseCase.currentStreaks(allHabits.map { it.id })
                .filter { (_, streak) -> streak > 0 }
        }.getOrDefault(emptyMap())

        // Yesterday completion
        val yesterday = today.minusDays(1)
        val yesterdayEntries = runCatching {
            entryRepository.observeAllEntries(yesterday, yesterday).first()
        }.getOrDefault(emptyList())
        val yDowBit = 1 shl (yesterday.dayOfWeek.value - 1)
        val yHabits = allHabits.filter { it.scheduleDays and yDowBit != 0 }
        val yesterdayCompletion = if (yHabits.isEmpty()) 0f
        else yesterdayEntries.count { it.completed }.toFloat() / yHabits.size

        // Week completion (Mon–today)
        val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val weekEntries = runCatching {
            entryRepository.observeAllEntries(weekStart, today).first()
        }.getOrDefault(emptyList())
        val weekTotal = (0 until today.dayOfWeek.value).sumOf { offset ->
            val d = weekStart.plusDays(offset.toLong())
            val bit = 1 shl (d.dayOfWeek.value - 1)
            allHabits.count { it.scheduleDays and bit != 0 }
        }
        val weekCompletion = if (weekTotal == 0) 0f
        else weekEntries.count { it.completed }.toFloat() / weekTotal

        return MotivationContext(
            todayHabits = todayHabits,
            activeStreaks = activeStreaks,
            yesterdayCompletion = yesterdayCompletion,
            weekCompletion = weekCompletion,
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun entryIntensity(entry: HabitEntry?, targetValue: Double?): Float = when {
        entry == null       -> 0f
        entry.skipped       -> 0f
        entry.completed     -> 1f
        entry.value != null -> {
            val v = entry.value!!
            if (targetValue != null && targetValue > 0)
                (v / targetValue).toFloat().coerceIn(0f, 1f)
            else 0.5f
        }
        else -> 0f
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        const val PERIODIC_WORK_NAME     = "verdant_widget_periodic_update"
        const val KEY_QUICK_LOG_HABIT_ID = "quick_log_habit_id"
        const val KEY_QUICK_LOG_DATE     = "quick_log_date"
        const val KEY_TOGGLE_HABIT_ID    = "toggle_habit_id"
        const val KEY_TOGGLE_DATE        = "toggle_date"

        val QUOTES: List<Pair<String, String>> = listOf(
            "We are what we repeatedly do. Excellence is not an act, but a habit." to "Aristotle",
            "The secret of getting ahead is getting started." to "Mark Twain",
            "It does not matter how slowly you go as long as you do not stop." to "Confucius",
            "Success is the sum of small efforts repeated day in and day out." to "Robert Collier",
            "Motivation is what gets you started. Habit is what keeps you going." to "Jim Ryun",
            "You don't rise to the level of your goals, you fall to the level of your systems." to "James Clear",
            "Every action you take is a vote for the type of person you wish to become." to "James Clear",
            "Small steps in the right direction can turn out to be the biggest step of your life." to "Unknown",
            "Discipline is choosing between what you want now and what you want most." to "Abraham Lincoln",
            "Consistency is the mother of mastery." to "Robin Sharma",
            "A year from now you may wish you had started today." to "Karen Lamb",
            "Don't watch the clock; do what it does — keep going." to "Sam Levenson",
            "The journey of a thousand miles begins with one step." to "Lao Tzu",
            "You are never too old to set another goal or to dream a new dream." to "C.S. Lewis",
            "What gets measured gets managed." to "Peter Drucker",
            "First forget inspiration. Habit is more dependable." to "Octavia Butler",
            "The only way to do great work is to love what you do." to "Steve Jobs",
            "In the middle of every difficulty lies opportunity." to "Albert Einstein",
            "Believe you can and you're halfway there." to "Theodore Roosevelt",
            "The future belongs to those who believe in the beauty of their dreams." to "Eleanor Roosevelt",
            "Strive not to be a success, but rather to be of value." to "Albert Einstein",
            "The mind is everything. What you think you become." to "Buddha",
            "An unexamined life is not worth living." to "Socrates",
            "Spread love everywhere you go." to "Mother Teresa",
            "You miss 100% of the shots you don't take." to "Wayne Gretzky",
            "Whether you think you can or think you can't, you're right." to "Henry Ford",
            "Too many of us are not living our dreams because we are living our fears." to "Les Brown",
            "I've learned that people will forget what you said, but not how you made them feel." to "Maya Angelou",
            "No act of kindness, no matter how small, is ever wasted." to "Aesop",
            "Do not go where the path may lead; go instead where there is no path." to "Emerson",
        )
    }
}
