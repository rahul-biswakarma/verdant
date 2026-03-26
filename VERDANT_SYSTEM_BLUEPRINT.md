# VERDANT: The System — Complete Product Blueprint

## "Arise." — From Habit Tracker to Life Evolution Engine

**Last Updated:** 2026-03-26
**Database Version:** Current v4 → Target v5
**App Architecture:** Multi-module Clean Architecture (Kotlin, Jetpack Compose, Room, Hilt, WorkManager)

---

## IMPLEMENTATION CHECKLIST

### Legend
- [x] Done — Already implemented and working
- [ ] Todo — Not yet started

---

### EXISTING FOUNDATION (What We Have)

#### Core Modules
- [x] `:app` — Main application entry point, navigation graph, Hilt setup
- [x] `:core:model` — Domain models (Habit, HabitEntry, Transaction, Label, Streak, etc.)
- [x] `:core:database` — Room DB v4, entities, DAOs, repositories, use cases
- [x] `:core:datastore` — User preferences via DataStore
- [x] `:core:network` — Retrofit + Firebase Functions + AuthInterceptor
- [x] `:core:ai` — VerdantAI interface, CloudAI (Claude), MediaPipe (Gemma 2B), FallbackAI, VerdantAIRouter
- [x] `:core:common` — HabitDataAggregator, FinanceDataAggregator, DayCellBuilder
- [x] `:core:sms` — SmsReader, RegexSmsParser, TransactionCategorizer
- [x] `:core:designsystem` — Compose components (ContributionGrid, CompletionRing, StreakRing, etc.)

#### Feature Modules
- [x] `:feature:home` — Dashboard with daily summary, AI motivation, finance card, alerts
- [x] `:feature:habits` — Habit list, create (conversational AI), detail, day detail
- [x] `:feature:analytics` — 5-tab analytics (Overview, Heatmaps, Trends, Correlations, Reports)
- [x] `:feature:insights` — AI insights feed + coach chat
- [x] `:feature:finance` — 3-tab finance (Overview, Transactions, Trends)
- [x] `:feature:settings` — Preferences, privacy, data management, onboarding
- [x] `:widget` — HabitGridWidget with responsive sizes, WidgetUpdateWorker
- [x] `:work` — Background workers module

#### Database (v4) — 6 Tables
- [x] `habits` — id, name, description, icon, color, label, trackingType, unit, targetValue, frequency, scheduleDays, reminders, visualizationType, sortOrder, createdAt
- [x] `habit_entries` — id, habitId (FK), date, completed, value, latitude, longitude, note, category, skipped, createdAt, updatedAt
- [x] `labels` — id, name, color
- [x] `ai_insights` — id, type, content, relatedHabitIds, generatedAt, expiresAt, dismissed
- [x] `transactions` — id, amount, type, merchant, category, subCategory, accountTail, bank, upiId, balanceAfter, transactionDate, rawSmsId, rawSmsBody, isRecurring, parseConfidence, userVerified, createdAt
- [x] `merchant_mappings` — id, merchantPattern, category, subCategory, useCount

#### Background Workers (5 Active)
- [x] `DailyMotivationWorker` — 24h, generates AI motivation notification at 8 AM
- [x] `StreakAlertWorker` — 2h, checks 4-7 PM for at-risk streaks, posts nudges
- [x] `WeeklySummaryWorker` — Weekly Sunday 7 PM, AI weekly report
- [x] `SmsProcessingWorker` — 2h, reads bank SMS, parses transactions
- [x] `SpendingAlertWorker` — 6h, compares spending vs last month

#### Tracking Types
- [x] `BINARY` — Yes/no habits
- [x] `NUMERIC` — Measurable value with target
- [x] `LOCATION` — GPS coordinate capture

#### AI Features
- [x] On-device AI (MediaPipe Gemma 2B) — habit parsing, nudges, milestones
- [x] Cloud AI (Claude via Firebase Functions) — insights, reports, correlations, predictions
- [x] Three-tier AI routing (MediaPipe → Cloud → Fallback)
- [x] AI Coach chat in insights tab
- [x] Daily motivation generation
- [x] Pattern recognition via `findPatterns()`
- [x] Spending prediction via `predictMonthlySpending()`
- [x] Dashboard cross-product insights

#### Permissions (Declared)
- [x] `READ_SMS` — Bank transaction parsing
- [x] `ACCESS_FINE_LOCATION` — Location-type habits
- [x] `ACCESS_COARSE_LOCATION` — Location fallback
- [x] `POST_NOTIFICATIONS` — Push notifications
- [x] `USE_EXACT_ALARM` / `SCHEDULE_EXACT_ALARM` — Habit reminders
- [x] `RECEIVE_BOOT_COMPLETED` — Re-register alarms after reboot
- [x] `WAKE_LOCK` — Background workers

#### Authentication
- [x] Firebase Auth with Google Sign-In (Credential Manager)
- [x] AuthInterceptor for token injection

#### Spending Categories (12)
- [x] FOOD, TRANSPORT, SHOPPING, BILLS, ENTERTAINMENT, HEALTH, EDUCATION, TRANSFERS, INVESTMENTS, SALARY, CASH, OTHER

---

### PHASE 1: FOUNDATION — Database + Core Infrastructure

#### New Modules
- [ ] `:core:health` — HealthConnect client wrapper, readers, mappers
- [ ] `:core:devicestats` — UsageStatsReader, CalendarReader, BatteryTracker
- [ ] `:core:prediction` — On-device statistical prediction models (pure Kotlin)

#### Database Migration v4 → v5 — New Tables
- [ ] `health_records` — id, record_type, value, secondary_value, unit, recorded_at, source, created_at
- [ ] `activity_records` — id, activity_type, confidence, duration_minutes, recorded_at, created_at
- [ ] `device_stats` — id, stat_type, value, detail (JSON), recorded_date, created_at
- [ ] `weather_snapshots` — id, date, temperature, condition, humidity, latitude, longitude, created_at
- [ ] `life_scores` — id, score_type, score (0-100), components (JSON), computed_date, created_at
- [ ] `predictions` — id, prediction_type, target_period, prediction_data (JSON), confidence, generated_at, expires_at
- [ ] `budgets` — id, name, category, amount, period, is_active, created_at
- [ ] `recurring_transactions` — id, merchant, category, typical_amount, frequency_days, last_seen, next_expected, confidence, is_active, created_at
- [ ] `habit_risk_snapshots` — id, habitId (FK), score, computedAt, triggeringFactors (JSON)
- [ ] `habit_places` — id, habitId (FK), name, lat, lon, radiusMeters, triggerOn
- [ ] `habit_target_history` — id, habitId (FK), oldTarget, newTarget, changedAt, reason
- [ ] `pending_ai_requests` — id, requestType, payload (JSON), createdAt, attemptCount
- [ ] `streak_cache` — habitId (PK), currentStreak, longestStreak, completionRate, cachedAt
- [ ] `emotional_context` — id, date, inferred_mood, energy_level, confidence, contributing_signals (JSON), user_confirmed
- [ ] `player_profile` — id, level, title, totalXP, currentLevelXP, xpToNextLevel, rank, stats (JSON), evolutionPath, created_at, updated_at
- [ ] `quests` — id, title, description, difficulty, xpReward, conditions (JSON), timeLimit, generatedBy, reasoning, status, started_at, completed_at
- [ ] `achievements` — id, title, description, xpReward, unlockedAt, category
- [ ] `device_signals` — id, device_id, signal_type, value, unit, timestamp, created_at
- [ ] `cross_correlations` — id, dimension_a, dimension_b, correlation_strength, description, discovered_at, sample_size

#### Column Additions to Existing Tables
- [ ] `habit_entries` + `auto_logged` (Boolean), `auto_track_source` (String?)
- [ ] `habits` + `auto_track_source` (String?), `geofence_enabled` (Boolean), `outdoor_activity` (Boolean)
- [ ] `transactions` + `recurring_group_id` (String?)

#### New Domain Models (`:core:model`)
- [ ] `HealthRecord.kt` + `HealthRecordType` enum (STEPS, SLEEP, HEART_RATE, WEIGHT, EXERCISE, HYDRATION)
- [ ] `ActivityRecord.kt` + `ActivityType` enum (WALKING, RUNNING, CYCLING, STILL, IN_VEHICLE)
- [ ] `DeviceStat.kt` + `DeviceStatType` enum (SCREEN_TIME, APP_USAGE, NOTIFICATION_COUNT, BATTERY_DRAIN, CALENDAR_BUSY_HOURS)
- [ ] `WeatherSnapshot.kt` + `WeatherCondition` enum
- [ ] `LifeScore.kt` + `ScoreType` enum (HEALTH, FINANCIAL, PRODUCTIVITY, WELLNESS, LIFESTYLE, STRESS)
- [ ] `Prediction.kt` + `PredictionType` enum (SPENDING_FORECAST, HABIT_SUSTAINABILITY, HEALTH_TRAJECTORY, LIFE_FORECAST)
- [ ] `Budget.kt` + `BudgetPeriod` enum
- [ ] `RecurringTransaction.kt`
- [ ] `EmotionalContext.kt` + `InferredMood` enum (ENERGIZED, NEUTRAL, LOW, STRESSED, ANXIOUS)
- [ ] `PlayerProfile.kt` + `PlayerRank` enum (E, D, C, B, A, S) + `PlayerStats` data class
- [ ] `Quest.kt` + `QuestDifficulty` enum (DAILY, WEEKLY, EPIC, LEGENDARY) + `QuestCondition`
- [ ] `Achievement.kt`
- [ ] `DeviceSignal.kt`
- [ ] `CrossCorrelation.kt`
- [ ] `EvolutionPath` enum (VITALITY, WISDOM, MASTERY, BALANCE, SHADOW)

#### New DAOs
- [ ] `HealthRecordDao.kt`
- [ ] `ActivityRecordDao.kt`
- [ ] `DeviceStatDao.kt`
- [ ] `WeatherDao.kt`
- [ ] `LifeScoreDao.kt`
- [ ] `PredictionDao.kt`
- [ ] `BudgetDao.kt`
- [ ] `RecurringTransactionDao.kt`
- [ ] `EmotionalContextDao.kt`
- [ ] `PlayerProfileDao.kt`
- [ ] `QuestDao.kt`
- [ ] `AchievementDao.kt`
- [ ] `DeviceSignalDao.kt`
- [ ] `CrossCorrelationDao.kt`

#### New Repositories
- [ ] `HealthRecordRepository.kt`
- [ ] `DeviceStatRepository.kt`
- [ ] `LifeScoreRepository.kt`
- [ ] `PredictionRepository.kt`
- [ ] `BudgetRepository.kt`
- [ ] `RecurringTransactionRepository.kt`
- [ ] `EmotionalContextRepository.kt`
- [ ] `PlayerProfileRepository.kt`
- [ ] `QuestRepository.kt`

#### DataStore Preference Keys
- [ ] `HEALTH_CONNECT_ENABLED` (Boolean, default false)
- [ ] `ACTIVITY_RECOGNITION_ENABLED` (Boolean, default false)
- [ ] `SCREEN_TIME_TRACKING_ENABLED` (Boolean, default false)
- [ ] `CALENDAR_SYNC_ENABLED` (Boolean, default false)
- [ ] `WEATHER_TRACKING_ENABLED` (Boolean, default false)
- [ ] `NOTIFICATION_TRACKING_ENABLED` (Boolean, default false)
- [ ] `LIFE_DASHBOARD_ONBOARDING_COMPLETED` (Boolean, default false)
- [ ] `LAST_HEALTH_SYNC_TIME` (Long, default 0)
- [ ] `LAST_DEVICE_STATS_SYNC_TIME` (Long, default 0)
- [ ] `EVOLUTION_PATH` (String, default "BALANCE")
- [ ] `PLAYER_LEVEL_SHOWN` (Int, default 0) — for level-up detection

#### New Permissions
- [ ] `android.permission.health.READ_STEPS`
- [ ] `android.permission.health.READ_SLEEP`
- [ ] `android.permission.health.READ_HEART_RATE`
- [ ] `android.permission.health.READ_EXERCISE`
- [ ] `android.permission.health.READ_WEIGHT`
- [ ] `android.permission.health.READ_HYDRATION`
- [ ] `android.permission.health.READ_NUTRITION`
- [ ] `android.permission.ACTIVITY_RECOGNITION`
- [ ] `android.permission.READ_CALENDAR`
- [ ] `android.permission.PACKAGE_USAGE_STATS`

#### Data Sources Settings Screen
- [ ] `DataSourcesScreen.kt` — toggle for each data source with explanation cards
- [ ] `DataAuditScreen.kt` — shows data collected per category with counts and storage

---

### PHASE 2: DATA COLLECTION — Workers & Passive Tracking

#### New Background Workers
- [ ] `HealthSyncWorker` — 3h, reads HealthConnect → `health_records`
- [ ] `DeviceStatsSyncWorker` — 6h, UsageStats + Calendar + Battery → `device_stats`
- [ ] `WeatherSyncWorker` — 12h, weather API → `weather_snapshots`
- [ ] `RecurringTransactionDetectorWorker` — 24h, frequency analysis → `recurring_transactions`
- [ ] `NotificationCounterService` — continuous NotificationListenerService → batch daily to `device_stats`

#### New Module: `:core:geofence`
- [ ] `GeofenceManager.kt` — wraps Geofencing API, registers/removes fences per habitId
- [ ] `GeofenceTransitionReceiver.kt` — BroadcastReceiver for ENTER/EXIT
- [ ] `GeofenceTransitionWorker.kt` — processes transitions, calls LogEntryUseCase
- [ ] `HabitGeofenceMapper.kt` — resolves habitId from geofence event

#### Ghost Mode (Passive Auto-Tracking)
- [ ] HealthConnect → auto-log exercise/steps habits (autoLogged = true, autoTrackSource = "HEALTH_CONNECT")
- [ ] Geofence → auto-log location habits on arrival (autoTrackSource = "LOCATION_GEOFENCE")
- [ ] UsageStats → auto-log screen time / digital detox habits (autoTrackSource = "SCREEN_TIME")
- [ ] Confirmation notification for ambiguous auto-logs ("Confirm?" with Yes/No actions)

#### Activity Recognition
- [ ] Activity Recognition API integration via Google Play Services
- [ ] PendingIntent-based callbacks for activity transitions
- [ ] Process activity segments into `activity_records`

---

### PHASE 3: INTELLIGENCE — Emotional Engine + Predictions

#### New Module: `:core:emotional`
- [ ] `SignalFusionLayer.kt` — combines micro-signals into emotional state
- [ ] `EmotionalStateMachine.kt` — FLOW, PLATEAU, DRIFT, CRISIS, RECOVERY, SURGE states
- [ ] `BehavioralModel.kt` — learns per-user patterns (day-of-week failures, trigger chains)
- [ ] `EmotionalMemory.kt` — stores/retrieves emotional context with confidence scoring

#### Emotional State Machine
- [ ] FLOW detection — high completion + low screen time + consistent sleep
- [ ] PLATEAU detection — consistent but flat, no improvement over 14+ days
- [ ] DRIFT detection — gradual decline over 7+ days across multiple dimensions
- [ ] CRISIS detection — multiple dimensions crashing simultaneously
- [ ] RECOVERY detection — upward trend after CRISIS/DRIFT
- [ ] SURGE detection — sudden spike in all metrics
- [ ] State transition responses (raise targets / reduce targets / compassionate messaging)

#### Risk Scoring Engine
- [ ] `RiskScoringEngine.kt` — 0.0-1.0 composite risk per habit
- [ ] Time pressure factor (how far into day vs typical completion window)
- [ ] Historical day-of-week failure rate
- [ ] Streak value urgency multiplier
- [ ] Recent miss density (2+ misses in last 7 days)
- [ ] Integrate into `StreakAlertWorker` (extend, not replace)

#### On-Device Prediction Models (`:core:prediction`)
- [ ] `SpendingForecaster.kt` — linear regression on 3-6 months category totals
- [ ] `HabitSustainabilityScorer.kt` — logistic model (streak length, completion decay, variance)
- [ ] `HealthTrajectoryPredictor.kt` — weighted moving average + trend extrapolation
- [ ] `StressIndexCalculator.kt` — composite z-scores from screen time, notifications, sleep, spending, misses
- [ ] `FinancialHealthScorer.kt` — savings rate, volatility, recurring ratio, category diversity
- [ ] `LifestyleScorer.kt` — weighted composite: Health 30%, Finance 25%, Productivity 25%, Wellness 20%

#### New Workers
- [ ] `EmotionalEngineWorker` — 6h, runs signal fusion → `emotional_context`
- [ ] `LifeScoreComputeWorker` — 12h, runs all scorers → `life_scores`
- [ ] `PredictionWorker` — 24h, runs forecasters → `predictions`
- [ ] `PatternMiningWorker` — 7 days, discovers cross-domain correlations → `cross_correlations`

#### Cross-Domain Correlations
- [ ] Sleep quality → Next-day productivity
- [ ] Spending patterns → Emotional state
- [ ] Exercise → Financial discipline
- [ ] Screen time → Sleep quality
- [ ] Weather → Habit compliance
- [ ] Social media → Mood trajectory
- [ ] Calendar load → Evening habits

#### AI Extensions
- [ ] `VerdantAI.generateLifeForecast(context: LifeForecastContext): LifeForecast`
- [ ] `VerdantAI.generateHealthInsight(data: HealthSummaryData): String`
- [ ] `VerdantAI.predictHabitSustainability(data: HabitHistoryData): SustainabilityPrediction`
- [ ] `VerdantAI.findCrossDomainCorrelations(data: CrossDomainData): List<CrossCorrelation>`
- [ ] `LifeDataAggregator.kt` — aggregates cross-domain data for AI context

---

### PHASE 4: THE SYSTEM UI — Life Dashboard + Player System

#### New Module: `:feature:lifedashboard`
- [ ] `LifeDashboardScreen.kt` — main screen with scrollable card layout
- [ ] `LifeDashboardViewModel.kt` — combines all score flows, predictions, player data
- [ ] `LifeDashboardUiState.kt` — state model

#### Player System
- [ ] `PlayerProfile` data class — level (1-100), title, XP, rank (E/D/C/B/A/S), stats, evolution path
- [ ] `PlayerStats` — vitality, discipline, wisdom, focus, resilience, awareness (each 0-100)
- [ ] XP calculation system (10 XP per habit, 50 for all daily, multipliers for streaks)
- [ ] Leveling curve (E: 1-5, D: 6-15, C: 16-30, B: 31-50, A: 51-75, S: 76-100)
- [ ] Title generation per rank tier
- [ ] Evolution Paths — Vitality, Wisdom, Mastery, Balance, Shadow
- [ ] Path selection UI
- [ ] `XPComputeWorker` — 6h, calculates XP from all completions

#### Quest System
- [ ] `Quest` data class — title, description, difficulty, conditions, XP reward, reasoning
- [ ] AI-generated personalized quests based on weak spots
- [ ] Quest difficulty tiers: DAILY, WEEKLY, EPIC, LEGENDARY
- [ ] Quest progress tracking
- [ ] Quest completion + XP reward
- [ ] `QuestGeneratorWorker` — 24h, generates quests from emotional engine + patterns

#### Dashboard UI Components
- [ ] `PlayerCard.kt` — level, rank, XP bar, evolution path
- [ ] `EmotionalStateBanner.kt` — current state with contextual message
- [ ] `StatDimensionGrid.kt` — 6 stat cards (vitality/discipline/wisdom/focus/resilience/awareness) with trends
- [ ] `ActiveQuestCard.kt` — quest progress with XP reward
- [ ] `PredictionCard.kt` — spending forecast, habit sustainability, health trajectory
- [ ] `SystemInsightCard.kt` — cross-domain AI insight
- [ ] `LifeForecastCard.kt` — 7-day AI narrative forecast
- [ ] `DimensionDetailSheet.kt` — bottom sheet drill-down per dimension
- [ ] `TrendLineChart.kt` — Compose Canvas chart for trend lines
- [ ] `StressGauge.kt` — visual stress meter

#### Danger Zone Home Card
- [ ] When 3+ habits have risk > 0.85, show persistent warning on home screen
- [ ] One-tap log buttons for at-risk habits
- [ ] Extend `HomeViewModel` with `riskScores` and `dangerZoneHabits`

#### Navigation
- [ ] Add Life Dashboard destination to app NavGraph
- [ ] Add bottom nav item or prominent card on Home screen
- [ ] Life score summary card on Home screen

#### New Insight Types
- [ ] `SPENDING_FORECAST`
- [ ] `HEALTH_INSIGHT`
- [ ] `STRESS_ALERT`
- [ ] `LIFE_FORECAST`
- [ ] `CROSS_CORRELATION`
- [ ] `BUDGET_ALERT`
- [ ] `ANOMALY`

---

### PHASE 5: MULTI-DEVICE — "The Network"

#### New Module: `:core:sync`
- [ ] `DeviceSyncManager.kt` — Firebase RTDB signal sync
- [ ] `SignalPublisher.kt` — publishes local signals to RTDB
- [ ] `SignalSubscriber.kt` — subscribes to signals from other devices
- [ ] `CrossDeviceSyncWorker` — 1h, pulls signals from RTDB → `device_signals`

#### Device Roles
- [ ] Phone as master device (fuses all signals locally)
- [ ] Device registration + management
- [ ] Signal format: `{ device_id, signal_type, value, unit, timestamp }`
- [ ] Encrypted sync — no raw data, only signal summaries

#### Mac Companion App (Spec + Prototype)
- [ ] macOS menu bar app (Swift) or browser extension spec
- [ ] Active app category tracking (Accessibility API / NSWorkspace)
- [ ] Focus session duration (screen lock/unlock + app switching)
- [ ] Coding time (VS Code / JetBrains extension)
- [ ] Meeting time (Calendar API)
- [ ] Browser categories (work/social/news/entertainment)
- [ ] Idle detection + break frequency
- [ ] Signal push to Firebase RTDB

#### WearOS Companion (Spec + Prototype)
- [ ] Continuous HR via HealthServices
- [ ] HRV (stress) via RMSSD values
- [ ] Sleep stages (Light/Deep/REM)
- [ ] Movement + sedentary alerts
- [ ] SPO2 (blood oxygen)
- [ ] Signal push to Firebase RTDB

#### Device Management UI
- [ ] Device management settings screen
- [ ] Add/remove devices
- [ ] Per-device signal toggle
- [ ] Signal fusion configuration

---

### PHASE 6: VOICE + CONTEXT-AWARE INTELLIGENCE

#### New Module: `:core:voice`
- [ ] `VoiceRecognitionManager.kt` — wraps SpeechRecognizer, exposes Flow<VoiceState>
- [ ] `VoiceCommandParser.kt` — parses recognized text → HabitLogCommand
- [ ] `VoiceChatManager.kt` — input (SpeechRecognizer) + output (TextToSpeech)

#### Voice Entry
- [ ] Fill existing `/* TODO: voice input via SpeechRecognizer */` in ConversationalEntrySection.kt
- [ ] Hold-to-talk mic button, releases to submit text
- [ ] Floating mic FAB on Home screen
- [ ] Multi-habit voice logging ("Logged 5km run and 30 minutes reading")
- [ ] Voice input feeds into existing Brain Dump AI pipeline (zero duplication)

#### Voice Coach
- [ ] Mic button in coach chat input row
- [ ] Speaker icon on AI response bubbles (tap to re-read via TTS)
- [ ] Voice → text → existing coach chat pipeline → AI response → TTS

#### New Module: `:core:context`
- [ ] `ContextSignalProvider.kt` — reads device state (charging, headphones, activity, time, network)
- [ ] `WeatherContextProvider.kt` — lightweight weather check via Open-Meteo (free, no key)

#### Context-Aware Notifications
- [ ] DRIVING detected → suppress gym/cycling/reading reminders
- [ ] Charging + Stationary + Evening → surface productivity habits
- [ ] Headphones connected → send music practice habits NOW
- [ ] Weekend + Morning → reduce urgency for work habits
- [ ] Applied in `StreakAlertWorker` and `HabitReminderWorker`

#### Weather-Aware Reminders
- [ ] For outdoor habits (outdoor_activity = true), suppress if precipitation likely
- [ ] Reschedule for +3 hours if weather blocks habit
- [ ] Weather cache: 3 hours in memory

#### Learned Optimal Timing
- [ ] Track "notification sent → habit completed within 1 hour" success rates by time slot
- [ ] After 2 weeks of data, write learned optimal hour per habit to DataStore
- [ ] Override manual reminder time with learned time

#### Duration Tracking Type
- [ ] Add `DURATION` to TrackingType enum
- [ ] `HabitTimerService.kt` — foreground service with persistent notification
- [ ] Timer notification actions: Pause / Stop & Log
- [ ] Voice-triggered: "Start meditation timer"

---

### PHASE 7: ADAPTIVE + SOCIAL + POLISH

#### Adaptive Difficulty
- [ ] `AdaptiveTargetUseCase.kt` — suggests target changes based on 7-day rolling performance
- [ ] Suggest INCREASE by 10% if completion > 90% AND avg > 115% of target
- [ ] Suggest DECREASE by 10% if completion < 40%
- [ ] Non-silent: adaptation card in insights feed with [Update] / [Not yet]
- [ ] Target history stored in `habit_target_history`

#### Habit Splitting
- [ ] Detect high-variance habits (some days 0, some days way over target)
- [ ] AI suggests splitting into baseline + stretch habit
- [ ] [Split habit] action navigates to CreateHabitScreen with pre-filled values

#### New Module: `:core:social`
- [ ] `SocialRepository.kt` — Firebase RTDB reads/writes for buddy connections
- [ ] `HabitBuddyEntity.kt` — local Room cache
- [ ] `BuddyInviteActivity.kt` — deep-link handler (verdant://buddy/[habitId]/[code])
- [ ] 6-digit shareable invite code per habit
- [ ] Streak-only updates (no values for NUMERIC — privacy-first)

#### Anonymous Streak Comparison
- [ ] Firebase Cloud Function for anonymized percentiles
- [ ] "87% of people tracking 'Exercise 30 min' have a shorter streak"
- [ ] Opt-in via `anonymousComparisonEnabled` DataStore key

#### Shared Element Transitions (Compose 1.7)
- [ ] Habit card in list → Habit detail (ring morphs/expands)
- [ ] Home insight card → Insights feed (card expands in-place)
- [ ] Contribution grid cell → Day detail (cell expands)

#### Milestone Celebrations
- [ ] `CelebrationOverlay.kt` — full-screen with Lottie confetti
- [ ] Personalized AI message at milestones (7, 30, 100, 365 days)
- [ ] "Your story so far" summary card
- [ ] `pendingCelebrations` DataStore key, checked on each app launch

#### Adaptive Home Screen States
- [ ] `Normal` — standard home layout
- [ ] `AllDoneEarly` — all habits done before noon
- [ ] `NothingDueToday` — no habits scheduled
- [ ] `SundayEvening` — auto-show week review
- [ ] `ReEngagement` — user hasn't opened in 2+ days
- [ ] `DangerZone` — 3+ habits at high risk

#### Export Completion
- [ ] Complete `ExportUseCase.kt` (already exists, incomplete)
- [ ] CSV export — one row per HabitEntry, all metadata
- [ ] JSON export — full Habit object with embedded entries (importable)
- [ ] Markdown export — Year in Review narrative format
- [ ] Share via `ShareCompat.IntentBuilder`

#### Google Sheets Sync (Optional)
- [ ] `GoogleSheetsSyncWorker` — weekly, appends new entries to spreadsheet
- [ ] Google Drive OAuth2 scope
- [ ] `googleSheetsEnabled` + `googleSheetId` DataStore keys

#### AI Interview Onboarding
- [ ] `AIInterviewScreen.kt` — 5-question conversational onboarding
- [ ] Goal, habit count preference, chronotype, experience, focus area
- [ ] Auto-create 3-5 suggested habits from answers
- [ ] `CloudAI.generatePersonalizedHabitPlan()`

#### Progressive Feature Disclosure
- [ ] `FeatureDiscoveryManager.kt` — gates features on usage milestones
- [ ] Day 1-3: BINARY only, no widgets/analytics
- [ ] Day 4-7: NUMERIC + first widget suggestion
- [ ] Week 2+: Analytics, adaptive targets
- [ ] Day 30+: Social, advanced AI insights

---

### PHASE 8: ROBUSTNESS

#### Logging & Observability
- [ ] Timber logging in all workers and `runCatching` blocks
- [ ] Firebase Crashlytics integration
- [ ] VerdantAnalytics event wrapper (habit_created, habit_completed, etc. — no PII)

#### CloudAI Circuit Breaker
- [ ] `CloudAICircuitBreaker.kt` — CLOSED / OPEN / HALF_OPEN states
- [ ] Opens after 3 consecutive failures within 1 hour
- [ ] Stays open 2 hours, then HALF_OPEN
- [ ] VerdantAIRouter routes to FallbackAI when OPEN

#### Offline-First AI
- [ ] Show stale AIInsightEntity from Room cache immediately on load
- [ ] Queue failed AI requests in `pending_ai_requests` table
- [ ] Retry on next WorkManager network-available run
- [ ] Apply NetworkConnected constraint to all CloudAI workers

#### Performance
- [ ] Paginate HabitEntryDao queries using Pager + PagingSource
- [ ] `streak_cache` table — cache computed streaks, invalidate on new entry
- [ ] Battery impact analysis — target < 5% daily drain from all Verdant workers

#### TrackingType Enum Cleanup
- [ ] Add `DURATION` — backed by HabitTimerService
- [ ] Add `FINANCIAL` — already partially supported in LogEntryUseCase
- [ ] Add `EMOTIONAL` — mood/wellbeing habits
- [ ] Add `EVENT_DRIVEN` — triggered by external event (geofence, HealthConnect)

---

## PRODUCT ARCHITECTURE

### How Everything Comes Together

```
USER'S DAY
    |
    |-- Phone: habits, steps, sleep, transactions, location, screen time
    |-- Mac: coding hours, meetings, focus sessions, browser patterns
    |-- Watch: continuous HR, HRV stress, movement, SPO2
    |
    v
SIGNAL FUSION (core:emotional)
    |
    |-- Raw --> Emotional Context (mood, energy, stress)
    |-- Raw --> Life Scores (vitality, discipline, wisdom, focus, resilience, awareness)
    |-- Raw --> Predictions (spending, habit sustainability, health trajectory)
    |-- Patterns --> Cross-correlations ("sleep --> spending" links)
    |
    v
THE SYSTEM (core:prediction + core:ai)
    |
    |-- Player Profile updated (XP, stats, level)
    |-- Quests generated from weak spots
    |-- Interventions triggered by state transitions
    |-- Life Forecast narrative generated
    |
    v
USER INTERFACE (feature:lifedashboard)
    |
    |-- Player card with level + rank
    |-- 6 stat dimensions with trends
    |-- Active quest progress
    |-- Emotional state banner
    |-- System insights + predictions
    |-- Life forecast narrative
    |
    v
USER EVOLVES
    |
    |-- "Arise." -- Level up notification
    |-- New title unlocked
    |-- New quest available
    |-- Pattern discovered
    |-- The cycle continues
```

### Emotional Engine Architecture

```
                     +-------------------------+
                     |    EMOTIONAL ENGINE      |
                     |  (core:emotional)        |
                     |                          |
  Raw Signals ------>|  Signal Fusion Layer     |------> Life Scores
  (all devices)      |  Pattern Memory          |------> Predictions
                     |  Behavioral Model        |------> Interventions
                     |  Emotional State Machine  |------> Emotional Map
                     +-------------------------+
```

### Multi-Device Sync Architecture

```
+----------+  +----------+  +----------+  +----------+
|  Phone   |  |   Mac    |  |  Watch   |  |  Tablet  |
| (master) |  | (work)   |  | (body)   |  | (rest)   |
+----+-----+  +----+-----+  +----+-----+  +----+-----+
     |             |             |             |
     +------+------+------+------+------+------+
            |             |             |
     +------v-------------v-------------v------+
     |         Firebase Realtime DB            |
     |    (encrypted sync -- signals only)     |
     +------------------+----------------------+
                        |
                 +------v------+
                 |   Phone     |
                 |  Emotional  |
                 |   Engine    |
                 +-------------+
```

### Player Leveling System

```
Level 1-5   | Rank E | "Awakened Novice"   | Basic tracking, daily motivation
Level 6-15  | Rank D | "Rising Hunter"     | Analytics, AI insights, adaptive targets
Level 16-30 | Rank C | "Shadow Striker"    | Health Connect, passive tracking, voice
Level 31-50 | Rank B | "Elite Commander"   | Multi-device sync, cross-correlations
Level 51-75 | Rank A | "Monarch's Will"    | Life forecast, emotional engine, quests
Level 76-100| Rank S | "Shadow Monarch"    | Full System access, mentor mode
```

### Emotional State Machine

```
              +------+
              | FLOW |<---------+
              +--+---+          |
                 |              |
    decline      |     recovery |
                 v              |
           +-----+------+  +---+------+
           |  PLATEAU   |  | RECOVERY |
           +-----+------+  +---+------+
                 |              ^
    continued    |              |
    decline      v    recovery  |
           +-----+------+------+
           |   DRIFT    |
           +-----+------+
                 |
    crash        v
           +-----+------+
           |  CRISIS    |
           +------------+

    At any point:
           +-----+------+
           |   SURGE    | (sudden spike in all metrics)
           +------------+
```

---

## DATA COLLECTION INVENTORY

### Phone (Android) — 15 Sources

| # | Source | API | Permission | Frequency | Status |
|---|--------|-----|-----------|-----------|--------|
| 1 | Habits (manual) | Room DB | None | User-driven | DONE |
| 2 | Transactions (SMS) | Content Provider | READ_SMS | 2h | DONE |
| 3 | Location (manual) | Fused Location | FINE_LOCATION | User-driven | DONE |
| 4 | Steps | HealthConnect | health.READ_STEPS | 3h | TODO |
| 5 | Sleep | HealthConnect | health.READ_SLEEP | 3h | TODO |
| 6 | Heart Rate | HealthConnect | health.READ_HEART_RATE | 3h | TODO |
| 7 | Exercise | HealthConnect | health.READ_EXERCISE | 3h | TODO |
| 8 | Weight | HealthConnect | health.READ_WEIGHT | 3h | TODO |
| 9 | Hydration | HealthConnect | health.READ_HYDRATION | 3h | TODO |
| 10 | Screen Time | UsageStatsManager | PACKAGE_USAGE_STATS | 6h | TODO |
| 11 | Activity | Activity Recognition | ACTIVITY_RECOGNITION | Continuous | TODO |
| 12 | Calendar | CalendarContract | READ_CALENDAR | 6h | TODO |
| 13 | Notifications | NotificationListenerService | System settings | Continuous | TODO |
| 14 | Battery | BatteryManager | None | 6h | TODO |
| 15 | Weather | OpenWeatherMap API | None (uses location) | 12h | TODO |

### Mac (Companion) — 6 Sources

| # | Source | API | Frequency | Status |
|---|--------|-----|-----------|--------|
| 1 | Active App Category | Accessibility / NSWorkspace | 5min | TODO |
| 2 | Focus Sessions | Screen lock + app switching | Events | TODO |
| 3 | Coding Time | VS Code / JetBrains extension | Commits | TODO |
| 4 | Meetings | Calendar API | Hourly | TODO |
| 5 | Browser Categories | Browser extension | 5min | TODO |
| 6 | Idle Time | CGEventSource | Minute-level | TODO |

### Watch (WearOS) — 5 Sources

| # | Source | API | Frequency | Status |
|---|--------|-----|-----------|--------|
| 1 | Continuous HR | HealthServices | 5min | TODO |
| 2 | HRV (Stress) | HealthServices | Hourly | TODO |
| 3 | Sleep Stages | HealthServices | On wake | TODO |
| 4 | Movement | Accelerometer | 10min | TODO |
| 5 | SPO2 | HealthServices | Nightly | TODO |

---

## BACKGROUND WORKERS — COMPLETE INVENTORY

| # | Worker | Interval | Status |
|---|--------|----------|--------|
| 1 | `DailyMotivationWorker` | 24h (8 AM) | DONE |
| 2 | `StreakAlertWorker` | 2h (4-10 PM gate) | DONE |
| 3 | `WeeklySummaryWorker` | Weekly (Sun 7 PM) | DONE |
| 4 | `SmsProcessingWorker` | 2h | DONE |
| 5 | `SpendingAlertWorker` | 6h | DONE |
| 6 | `HabitReminderWorker` | Per-alarm | DONE |
| 7 | `HealthSyncWorker` | 3h | TODO |
| 8 | `DeviceStatsSyncWorker` | 6h | TODO |
| 9 | `WeatherSyncWorker` | 12h | TODO |
| 10 | `RecurringTransactionDetectorWorker` | 24h | TODO |
| 11 | `EmotionalEngineWorker` | 6h | TODO |
| 12 | `LifeScoreComputeWorker` | 12h | TODO |
| 13 | `PredictionWorker` | 24h | TODO |
| 14 | `QuestGeneratorWorker` | 24h | TODO |
| 15 | `XPComputeWorker` | 6h | TODO |
| 16 | `PatternMiningWorker` | 7 days | TODO |
| 17 | `CrossDeviceSyncWorker` | 1h | TODO |
| 18 | `NotificationCounterService` | Continuous | TODO |
| 19 | `BudgetAlertWorker` | 6h | TODO |
| 20 | `GoogleSheetsSyncWorker` | Weekly | TODO |

---

## PRIVACY ARCHITECTURE

1. **All raw data stays on-device.** No screenshots, keystrokes, message contents, or call logs leave any device.
2. **Only anonymized aggregates to cloud AI** — controlled by existing `llmDataSharing` toggle.
3. **Each data source independently toggleable** — graceful degradation when disabled.
4. **Cross-device sync transmits signals, not data** — `{type: "focus_session", value: 45, unit: "min"}`, never app names or content.
5. **Emotional state inferences are always correctable** — user can override any mood inference.
6. **Data retention policy** — configurable per-table, default 1 year, with Data Audit screen.
7. **XP/level data is local-only** — no leaderboards unless user opts into anonymous comparison.

---

## MODULES — COMPLETE INVENTORY

| # | Module | Purpose | Status |
|---|--------|---------|--------|
| 1 | `:app` | Main entry, nav graph, Hilt | DONE |
| 2 | `:core:model` | Domain models | DONE (extending) |
| 3 | `:core:database` | Room DB, entities, DAOs, repos | DONE (extending) |
| 4 | `:core:datastore` | User preferences | DONE (extending) |
| 5 | `:core:network` | Retrofit + Firebase | DONE |
| 6 | `:core:ai` | AI interface, Cloud/Local/Fallback | DONE (extending) |
| 7 | `:core:common` | Aggregators, utilities | DONE (extending) |
| 8 | `:core:sms` | SMS parsing for finance | DONE |
| 9 | `:core:designsystem` | Compose UI components | DONE (extending) |
| 10 | `:feature:home` | Dashboard | DONE (extending) |
| 11 | `:feature:habits` | Habit CRUD + detail | DONE |
| 12 | `:feature:analytics` | Analytics tabs | DONE |
| 13 | `:feature:insights` | AI insights + coach | DONE (extending) |
| 14 | `:feature:finance` | Finance tracking | DONE (extending) |
| 15 | `:feature:settings` | Settings + onboarding | DONE (extending) |
| 16 | `:widget` | Glance widgets | DONE (extending) |
| 17 | `:work` | Background workers | DONE (extending) |
| 18 | `:core:health` | HealthConnect + Activity Recognition | TODO |
| 19 | `:core:devicestats` | UsageStats, Calendar, Battery | TODO |
| 20 | `:core:geofence` | Location geofencing | TODO |
| 21 | `:core:context` | Context signals, weather | TODO |
| 22 | `:core:voice` | Speech recognition + TTS | TODO |
| 23 | `:core:emotional` | Emotional Engine | TODO |
| 24 | `:core:prediction` | Statistical prediction models | TODO |
| 25 | `:core:social` | Buddy system | TODO |
| 26 | `:core:sync` | Multi-device signal sync | TODO |
| 27 | `:feature:lifedashboard` | Life Dashboard + Player System UI | TODO |

---

## SUMMARY STATS

| Metric | Done | Todo | Total |
|--------|------|------|-------|
| Modules | 17 | 10 | 27 |
| Database Tables | 6 | 19 | 25 |
| Background Workers | 6 | 14 | 20 |
| Data Sources (Phone) | 3 | 12 | 15 |
| Data Sources (Mac) | 0 | 6 | 6 |
| Data Sources (Watch) | 0 | 5 | 5 |
| Tracking Types | 3 | 4 | 7 |
| AI Methods | ~8 | ~4 | ~12 |
| Permissions | 7 | 10 | 17 |
