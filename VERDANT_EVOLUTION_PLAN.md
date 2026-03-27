# Verdant Evolution Plan
## "Less Human Effort, More Insight, Better Tracking"

---

## FOUNDATION ASSESSMENT

The codebase is multi-module Clean Architecture: `app/`, `core/*`, `feature/*`, `widget/`, `work/`. Three-tier AI routing (MediaPipe on-device → CloudAI via Firebase/Claude → FallbackAI templates). WorkManager workers for motivation, streak alerts, weekly summary, SMS processing. Room DB at v4 with HabitEntity, HabitEntryEntity, TransactionEntity, AIInsightEntity. DataStore for preferences. Glance widgets.

Current tracking types: BINARY, NUMERIC, LOCATION. Workers are reactive — they describe what happened. The next evolution makes the app predictive and proactive.

The single most important TODO in the codebase:
```
// feature/habits/src/main/kotlin/com/verdant/feature/habits/create/ConversationalEntrySection.kt
/* TODO: voice input via SpeechRecognizer */
```
That single comment unlocks a massive UX shift.

All Phase 1–5 schema additions land in one **v5 migration** — see end of document.

---

## PHASE 1 — Predictive Intelligence Engine
*"The app that warns you before you fail"*

### 1.1 Habit Risk Scoring

Replaces the binary "streak at risk" check with a continuous 0.0–1.0 risk score per habit, updated every 2 hours.

**Current state:** `StreakAlertWorker` fires in a fixed 4 PM–10 PM window if streak ≥ 3 days and today is not complete. No gradient, no personalization.

**Architecture decision:** Don't replace — extend. `StreakAlertWorker` becomes a consumer of a new `RiskScoringEngine` use case.

**New file:**
```
core/database/src/main/kotlin/com/verdant/core/database/usecase/RiskScoringEngine.kt
```

Risk score factors (0.0–1.0 composite):
- Time pressure: how far into the day relative to the habit's typical completion window
- Historical day-of-week failure rate: computed from existing HabitEntryEntity records
- Streak value: higher streak = higher urgency multiplier
- Recent miss density: 2+ misses in last 7 days elevates baseline
- Energy/stress proxy: if stressLevel was high on yesterday's entry, today's baseline risk is elevated (data already in HabitEntryEntity)

**New Room entity:**
```
core/database/src/main/kotlin/com/verdant/core/database/entity/HabitRiskSnapshotEntity.kt
fields: id, habitId (FK), score: Float, computedAt: Long, triggeringFactors: String (JSON blob)
```

**Changes to StreakAlertWorker:**
```
work/src/main/kotlin/com/verdant/work/worker/StreakAlertWorker.kt
```
- Inject RiskScoringEngine
- For each active habit, compute score
- Fire notification when score > 0.7 AND within quiet hours window (respects existing DataStore quietHoursStart/End)
- Personalized notification: "You usually skip guitar on Wednesdays — tap for 5 minutes now"
- Persist snapshot to HabitRiskSnapshotEntity for pattern mining

### 1.2 Danger Zone Home Card

When 3+ habits simultaneously have risk > 0.85, a persistent in-app warning card appears on the home screen with one-tap log buttons.

**Touch points:**
```
feature/home/src/main/kotlin/com/verdant/feature/home/HomeViewModel.kt
  — add riskScores: Map<String, Float> to HomeUiState
  — add dangerZoneHabits: List<Habit> computed field

feature/home/src/main/kotlin/com/verdant/feature/home/HomeScreen.kt
  — render DangerZoneCard in the existing alerts/highlights section
  — reuse existing AlertCard composable from core/designsystem
```

HomeViewModel already loads streaks and alerts on startup. Add riskScoreRepository.getHighRiskHabits() in the same coroutine scope — no new loading state needed.

### 1.3 Pattern Mining Worker

Analyzes historical data weekly to find skip patterns, co-occurrence, and time-of-day signatures.

**New file:**
```
work/src/main/kotlin/com/verdant/work/worker/PatternMiningWorker.kt
  — interval: 7 days, fires Sunday AM before WeeklySummaryWorker
  — calls VerdantAIRouter.findPatterns() — already exists in CloudAI.kt
  — stores results as AIInsightEntity with type PATTERN (InsightType enum already handles this)
  — no new screens needed — insights feed renders it automatically
```

Registered in VerdantApplication.kt alongside existing worker registrations.

---

## PHASE 2 — Ghost Mode / Passive Tracking
*"The app that tracks when you forget to track"*

**Architecture decision:** Mirror the exact pattern of `core/sms/` for all new auto-tracking sources. Each source gets a reader, a mapper, and a periodic worker. All auto-logged entries flow through the existing `LogEntryUseCase` unchanged. New columns on HabitEntryEntity carry the provenance.

**Schema additions (v5 migration):**
```
HabitEntryEntity — add: autoLogged: Boolean DEFAULT false, autoTrackSource: String? DEFAULT null
HabitEntity      — add: autoTrackSource: String? DEFAULT null, geofenceEnabled: Boolean DEFAULT false
```

autoTrackSource values: "HEALTH_CONNECT", "LOCATION_GEOFENCE", "SCREEN_TIME", null (manual).

### 2.1 Health Connect Integration

**New module:** `core/healthconnect/`

```
core/healthconnect/src/main/kotlin/com/verdant/core/healthconnect/
  HealthConnectReader.kt     — wraps HealthConnectClient, reads Steps/ExerciseSession/Sleep records
  HealthConnectMapper.kt     — maps records to HabitEntry candidates
  HealthConnectSyncWorker.kt — interval: 2h, mirrors SmsProcessingWorker exactly
```

HealthConnectSyncWorker logic:
1. Gate on ACTIVITY_RECOGNITION permission + Health Connect availability
2. Read sessions since lastHealthConnectSyncTime (new DataStore key)
3. For each exercise-type habit with autoTrackSource = HEALTH_CONNECT, map records to value
4. Call LogEntryUseCase with autoLogged = true
5. Update lastHealthConnectSyncTime
6. Post dismissible confirmation notification: "Auto-logged: 6,234 steps → Walking habit ✓"

**New DataStore keys in UserPreferencesDataStore.kt:**
```
lastHealthConnectSyncTime: Long (default 0)
healthConnectEnabled: Boolean (default false)
```

Permission flow added to existing settings permissions section — same UI pattern as SMS permission toggle.

### 2.2 Location Geofencing (Smart Check-In)

Auto-complete location-based habits on arrival. HabitEntryEntity already stores latitude/longitude — this makes detection automatic instead of manual.

**New module:** `core/geofence/`

```
core/geofence/src/main/kotlin/com/verdant/core/geofence/
  GeofenceManager.kt            — wraps Geofencing API, registers/removes fences per habitId
  GeofenceTransitionReceiver.kt — BroadcastReceiver for ENTER/EXIT intents
  GeofenceTransitionWorker.kt   — enqueued by receiver, calls LogEntryUseCase
  HabitGeofenceMapper.kt        — resolves habitId from geofence transition event
```

**New Room entity:**
```
core/database/src/main/kotlin/com/verdant/core/database/entity/HabitPlaceEntity.kt
fields: id, habitId (FK), name: String, lat: Double, lon: Double,
        radiusMeters: Float, triggerOn: String (ENTER/EXIT/BOTH)
```

On geofence ENTER: GeofenceTransitionWorker calls LogEntryUseCase.logLocation(habitId, lat, lon, autoLogged = true).

For ambiguous habits: send "Confirm?" notification with Yes/No actions. If no response in 2 hours, auto-confirm. Uses existing NotificationActionReceiver pattern with a new ACTION_CONFIRM_AUTO_LOG action constant.

GeofenceManager called from HabitDetailScreen when user enables geofencing toggle (new UI in habit settings section).

### 2.3 Screen Time / Digital Detox Auto-Tracking

For "No social media after 9 PM" habits, read UsageStatsManager instead of requiring manual input.

**New file:**
```
core/common/src/main/kotlin/com/verdant/core/common/UsageStatsReader.kt
  — reads UsageStatsManager (requires PACKAGE_USAGE_STATS permission)
  — returns Map<packageName, totalMinutes> for a given time window
```

Habits with autoTrackSource = SCREEN_TIME store a targetApp: String (package name) in HabitEntity metadata. HealthConnectSyncWorker handles this alongside Health Connect — same worker, additional data source. Worker checks usage against the habit's threshold and logs BINARY completion accordingly.

---

## PHASE 3 — Adaptive Difficulty
*"The app that grows with you"*

### 3.1 Target Auto-Adjustment Engine

For NUMERIC habits, suggests (never silently applies) updated targets based on 7-day rolling performance.

**Current gap:** HabitEntity.targetValue is a static Float. Set once, never changes.

**New use case:**
```
core/database/src/main/kotlin/com/verdant/core/database/usecase/AdaptiveTargetUseCase.kt
```

Algorithm:
```
val rollingAvg = last7DaysValues.average()
val completionRate = last7DaysCompleted / 7.0

when {
    completionRate > 0.90 && rollingAvg > targetValue * 1.15 ->
        suggest INCREASE by 10%
    completionRate < 0.40 ->
        suggest DECREASE by 10%
    else -> no suggestion
}
```

**New Room entity:**
```
core/database/src/main/kotlin/com/verdant/core/database/entity/HabitTargetHistoryEntity.kt
fields: id, habitId (FK), oldTarget: Float, newTarget: Float,
        changedAt: Long, reason: String (AUTO_SUGGESTED / USER_MANUAL)
```

**UX — never silent.** Suggestions surface as an adaptation card in the insights feed:
> "You've averaged 7.2km this week. Ready to bump your goal to 7.5km? [Update] [Not yet]"

Tapping Update calls new HabitRepository.updateTarget(habitId, newTarget) and writes to HabitTargetHistoryEntity. Tapping "Not yet" calls existing AIInsightDao.dismiss().

Runs inside the existing DailyMotivationWorker after motivation generation — no new worker needed.

### 3.2 Habit Splitting Suggestions

When a habit has low completion AND high variance (some days 0, some days way over target), AI suggests splitting into a baseline + stretch habit.

Uses existing COM_B insight type in InsightType enum — no new enum value. PatternMiningWorker (Phase 1.3) detects high-variance habits and passes to VerdantAIRouter.findPatterns(). Insight card gets a [Split habit] action button that navigates to CreateHabitScreen with pre-filled values from the JSON payload.

---

## PHASE 4 — Smart Reminders (Context-Aware)
*"Notifications that understand your life"*

### 4.1 Context Signal Aggregator

**New module:** `core/context/`

```
core/context/src/main/kotlin/com/verdant/core/context/
  ContextSignalProvider.kt   — reads device state, returns DeviceContextSignal
  WeatherContextProvider.kt  — lightweight weather check for outdoor habits
```

DeviceContextSignal fields:
```kotlin
data class DeviceContextSignal(
    val isCharging: Boolean,               // BatteryManager
    val isHeadphonesConnected: Boolean,    // AudioManager (music/podcast habits)
    val estimatedActivity: ActivityType,   // STILL / WALKING / DRIVING via ActivityRecognition API
    val currentHour: Int,
    val isWeekend: Boolean,
    val networkType: NetworkType           // WIFI / CELLULAR / NONE
)
```

Suppression rules applied in StreakAlertWorker and HabitReminderWorker before posting any notification:

| Condition | Suppressed habits |
|-----------|------------------|
| DRIVING detected | Gym, Cycling, Reading |
| Charging + Stationary + Evening | Surface productivity habits |
| Headphones connected | Music practice habits — send NOW instead |
| Weekend + Morning | Reduce urgency for work habits |

Pure enhancement to existing workers — no new workers. Both files:
```
work/src/main/kotlin/com/verdant/work/worker/StreakAlertWorker.kt
work/src/main/kotlin/com/verdant/work/worker/HabitReminderWorker.kt
```

### 4.2 Learned Optimal Timing

PatternMiningWorker (Phase 1.3) tracks "notification sent → habit completed within 1 hour" success rates by time slot. After 2 weeks of data, it writes a learned optimal hour per habit to DataStore.

**New DataStore key:**
```
learnedOptimalHour: Map<habitId, Int>  — if set, overrides user's manual reminder time
```

HabitReminderWorker reads this key and schedules alarms at the learned time instead of the configured time.

### 4.3 Weather-Aware Reminders

For outdoor habits (cycling, running, walking), suppress the reminder if precipitation is likely.

```
core/context/src/main/kotlin/com/verdant/core/context/WeatherContextProvider.kt
  — calls a lightweight weather API (Open-Meteo — free, no key required)
  — returns: isPrecipitationLikely: Boolean, temperatureCelsius: Float
  — result cached for 3 hours in memory to avoid redundant API calls
```

HabitReminderWorker checks WeatherContextProvider before posting. If isPrecipitationLikely and habit has outdoorActivity = true (new optional flag on HabitEntity), the notification is suppressed and rescheduled for +3 hours.

---

## PHASE 5 — Voice-First Interface
*"Track without touching the screen"*

### 5.1 Voice Entry (The Existing TODO)

The `/* TODO: voice input via SpeechRecognizer */` in ConversationalEntrySection.kt is the implementation entry point.

**New module:** `core/voice/`

```
core/voice/src/main/kotlin/com/verdant/core/voice/
  VoiceRecognitionManager.kt  — wraps SpeechRecognizer, exposes Flow<VoiceState>
  VoiceCommandParser.kt       — parses recognized text → HabitLogCommand
```

VoiceState:
```kotlin
sealed class VoiceState {
    object Idle : VoiceState()
    object Listening : VoiceState()
    data class Recognized(val text: String) : VoiceState()
    data class Error(val reason: String) : VoiceState()
}
```

**Architecture decision:** Voice input is just a new input path into the existing Brain Dump pipeline. Recognized text → existing BrainDumpViewModel.parseNaturalLanguage() → same NL2JSON AI → same LogEntryUseCase calls. Zero duplication.

**Touch points:**
```
feature/habits/src/main/kotlin/com/verdant/feature/habits/create/ConversationalEntrySection.kt
  — fill the TODO with VoiceRecognitionManager integration
  — hold-to-talk mic button, releases to submit text

feature/home/src/main/kotlin/com/verdant/feature/home/HomeScreen.kt
  — floating mic FAB (bottom-right, above bottom nav)
  — "Logged 5km run and 30 minutes reading" → both habits logged, confirmation card
```

### 5.2 Voice Coach Conversations

The existing InsightsScreen Coach Chat tab gets voice input/output.

```
core/voice/src/main/kotlin/com/verdant/core/voice/VoiceChatManager.kt
  — input:  VoiceRecognitionManager (SpeechRecognizer)
  — output: Android TextToSpeech API
  — lifecycle: tied to InsightsViewModel, released on screen exit
```

User speaks "Why am I struggling with meditation?" → voice → text → existing coach chat pipeline (InsightsViewModel.sendCoachMessage()) → AI response → TTS reads it back.

Touch point:
```
feature/insights/src/main/kotlin/com/verdant/feature/insights/InsightsScreen.kt
  — mic button in coach chat input row
  — speaker icon on AI response bubbles (tap to re-read)
```

### 5.3 Duration Habit Timer (Voice-Triggered)

The TrackingType enum currently has BINARY, NUMERIC, LOCATION. A proper DURATION type is needed for voice flows: "Start meditation timer" → foreground timer → auto-logs on stop.

**Schema addition:**
```
TrackingType enum: add DURATION
HabitEntity: no new columns needed — targetValue stores the target duration in seconds
HabitEntryEntity: value stores the actual duration logged in seconds
```

**New foreground service:**
```
work/src/main/kotlin/com/verdant/work/service/HabitTimerService.kt
  — foreground service with persistent notification showing elapsed time
  — notification actions: Pause / Stop & Log
  — on stop: calls LogEntryUseCase with duration value and autoLogged = false
```

The existing QuickToggle widget (already exists in widget/) shows a live timer countdown for DURATION habits.

---

## PHASE 6 — Onboarding Intelligence
*"The app that builds your habit plan for you"*

### 6.1 AI Interview Flow

Replace the generic step-by-step wizard with a 5-question AI interview on first launch.

**Current state:** onboardingCompleted flag exists in DataStore. Settings module has onboarding screens. The wizard is a standard step-by-step flow.

**New files:**
```
feature/settings/src/main/kotlin/com/verdant/feature/settings/onboarding/
  AIInterviewScreen.kt      — 5-question conversational UI
  AIInterviewViewModel.kt   — state + AI call management
```

Interview questions:
1. "What's your main goal right now?" (health / productivity / learning / finance / wellbeing)
2. "How many habits do you realistically want to track?" (1–3 / 4–7 / 8+)
3. "Are you a morning person or night owl?" (Morning / Night / Variable)
4. "Have you tried habit tracking before?" (First time / Tried and quit / Currently doing it)
5. "What's the ONE thing you most want to improve this month?" (free text)

Q5 free text → VerdantAIRouter.parseHabitDescription() (already exists). Interview ends by auto-creating 3–5 suggested habits. User reviews, toggles on/off, confirms. All habits created in one tap.

**New CloudAI method:**
```
core/ai/src/main/kotlin/com/verdant/core/ai/CloudAI.kt
  — add: suspend fun generatePersonalizedHabitPlan(answers: InterviewAnswers): List<HabitSuggestion>
```

### 6.2 Progressive Feature Disclosure

Don't show all features at once. Gate on usage milestones.

**New DataStore keys:**
```
habitsLoggedCount: Int (default 0)
daysActive: Int (default 0)
```

**New utility:**
```
core/common/src/main/kotlin/com/verdant/core/common/FeatureDiscoveryManager.kt
  — isUnlocked(feature: VerdantFeature): Boolean
  — markSeen(feature: VerdantFeature)
```

Unlock gates:
- Day 1–3: BINARY tracking only, no widgets, no analytics
- Day 4–7: NUMERIC tracking + first widget suggestion toast
- Week 2+: Analytics tab, adaptive targets
- Day 30+: Social features, advanced AI insights

UI: subtle "New feature unlocked" card on home screen using existing AlertCard composable. Not a modal — non-blocking.

---

## PHASE 7 — Social & Accountability
*"The optional human layer"*

### 7.1 Habit Buddies

**Architecture decision:** Keep server-side minimal. Use Firebase Realtime Database (already in the project) for buddy connections. No new user accounts — generate a shareable 6-digit code per habit.

**New module:** `core/social/`

```
core/social/src/main/kotlin/com/verdant/core/social/
  SocialRepository.kt          — Firebase RTDB reads/writes
  HabitBuddyEntity.kt          — local Room cache of buddy connections
  BuddyInviteActivity.kt       — deep-link handler (verdant://buddy/[habitId]/[code])
```

Share flow: HabitDetailScreen → Share → generates invite code → recipient opens link → sees habit name + streak → taps "Be my buddy." Both users get streak-only updates (no values for NUMERIC habits — privacy-first).

### 7.2 Anonymous Streak Comparison

New insight type: ANONYMOUS_COMPARISON. "87% of people tracking 'Exercise 30 min' have a shorter streak than you."

Firebase Cloud Function aggregates anonymized streak percentiles across all users — no individual data exposed. Result delivered as AIInsightEntity, rendered by existing insights feed.

**New DataStore key:**
```
anonymousComparisonEnabled: Boolean (default false, user opts in)
```

---

## PHASE 8 — Data Export & Interoperability

### 8.1 Complete the ExportUseCase

ExportUseCase already exists at:
```
core/database/src/main/kotlin/com/verdant/core/database/usecase/ExportUseCase.kt
```
It is incomplete. Fill it out:

```kotlin
enum class ExportFormat { CSV, JSON, MARKDOWN }
data class ExportConfig(
    val format: ExportFormat,
    val habitIds: List<String>?,  // null = all habits
    val dateRange: ClosedRange<LocalDate>?
)
```

- **CSV:** one row per HabitEntry, all metadata columns (stressLevel, energyLevel, missedReason, autoLogged, autoTrackSource)
- **JSON:** full Habit object with embedded entries[] array — importable by other apps
- **Markdown:** Year in Review narrative format, human-readable

Share via `ShareCompat.IntentBuilder` — user can save to Drive, email, AirDrop to Notion.

Export triggered from SettingsScreen → new "Export Data" row in the existing data management accordion section.

### 8.2 Google Sheets Sync (Optional)

**New worker:**
```
work/src/main/kotlin/com/verdant/work/worker/GoogleSheetsSyncWorker.kt
  — interval: weekly
  — requires: Google Drive OAuth2 scope in AndroidManifest
  — appends new HabitEntry rows to user-specified spreadsheet
  — gate: googleSheetsEnabled = true AND googleSheetId is set
```

**New DataStore keys:**
```
googleSheetsEnabled: Boolean (default false)
googleSheetId: String? (default null)
lastSheetsSyncTime: Long (default 0)
```

Off by default. User enables in Settings → Integrations section (new accordion).

---

## PHASE 9 — Experience Continuity & Polish

### 9.1 Shared Element Transitions

Upgrade to Compose shared element transitions (available in Compose 1.7 via sharedElement() modifier):

- Habit card in list → Habit detail: completion ring morphs/expands into detail header
- Home insight card → Insights feed: card expands in-place
- Contribution grid cell → Day detail: cell expands into day editing screen

Touch points:
```
feature/habits/src/main/kotlin/com/verdant/feature/habits/HabitsScreen.kt
feature/habits/src/main/kotlin/com/verdant/feature/habits/detail/HabitDetailScreen.kt
feature/home/src/main/kotlin/com/verdant/feature/home/HomeScreen.kt
feature/insights/src/main/kotlin/com/verdant/feature/insights/InsightsScreen.kt
```

Pure UI change — no architecture changes.

### 9.2 Full-Screen Milestone Celebrations

When users hit milestones (7, 30, 100, 365 days), show a full-screen celebration on next app open instead of just a notification.

**New composable:**
```
core/designsystem/src/main/kotlin/com/verdant/core/designsystem/component/CelebrationOverlay.kt
  — full-screen with Lottie confetti animation
  — personalized AI message (generated by DailyMotivationWorker at milestone time)
  — "Your story so far" summary card
```

**New DataStore key:**
```
pendingCelebrations: Set<String> (milestone type IDs)
```

MainActivity checks pendingCelebrations on each launch. If non-empty, shows CelebrationOverlay before navigating to home.

### 9.3 Adaptive Home Screen States

Home screen currently has fixed sections. Add HomeUiState variants:

```kotlin
sealed class HomeState {
    object Normal : HomeState()
    object AllDoneEarly : HomeState()   // all habits done before noon
    object NothingDueToday : HomeState()
    object SundayEvening : HomeState()  // auto-show week review
    object ReEngagement : HomeState()   // user hasn't opened in 2+ days
    data class DangerZone(val habits: List<Habit>) : HomeState()
}
```

Touch points:
```
feature/home/src/main/kotlin/com/verdant/feature/home/HomeViewModel.kt
feature/home/src/main/kotlin/com/verdant/feature/home/HomeScreen.kt
```

---

## PHASE 10 — Robustness & Scalability

### 10.1 Logging & Observability

Add Timber throughout. Every `runCatching` block currently swallows failures silently.

Pattern to apply everywhere:
```kotlin
runCatching { ... }.onFailure { Timber.e(it, "ClassName: operation failed") }
```

Key workers to instrument first:
```
work/src/main/kotlin/com/verdant/work/worker/DailyMotivationWorker.kt
work/src/main/kotlin/com/verdant/work/worker/StreakAlertWorker.kt
work/src/main/kotlin/com/verdant/work/worker/SmsProcessingWorker.kt
```

Add Firebase Crashlytics (Firebase already in project) for production crash tracking.

Add VerdantAnalytics wrapper for key events (habit_created, habit_completed, ai_insight_dismissed, voice_log_success) — no PII, event-level only.

### 10.2 Pagination

HabitsListViewModel loads all habits + all entries for the last 30 days. With 50+ habits over years, this is a memory problem.

Fix:
- Paginate HabitEntryDao queries using Pager + PagingSource
- Cache computed streak values in a new StreakCacheEntity table — invalidate on new entry insertion, not on every read

**New Room entity:**
```
core/database/src/main/kotlin/com/verdant/core/database/entity/StreakCacheEntity.kt
fields: habitId (PK), currentStreak: Int, longestStreak: Int, completionRate: Float, cachedAt: Long
```

CalculateStreakUseCase writes to this cache after computation. HabitsListViewModel reads from cache first, falls back to full computation only on cache miss or invalidation.

### 10.3 Circuit Breaker for CloudAI

CloudAI propagates RateLimitException but has no backoff logic.

**New file:**
```
core/ai/src/main/kotlin/com/verdant/core/ai/CloudAICircuitBreaker.kt
  — state: CLOSED (normal) / OPEN (suspended) / HALF_OPEN (testing)
  — opens after 3 consecutive RateLimitException or ServerException within 1 hour
  — stays open for 2 hours, then enters HALF_OPEN
  — persisted via DataStore key: cloudAiCircuitBreakerUntil: Long
```

VerdantAIRouter.kt checks circuit state before calling CloudAI — routes to FallbackAI if OPEN. No behavior change for the UI.

### 10.4 Offline-First AI Requests

HomeViewModel currently calls AI synchronously on load. If network is slow, home screen shows a spinner.

Fix:
- Show stale AIInsightEntity from Room cache immediately on load (already stored with expiresAt TTL)
- Queue failed AI requests in a new PendingAIRequestEntity table, retry on next WorkManager network-available run
- Apply NetworkConnected constraint to all CloudAI workers (WeeklySummaryWorker already does this — mirror the pattern to DailyMotivationWorker)

**New Room entity:**
```
core/database/src/main/kotlin/com/verdant/core/database/entity/PendingAIRequestEntity.kt
fields: id, requestType: String, payload: String (JSON), createdAt: Long, attemptCount: Int
```

### 10.5 TrackingType Enum Cleanup

The codebase has comments referencing DURATION, FINANCIAL, EMOTIONAL, EVENT_DRIVEN from an earlier design iteration. The app model and the schema diverged. The correct fix is to add the types properly:

```kotlin
// core/model/src/main/kotlin/com/verdant/core/model/TrackingType.kt
enum class TrackingType {
    BINARY,
    NUMERIC,
    LOCATION,
    DURATION,      // add — backed by HabitTimerService (Phase 5.3)
    FINANCIAL,     // add — already partially supported in LogEntryUseCase
    EMOTIONAL,     // add — mood/wellbeing habits
    EVENT_DRIVEN   // add — triggered by external event (geofence, Health Connect)
}
```

v5 DB migration updates the tracking_type column check constraint to include new values.

---

## V5 DATABASE MIGRATION — COMPLETE SUMMARY

Single migration `DatabaseMigrations.MIGRATION_4_5` in:
```
core/database/src/main/kotlin/com/verdant/core/database/DatabaseMigrations.kt
```

```sql
-- New tables
CREATE TABLE habit_risk_snapshots (
    id TEXT PRIMARY KEY NOT NULL,
    habitId TEXT NOT NULL,
    score REAL NOT NULL,
    computedAt INTEGER NOT NULL,
    triggeringFactors TEXT NOT NULL,
    FOREIGN KEY (habitId) REFERENCES habits(id) ON DELETE CASCADE
);

CREATE TABLE habit_places (
    id TEXT PRIMARY KEY NOT NULL,
    habitId TEXT NOT NULL,
    name TEXT NOT NULL,
    lat REAL NOT NULL,
    lon REAL NOT NULL,
    radiusMeters REAL NOT NULL,
    triggerOn TEXT NOT NULL,
    FOREIGN KEY (habitId) REFERENCES habits(id) ON DELETE CASCADE
);

CREATE TABLE habit_target_history (
    id TEXT PRIMARY KEY NOT NULL,
    habitId TEXT NOT NULL,
    oldTarget REAL NOT NULL,
    newTarget REAL NOT NULL,
    changedAt INTEGER NOT NULL,
    reason TEXT NOT NULL,
    FOREIGN KEY (habitId) REFERENCES habits(id) ON DELETE CASCADE
);

CREATE TABLE pending_ai_requests (
    id TEXT PRIMARY KEY NOT NULL,
    requestType TEXT NOT NULL,
    payload TEXT NOT NULL,
    createdAt INTEGER NOT NULL,
    attemptCount INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE streak_cache (
    habitId TEXT PRIMARY KEY NOT NULL,
    currentStreak INTEGER NOT NULL,
    longestStreak INTEGER NOT NULL,
    completionRate REAL NOT NULL,
    cachedAt INTEGER NOT NULL,
    FOREIGN KEY (habitId) REFERENCES habits(id) ON DELETE CASCADE
);

-- Altered tables
ALTER TABLE habit_entries ADD COLUMN auto_logged INTEGER NOT NULL DEFAULT 0;
ALTER TABLE habit_entries ADD COLUMN auto_track_source TEXT;
ALTER TABLE habits ADD COLUMN auto_track_source TEXT;
ALTER TABLE habits ADD COLUMN geofence_enabled INTEGER NOT NULL DEFAULT 0;
ALTER TABLE habits ADD COLUMN outdoor_activity INTEGER NOT NULL DEFAULT 0;
```

All nullable/defaulted — zero breaking changes to existing data.

---

## NEW MODULES SUMMARY

| Module | Mirrors pattern of | Key components |
|--------|-------------------|----------------|
| `core/healthconnect/` | `core/sms/` | HealthConnectReader, HealthConnectMapper, HealthConnectSyncWorker |
| `core/geofence/` | `core/sms/` | GeofenceManager, GeofenceTransitionReceiver, GeofenceTransitionWorker |
| `core/context/` | `core/common/` | ContextSignalProvider, WeatherContextProvider |
| `core/voice/` | standalone | VoiceRecognitionManager, VoiceCommandParser, VoiceChatManager |
| `core/social/` | `core/network/` | SocialRepository, HabitBuddyEntity, BuddyInviteActivity |

---

## IMPLEMENTATION PRIORITY

**Highest ROI / Lowest effort (do first):**
1. Voice entry — fills existing TODO, reuses Brain Dump AI pipeline (Phase 5.1)
2. Context-aware notification suppression — enhances existing workers (Phase 4.1)
3. Habit risk scoring — extends StreakAlertWorker (Phase 1.1)
4. Timber + silent failure fixes — pure robustness (Phase 10.1)
5. Adaptive targets — new use case, no new screens (Phase 3.1)

**Medium effort, high impact:**
6. Health Connect integration — mirrors SmsProcessingWorker exactly (Phase 2.1)
7. AI Interview onboarding — new screen + new CloudAI method (Phase 6.1)
8. Danger Zone home card — extends HomeViewModel states (Phase 1.2)
9. Export completion — ExportUseCase already exists (Phase 8.1)

**Larger investments:**
10. Geofencing — new module + new DB table + new worker (Phase 2.2)
11. Circuit breaker for CloudAI — architectural reliability (Phase 10.3)
12. Shared element transitions — Compose 1.7 upgrade (Phase 9.1)
13. Social/buddies — Firebase RTDB + invite deep links (Phase 7.1)
14. Google Sheets sync — OAuth2 + new worker (Phase 8.2)

---

## WHAT STAYS UNCHANGED

The three-tier AI routing (VerdantAIRouter → MediaPipe → Cloud → Fallback) is solid — do not restructure it. The LogEntryUseCase is a clean abstraction — all new auto-tracking paths funnel through it unchanged. The notification channel infrastructure already has capacity (7 channels defined). The widget system needs data refreshed, not redesigned. The existing WorkManager registration pattern in VerdantApplication.kt is the right place to register all new workers.

