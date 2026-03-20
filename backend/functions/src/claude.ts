import Anthropic from '@anthropic-ai/sdk';
import { HabitDataPayload, InsightResponse, InsightType, ReportResponse, ReportType } from './types';

// ── Constants ─────────────────────────────────────────────────────────────────

/**
 * Haiku is used for all calls — fast and cost-efficient for habit-sized payloads.
 * Upgrade individual calls to claude-sonnet-4-6 here if richer output is needed.
 */
const CLAUDE_MODEL = 'claude-haiku-4-5-20251001';

const INSIGHT_SYSTEM_PROMPT = `You are Verdant AI, an encouraging habit coach. \
You receive structured data about a user's habits. \
Be specific (reference actual numbers), warm, never preachy. \
Celebrate consistency, not perfection. \
Keep messages under 200 characters for notifications, longer for reports/chat.`;

const REPORT_SYSTEM_PROMPT = `You are Verdant AI, a detailed habit analytics assistant. \
Analyze the provided habit data and generate comprehensive, actionable insights. \
Reference specific numbers and trends. \
Respond ONLY with a valid JSON object matching the schema provided in the user message.`;

// ── generateInsight ───────────────────────────────────────────────────────────

export async function generateInsightFromClaude(
  type: InsightType,
  habitData: HabitDataPayload,
  message?: string,
): Promise<InsightResponse> {
  const client = new Anthropic(); // reads ANTHROPIC_API_KEY from env

  const userPrompt = buildInsightPrompt(type, habitData, message);

  const completion = await client.messages.create({
    model: CLAUDE_MODEL,
    max_tokens: type === 'coach_reply' ? 500 : 200,
    system: INSIGHT_SYSTEM_PROMPT,
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
): string {
  const habitLines = data.habits
    .map((h) => {
      const pct = Math.round(h.completionRate * 100);
      const val = h.avgValue !== undefined ? `, avg ${h.avgValue} ${h.unit ?? ''}` : '';
      return `  • ${h.icon} ${h.name}: ${pct}% completion, ${h.currentStreak}d streak${val}`;
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
      return `Give a short motivational message for today (max 150 chars).\n\n${context}`;

    case 'pattern':
      return `Identify the single most significant pattern in these habits.\n\n${context}`;

    case 'correlation':
      return `Find a meaningful positive or negative correlation between any two habits.\n\n${context}`;

    case 'weekly_summary':
      return `Summarise this week's habit performance in 1–2 sentences.\n\n${context}`;

    case 'monthly_summary':
      return `Summarise this month's habit performance in 2–3 sentences.\n\n${context}`;

    case 'suggestion':
      return `Give one specific, actionable suggestion to improve habit adherence.\n\n${context}`;

    case 'coach_reply':
      return (
        `User message: "${message ?? ''}"\n\n` +
        `Context:\n${context}\n\n` +
        `Respond as a warm, knowledgeable habit coach. Be conversational and specific.`
      );
  }
}

function buildReportPrompt(type: ReportType, data: HabitDataPayload): string {
  const period = type === 'weekly' ? '7 days' : '30 days';
  const completionKey = type === 'weekly' ? data.overallCompletionThisWeek : data.overallCompletionThisMonth;

  const habitLines = data.habits
    .map((h) => {
      const pct = Math.round(h.completionRate * 100);
      const val = h.avgValue !== undefined ? `, avg: ${h.avgValue} ${h.unit ?? ''}` : '';
      return `  ${h.icon} ${h.name}: ${pct}% (${h.currentStreak}d streak)${val}`;
    })
    .join('\n');

  const topStreak = data.topStreaks[0];

  const weeklyRows =
    data.weeklyBreakdown
      ?.map((d) => `  ${d.date}: ${Math.round(d.completionRate * 100)}%`)
      .join('\n') ?? '';

  return [
    `Generate a ${period} habit report. Return ONLY this JSON:`,
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
    `Rules: 2–3 items per array, reference actual numbers, no markdown in strings.`,
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
