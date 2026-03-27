import Anthropic from '@anthropic-ai/sdk';
import {
  HabitDataPayload,
  HabitStackContext,
  InsightResponse,
  InsightType,
  ReportResponse,
  ReportType,
} from './types';

// ── Constants ─────────────────────────────────────────────────────────────────

/**
 * Haiku is used for most calls — fast and cost-efficient for habit-sized payloads.
 * Upgrade individual calls to claude-sonnet-4-6 here if richer output is needed.
 */
const CLAUDE_MODEL = 'claude-haiku-4-5-20251001';

// ── System prompts ────────────────────────────────────────────────────────────

/**
 * Motivational Interviewing (MI) tone guidelines applied to ALL system prompts:
 *  - Validate the user's experience first; normalise setbacks
 *  - No guilt, shame, or streak-broken language
 *  - Frame data as neutral information, not a moral scorecard
 *  - Use open-ended, evocative questions in chat; affirmations in messages
 *  - Celebrate effort and direction, not just outcomes
 */
const MI_TONE_RULES = `\
Tone rules (Motivational Interviewing):
- Validate before advising. Normalise hard days.
- Never use guilt, shame, or "broken streak" framing.
- Frame numbers as neutral information, not judgement.
- Celebrate direction and effort, not just outcomes.
- Use warm, conversational language — not corporate wellness-speak.`;

const INSIGHT_SYSTEM_PROMPT = `You are Verdant, a supportive habit companion grounded in behavioural science. \
You receive structured data about a user's habits. \
Be specific (reference actual numbers), warm, and never preachy. \
Celebrate consistency and small wins — not perfection. \
Keep messages under 200 characters for notifications, longer for reports and chat.

${MI_TONE_RULES}`;

const REPORT_SYSTEM_PROMPT = `You are Verdant, a behavioural analytics companion. \
Analyse the provided habit data and generate comprehensive, actionable insights. \
Reference specific numbers and trends. \
Respond ONLY with a valid JSON object matching the schema provided in the user message.

${MI_TONE_RULES}`;

const COACH_SYSTEM_PROMPT = `You are Verdant, an AI habit coach trained in behavioural science, \
specifically the COM-B model (Capability, Opportunity, Motivation) and Motivational Interviewing (MI).

When a user shares a struggle or you detect declining performance, silently diagnose which \
COM-B pillar is the likely bottleneck, then offer a targeted response:
  • Capability gap → practical how-to, simplify the habit, reduce friction
  • Opportunity gap → environmental tweaks, scheduling, cue design
  • Motivation gap → explore ambivalence with open questions, link to values, validate emotions

${MI_TONE_RULES}

Keep responses conversational (2–4 sentences max). \
Ask one open question at the end when appropriate. \
Never diagnose out loud — just respond helpfully from the right angle.`;

// ── generateInsight ───────────────────────────────────────────────────────────

export async function generateInsightFromClaude(
  type: InsightType,
  habitData: HabitDataPayload,
  message?: string,
  stackContext?: HabitStackContext,
): Promise<InsightResponse> {
  const client = new Anthropic(); // reads ANTHROPIC_API_KEY from env

  const systemPrompt = type === 'coach_reply' ? COACH_SYSTEM_PROMPT : INSIGHT_SYSTEM_PROMPT;
  const maxTokens = type === 'coach_reply' ? 500 : type === 'weekly_behavioral_synthesis' ? 350 : 200;

  const userPrompt = buildInsightPrompt(type, habitData, message, stackContext);

  const completion = await client.messages.create({
    model: CLAUDE_MODEL,
    max_tokens: maxTokens,
    system: systemPrompt,
    messages: [{ role: 'user', content: userPrompt }],
  });

  const content = extractText(completion.content);

  return {
    content: content.trim(),
    relatedHabitIds: findMentionedHabitIds(content, habitData, type),
    confidence: computeConfidence(habitData),
  };
}

// ── generateReport ────────────────────────────────────────────────────────────

export async function generateReportFromClaude(
  type: ReportType,
  habitData: HabitDataPayload,
): Promise<ReportResponse> {
  const client = new Anthropic();

  const userPrompt = buildReportPrompt(type, habitData);

  const completion = await client.messages.create({
    model: CLAUDE_MODEL,
    max_tokens: type === 'monthly' ? 1200 : 800,
    system: REPORT_SYSTEM_PROMPT,
    messages: [{ role: 'user', content: userPrompt }],
  });

  const raw = extractText(completion.content);

  return parseReportJson(raw);
}

// ── Prompt builders ───────────────────────────────────────────────────────────

function buildInsightPrompt(
  type: InsightType,
  data: HabitDataPayload,
  message?: string,
  stackContext?: HabitStackContext,
): string {
  const habitLines = data.habits
    .map((h) => {
      const pct = Math.round(h.completionRate * 100);
      const val = h.avgValue !== undefined ? `, avg ${h.avgValue} ${h.unit ?? ''}` : '';
      const stressNote =
        h.avgStressOnMiss !== undefined ? `, stress on miss: ${h.avgStressOnMiss.toFixed(1)}/10` : '';
      const energyNote =
        h.avgEnergyOnComplete !== undefined
          ? `, energy on complete: ${h.avgEnergyOnComplete.toFixed(1)}/10`
          : '';
      const missNote = h.topMissedReason ? `, common miss reason: "${h.topMissedReason}"` : '';
      return `  • ${h.icon} ${h.name}: ${pct}% completion, ${h.currentStreak}d streak${val}${stressNote}${energyNote}${missNote}`;
    })
    .join('\n');

  const overview = [
    `Today: ${Math.round(data.overallCompletionToday * 100)}%`,
    `This week: ${Math.round(data.overallCompletionThisWeek * 100)}%`,
    `This month: ${Math.round(data.overallCompletionThisMonth * 100)}%`,
  ].join('  |  ');

  const context = `${overview}\n\nHabits (${data.periodDays}d window):\n${habitLines}`;

  switch (type) {
    case 'daily_motivation':
      return `Give a short, warm motivational message for today (max 150 chars). \
Reference a specific habit or stat. Do not use streak-broken framing if completion is low — \
validate the effort so far and encourage the next small step.\n\n${context}`;

    case 'pattern':
      return `Identify the single most significant behavioural pattern in these habits. \
State it as a neutral observation, not a judgement.\n\n${context}`;

    case 'correlation':
      return `Find one meaningful positive or negative correlation between any two habits. \
Frame it as a curiosity to explore, not a problem to fix.\n\n${context}`;

    case 'weekly_summary':
      return `Summarise this week's habit performance in 1–2 warm sentences. \
Reference a specific number, acknowledge any harder days, and name one direction to build on.\n\n${context}`;

    case 'monthly_summary':
      return `Summarise this month's habit performance in 2–3 sentences. \
Highlight overall direction and one notable win. Validate any inconsistency neutrally.\n\n${context}`;

    case 'suggestion':
      return `Give one specific, actionable suggestion to improve habit adherence. \
Diagnose the likely barrier first (is it a knowledge/skill gap, an environment/scheduling gap, \
or a motivation/emotion gap?) then suggest the appropriate intervention. \
Frame it as a curious experiment, not a prescription.\n\n${context}`;

    case 'coach_reply': {
      // Build COM-B diagnostic hint from context signals for the coach
      const combHints = buildCombDiagnosticHints(data);
      return (
        `User message: "${message ?? ''}"\n\n` +
        `Context:\n${context}\n\n` +
        (combHints ? `Behavioural signals:\n${combHints}\n\n` : '') +
        `Respond as Verdant — warm, non-judgmental, behaviourally informed. ` +
        `If the user is struggling, address the most likely COM-B bottleneck. ` +
        `Be specific to their data. End with one open question when helpful.`
      );
    }

    case 'habit_stack': {
      if (!stackContext) {
        return `Suggest one habit-stacking opportunity based on the highest-consistency habit.\n\n${context}`;
      }
      const anchorPct = Math.round(stackContext.anchorCompletionRate * 100);
      const targetPct = Math.round(stackContext.targetCompletionRate * 100);
      const timeHint = stackContext.anchorConsistentTime
        ? ` usually around ${stackContext.anchorConsistentTime}`
        : '';

      return (
        `Write a Fogg habit-stack formula for this user.\n\n` +
        `Anchor habit: ${stackContext.anchorHabitIcon} ${stackContext.anchorHabitName} ` +
        `(${anchorPct}% consistency${timeHint})\n` +
        `Target habit: ${stackContext.targetHabitIcon} ${stackContext.targetHabitName} ` +
        `(${targetPct}% consistency — new or struggling)\n\n` +
        `Write exactly this format in 1–2 warm sentences:\n` +
        `"After I [anchor], I will [target] for just [tiny duration]." ` +
        `Then add one sentence explaining why this pairing makes sense behaviourally. ` +
        `Keep the total under 200 characters.\n\n` +
        `Broader context:\n${context}`
      );
    }

    case 'weekly_behavioral_synthesis': {
      return buildBehavioralSynthesisPrompt(data, context);
    }
  }
}

/**
 * Builds COM-B diagnostic hints by inspecting habit data signals.
 * Returns a bullet list the coach prompt can reference, or empty string if nothing notable.
 */
function buildCombDiagnosticHints(data: HabitDataPayload): string {
  const hints: string[] = [];

  // Low overall completion may indicate motivation or opportunity barriers
  const weekPct = Math.round(data.overallCompletionThisWeek * 100);
  if (weekPct < 40) {
    hints.push(
      `• Low weekly completion (${weekPct}%) — possible Motivation or Opportunity barrier`,
    );
  }

  // High stress on miss → likely Motivation (emotional override) barrier
  const highStressHabits = data.habits.filter(
    (h) => h.avgStressOnMiss !== undefined && h.avgStressOnMiss >= 7,
  );
  if (highStressHabits.length > 0) {
    const names = highStressHabits.map((h) => h.name).join(', ');
    hints.push(
      `• High stress on miss days for: ${names} → likely Motivation/emotional override barrier`,
    );
  }

  // Low energy on complete → may indicate physical Capability barrier
  const lowEnergyHabits = data.habits.filter(
    (h) => h.avgEnergyOnComplete !== undefined && h.avgEnergyOnComplete <= 4,
  );
  if (lowEnergyHabits.length > 0) {
    const names = lowEnergyHabits.map((h) => h.name).join(', ');
    hints.push(
      `• Low energy even on completion days for: ${names} → possible physical Capability barrier`,
    );
  }

  // Missed reason mentions → Capability gap signals
  const capabilityKeywords = ['forgot', 'didn\'t know', 'not sure how', 'confused'];
  const capabilityHabits = data.habits.filter(
    (h) =>
      h.topMissedReason &&
      capabilityKeywords.some((kw) => h.topMissedReason!.toLowerCase().includes(kw)),
  );
  if (capabilityHabits.length > 0) {
    const names = capabilityHabits.map((h) => h.name).join(', ');
    hints.push(`• "Forgot" / "not sure how" miss reasons for: ${names} → Capability gap`);
  }

  // Opportunity gap signals from missed reasons
  const opportunityKeywords = ['no time', 'busy', 'travelling', 'work', 'schedule'];
  const opportunityHabits = data.habits.filter(
    (h) =>
      h.topMissedReason &&
      opportunityKeywords.some((kw) => h.topMissedReason!.toLowerCase().includes(kw)),
  );
  if (opportunityHabits.length > 0) {
    const names = opportunityHabits.map((h) => h.name).join(', ');
    hints.push(
      `• Time/schedule miss reasons for: ${names} → Opportunity gap (environment/scheduling)`,
    );
  }

  return hints.join('\n');
}

/**
 * Builds the weekly behavioral synthesis prompt.
 * Looks for cross-domain correlations (e.g. "skipping supplements + high stress → cycling drops").
 */
function buildBehavioralSynthesisPrompt(data: HabitDataPayload, context: string): string {
  // Surface habits with context data for richer synthesis
  const habitsWithContext = data.habits.filter(
    (h) =>
      h.avgStressOnMiss !== undefined ||
      h.avgEnergyOnComplete !== undefined ||
      h.topMissedReason !== undefined,
  );

  const contextSection =
    habitsWithContext.length > 0
      ? `\nContextual signals available for cross-domain analysis:\n` +
        habitsWithContext
          .map((h) => {
            const parts = [];
            if (h.avgStressOnMiss !== undefined)
              parts.push(`stress on miss: ${h.avgStressOnMiss.toFixed(1)}/10`);
            if (h.avgEnergyOnComplete !== undefined)
              parts.push(`energy on complete: ${h.avgEnergyOnComplete.toFixed(1)}/10`);
            if (h.topMissedReason) parts.push(`miss reason: "${h.topMissedReason}"`);
            return `  • ${h.icon} ${h.name}: ${parts.join(', ')}`;
          })
          .join('\n')
      : '';

  return (
    `Analyse this week's cross-domain habit data and write ONE specific behavioural insight.\n\n` +
    `Look for patterns like:\n` +
    `  - "On days you miss [habit A], [habit B] drops by X%"\n` +
    `  - "Your [habit A] and [habit B] tend to succeed together — completing one predicts the other"\n` +
    `  - "High-stress days correlate with skipping [habit A] AND [habit B]"\n\n` +
    `Format: 1–2 warm, specific sentences framed as a curious observation (not a warning). ` +
    `Reference actual percentages. Max 250 characters.\n\n` +
    context +
    contextSection
  );
}

function buildReportPrompt(type: ReportType, data: HabitDataPayload): string {
  const period = type === 'weekly' ? '7 days' : '30 days';
  const completionKey =
    type === 'weekly' ? data.overallCompletionThisWeek : data.overallCompletionThisMonth;

  const habitLines = data.habits
    .map((h) => {
      const pct = Math.round(h.completionRate * 100);
      const val = h.avgValue !== undefined ? `, avg: ${h.avgValue} ${h.unit ?? ''}` : '';
      const missNote = h.topMissedReason ? `, miss reason: "${h.topMissedReason}"` : '';
      return `  ${h.icon} ${h.name}: ${pct}% (${h.currentStreak}d streak)${val}${missNote}`;
    })
    .join('\n');

  const topStreak = data.topStreaks[0];

  const weeklyRows =
    data.weeklyBreakdown
      ?.map((d) => `  ${d.date}: ${Math.round(d.completionRate * 100)}%`)
      .join('\n') ?? '';

  return [
    `Generate a ${period} habit report with MI tone (no shame, validate hard days, celebrate direction).`,
    `Return ONLY this JSON:`,
    `{"summary":"...","patterns":["...","..."],"suggestions":["...","..."],"highlights":["...","..."]}`,
    ``,
    `Stats (${period}):`,
    `- Overall completion: ${Math.round(completionKey * 100)}%`,
    `- Best streak: ${topStreak?.habitName ?? 'none'} (${topStreak?.currentStreak ?? 0} days)`,
    `- Tracked habits: ${data.habits.length}`,
    ``,
    `Per-habit:`,
    habitLines,
    weeklyRows ? `\nDaily breakdown:\n${weeklyRows}` : '',
    ``,
    `Rules: 2–3 items per array. Reference actual numbers. Patterns/suggestions use COM-B framing where relevant. No markdown in strings.`,
  ]
    .filter(Boolean)
    .join('\n');
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function extractText(content: Anthropic.ContentBlock[]): string {
  return content
    .filter((b): b is Anthropic.TextBlock => b.type === 'text')
    .map((b) => b.text)
    .join('');
}

function findMentionedHabitIds(
  content: string,
  data: HabitDataPayload,
  type: InsightType,
): string[] {
  if (type === 'daily_motivation' || type === 'coach_reply') return [];
  const lower = content.toLowerCase();
  return data.habits.filter((h) => lower.includes(h.name.toLowerCase())).map((h) => h.id);
}

function computeConfidence(data: HabitDataPayload): number {
  const dataPoints = data.habits.length * data.periodDays;
  if (dataPoints >= 70) return 0.92;
  if (dataPoints >= 30) return 0.78;
  if (dataPoints >= 14) return 0.63;
  return 0.5;
}

function parseReportJson(raw: string): ReportResponse {
  try {
    const match = raw.match(/\{[\s\S]*\}/);
    if (!match) throw new Error('No JSON found');
    const parsed = JSON.parse(match[0]) as Partial<ReportResponse>;
    return {
      summary: parsed.summary ?? raw.trim(),
      patterns: Array.isArray(parsed.patterns) ? parsed.patterns : [],
      suggestions: Array.isArray(parsed.suggestions) ? parsed.suggestions : [],
      highlights: Array.isArray(parsed.highlights) ? parsed.highlights : [],
    };
  } catch {
    return { summary: raw.trim(), patterns: [], suggestions: [], highlights: [] };
  }
}
