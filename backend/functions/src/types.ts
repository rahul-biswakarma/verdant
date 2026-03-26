// ── Insight types ─────────────────────────────────────────────────────────────

export type InsightType =
  | 'daily_motivation'
  | 'pattern'
  | 'correlation'
  | 'weekly_summary'
  | 'monthly_summary'
  | 'suggestion'
  | 'coach_reply'
  | 'habit_stack'
  | 'weekly_behavioral_synthesis';

export type ReportType = 'weekly' | 'monthly';

// ── Request / Response shapes ─────────────────────────────────────────────────

export interface InsightRequest {
  type: InsightType;
  habitData: HabitDataPayload;
  /** Only used for coach_reply type */
  message?: string;
  /** Only used for habit_stack type */
  stackContext?: HabitStackContext;
}

export interface InsightResponse {
  content: string;
  relatedHabitIds: string[];
  confidence: number;
}

export interface ReportRequest {
  type: ReportType;
  habitData: HabitDataPayload;
}

export interface ReportResponse {
  summary: string;
  patterns: string[];
  suggestions: string[];
  highlights: string[];
}

// ── Compact habit data payload ────────────────────────────────────────────────
// Produced by HabitDataAggregator on Android. Designed to stay under 500 tokens
// for daily insights and under 1000 tokens for weekly/monthly reports.

export interface HabitDataPayload {
  /** One entry per active habit */
  habits: HabitSummaryItem[];
  /** 0–1 fraction of today's scheduled habits completed */
  overallCompletionToday: number;
  /** 0–1 fraction of this week's scheduled habits completed */
  overallCompletionThisWeek: number;
  /** 0–1 fraction of this month's scheduled habits completed */
  overallCompletionThisMonth: number;
  /** Habits with active streaks, descending by streak length */
  topStreaks: StreakItem[];
  /** Number of days of history included in this payload */
  periodDays: number;
  /** Optional per-day breakdown for report payloads */
  weeklyBreakdown?: DailyCompletion[];
}

export interface HabitSummaryItem {
  id: string;
  name: string;
  icon: string;
  /** BINARY | QUANTITATIVE | DURATION | LOCATION | FINANCIAL */
  trackingType: string;
  /** 0–1 completion rate over periodDays */
  completionRate: number;
  currentStreak: number;
  /** Mean value for QUANTITATIVE/DURATION/FINANCIAL habits */
  avgValue?: number;
  unit?: string;
  /** Average self-reported stress level on days this habit was missed (1–10; from Phase 1 context data) */
  avgStressOnMiss?: number;
  /** Average self-reported energy level on days this habit was completed (1–10; from Phase 1 context data) */
  avgEnergyOnComplete?: number;
  /** Most common reason the user reported for missing this habit */
  topMissedReason?: string;
}

export interface StreakItem {
  habitId: string;
  habitName: string;
  currentStreak: number;
}

export interface DailyCompletion {
  /** ISO date YYYY-MM-DD */
  date: string;
  completionRate: number;
}

// ── Habit Stack context (used with habit_stack InsightType) ───────────────────

export interface HabitStackContext {
  /** The anchor habit (high consistency — used as the "cue") */
  anchorHabitId: string;
  anchorHabitName: string;
  anchorHabitIcon: string;
  anchorCompletionRate: number;
  /** Typical time-of-day the user completes the anchor habit (HH:mm), if known */
  anchorConsistentTime?: string;
  /** The target habit (new or struggling) to stack onto the anchor */
  targetHabitId: string;
  targetHabitName: string;
  targetHabitIcon: string;
  targetCompletionRate: number;
}
