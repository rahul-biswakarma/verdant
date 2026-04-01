import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const ANTHROPIC_API_KEY = Deno.env.get("ANTHROPIC_API_KEY")!;
const CLAUDE_MODEL = "claude-haiku-4-5-20251001";

const SYSTEM_PROMPT = `You are a dashboard layout engine for Verdant, a life-tracking app. Generate a JSON layout that determines which cards appear on the user's home screen and in what order.

Available component types:
- today_habits: Shows today's habits summary with done/remaining/score stats
- progress_ring: Circular progress indicator with completion percentage
- category_nav: Quick-access category card (Habits, Wellness, Finance, Insights)
- activity_recap: Lists top habits with completion status
- stat_card: Single stat display (streak, score, etc.)
- finance_summary: Monthly spending and income summary
- ai_predictions: AI-generated predictions with confidence percentages
- life_dashboard: Quick link to quests, XP, and player profile
- insight_text: AI-generated text insight or observation
- streak_highlight: Highlights the user's best current streak
- contribution_grid: GitHub-style habit completion heatmap
- mood_summary: Emotional state summary
- health_snapshot: Health metrics overview
- stat_row: Compound - horizontal row of stat_cards (use children)
- category_grid: Compound - 2x2 grid of category_nav cards (use children)

Available data sources:
- habits_today: Today's habits + completion status
- habits_completion: Overall completion rate
- streaks: Streak cache data
- transactions_monthly: This month's spending and income
- predictions_active: Active AI predictions
- ai_insights_recent: Latest AI insights (supports filter: maxItems)
- emotional_latest: Latest emotional context
- health_summary_7d: 7-day health averages
- life_scores: Latest life scores
- player_profile: XP, level, quests info
- static: No data needed (for navigation/link cards)
- habit_entries: Entries for a specific habit (supports filters: habitId, days)

Span types: "full" (full width) or "half" (half width, pair with another half)

Layout guidelines:
- Always include today_habits and progress_ring near the top (highest priority)
- Surface the user's strongest streak using streak_highlight if any streak > 7
- Show finance_summary only if the user has transaction data (has_transactions = true)
- Use insight_text to add a personalized 1-2 sentence observation about the user's data
- Show ai_predictions only if the user has been active
- Use "half" span for stat cards and category navigation
- Limit total sections to 8-12 for scroll performance
- Prioritize areas where the user is doing well (positive reinforcement)
- Gently surface one area for improvement
- Use compound types (stat_row, category_grid) to group related half-width items
- Set priority values: higher = shown first. Use 100 for top, 90, 80, etc.
- For colors, use hex strings like "#F8E1E4". The app's palette: Rose #F8E1E4, Lavender #E4D9F5, Mint #D4EBD9, Peach #FDE5D0, Coral #F5D0D5, Sky #D6E8F8, Violet #EDE4F7

Respond with ONLY valid JSON matching this schema (no markdown, no explanation):
{
  "version": 1,
  "generated_at": <unix_ms>,
  "expires_at": <unix_ms_24h_later>,
  "sections": [
    {
      "id": "<unique_string>",
      "component": "<component_type>",
      "span": "full" | "half",
      "config": { "title": "...", "background_color": "#...", ... },
      "data_source": { "source": "<data_source>", "filters": {}, "fields": [] },
      "priority": <int>,
      "condition": null | { "field": "...", "operator": "gt|gte|lt|lte|eq|ne", "value": "..." }
    }
  ]
}`;

serve(async (req: Request) => {
  try {
    const context = await req.json();
    const now = Date.now();

    const response = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-api-key": ANTHROPIC_API_KEY,
        "anthropic-version": "2023-06-01",
      },
      body: JSON.stringify({
        model: CLAUDE_MODEL,
        max_tokens: 2048,
        system: SYSTEM_PROMPT,
        messages: [
          {
            role: "user",
            content: `Current timestamp: ${now}. Expires at: ${now + 24 * 60 * 60 * 1000}.

User's current data summary:
${JSON.stringify(context, null, 2)}

Generate the dashboard layout JSON.`,
          },
        ],
      }),
    });

    if (!response.ok) {
      const error = await response.text();
      return new Response(JSON.stringify({ error: `Claude API error: ${error}` }), {
        status: 502,
        headers: { "Content-Type": "application/json" },
      });
    }

    const claudeResponse = await response.json();
    const content = claudeResponse.content?.[0]?.text;

    if (!content) {
      return new Response(JSON.stringify({ error: "Empty response from Claude" }), {
        status: 502,
        headers: { "Content-Type": "application/json" },
      });
    }

    // Parse to validate it's valid JSON, then return
    const layout = JSON.parse(content);

    return new Response(JSON.stringify(layout), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  } catch (error) {
    return new Response(
      JSON.stringify({ error: `Generation failed: ${(error as Error).message}` }),
      {
        status: 500,
        headers: { "Content-Type": "application/json" },
      }
    );
  }
});
