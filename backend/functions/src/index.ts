/**
 * Verdant AI – Firebase Functions backend proxy
 *
 * Exposes two HTTPS endpoints that act as a secure proxy between the Android app
 * and the Anthropic Claude API:
 *
 *  POST /generateInsight  – Short AI insight (motivation, pattern, nudge, chat)
 *  POST /generateReport   – Structured weekly / monthly report
 *
 * All requests must carry a valid Firebase Auth ID token in the Authorization header.
 * Rate limits are enforced per-user per-day via Firestore counters.
 *
 * Deploy:
 *   firebase functions:secrets:set ANTHROPIC_API_KEY
 *   firebase deploy --only functions
 */

import * as admin from 'firebase-admin';
import { onRequest } from 'firebase-functions/v2/https';
import { AuthError, verifyAuthToken } from './auth';
import { generateInsightFromClaude, generateReportFromClaude } from './claude';
import { RateLimitError, checkAndIncrementRateLimit } from './rateLimit';
import { InsightRequest, ReportRequest } from './types';

admin.initializeApp();

// ── generateInsight ───────────────────────────────────────────────────────────

export const generateInsight = onRequest(
  {
    region: 'us-central1',
    cors: true, // Allow all origins; Android sends Firebase Auth token for auth
    secrets: ['ANTHROPIC_API_KEY'],
    timeoutSeconds: 60,
    minInstances: 0,
  },
  async (req, res) => {
    if (req.method !== 'POST') {
      res.status(405).json({ error: 'Method not allowed' });
      return;
    }

    try {
      // ── Auth ──────────────────────────────────────────────────────────────
      const userId = await verifyAuthToken(req);

      // ── Validate body ─────────────────────────────────────────────────────
      const body = req.body as InsightRequest;
      if (!body?.type || !body?.habitData) {
        res.status(400).json({
          error: 'Request body must contain: type, habitData',
        });
        return;
      }

      const validTypes = [
        'daily_motivation',
        'pattern',
        'correlation',
        'weekly_summary',
        'monthly_summary',
        'suggestion',
        'coach_reply',
        'habit_stack',
        'weekly_behavioral_synthesis',
      ];
      if (!validTypes.includes(body.type)) {
        res.status(400).json({ error: `Unknown insight type: ${body.type}` });
        return;
      }

      // ── Rate limit ────────────────────────────────────────────────────────
      await checkAndIncrementRateLimit(userId, body.type);

      // ── Call Claude ───────────────────────────────────────────────────────
      const result = await generateInsightFromClaude(
        body.type,
        body.habitData,
        body.message,
        body.stackContext,
      );

      res.status(200).json(result);
    } catch (err) {
      handleError(err, res);
    }
  },
);

// ── generateReport ────────────────────────────────────────────────────────────

export const generateReport = onRequest(
  {
    region: 'us-central1',
    cors: true,
    secrets: ['ANTHROPIC_API_KEY'],
    timeoutSeconds: 90, // Reports need longer inference time
    minInstances: 0,
  },
  async (req, res) => {
    if (req.method !== 'POST') {
      res.status(405).json({ error: 'Method not allowed' });
      return;
    }

    try {
      // ── Auth ──────────────────────────────────────────────────────────────
      const userId = await verifyAuthToken(req);

      // ── Validate body ─────────────────────────────────────────────────────
      const body = req.body as ReportRequest;
      if (!body?.type || !body?.habitData) {
        res.status(400).json({
          error: 'Request body must contain: type, habitData',
        });
        return;
      }

      if (body.type !== 'weekly' && body.type !== 'monthly') {
        res.status(400).json({ error: `Report type must be "weekly" or "monthly"` });
        return;
      }

      // Reports share the insight rate-limit bucket
      const insightType = body.type === 'weekly' ? 'weekly_summary' : 'monthly_summary';
      await checkAndIncrementRateLimit(userId, insightType);

      // ── Call Claude ───────────────────────────────────────────────────────
      const result = await generateReportFromClaude(body.type, body.habitData);

      res.status(200).json(result);
    } catch (err) {
      handleError(err, res);
    }
  },
);

// ── Error handler ─────────────────────────────────────────────────────────────

function handleError(err: unknown, res: import('firebase-functions/v2/https').Response): void {
  if (err instanceof RateLimitError) {
    res.status(429).json({
      error: err.message,
      code: err.code,
      limit: err.limit,
    });
  } else if (err instanceof AuthError) {
    res.status(401).json({
      error: err.message,
      code: err.code,
    });
  } else {
    const message = err instanceof Error ? err.message : 'Unknown error';
    console.error('[generateInsight]', message, err);
    res.status(500).json({ error: 'Internal server error' });
  }
}
