import * as admin from 'firebase-admin';
import { InsightType } from './types';

// ── Limits ────────────────────────────────────────────────────────────────────

/** Max daily calls for all non-coach insight types combined */
const INSIGHT_DAILY_LIMIT = 5;

/** Max daily calls for coach_reply type */
const COACH_REPLY_DAILY_LIMIT = 10;

// ── Firestore path ────────────────────────────────────────────────────────────
// Structure: rateLimits/{userId}/daily/{YYYY-MM-DD}
//   Fields: insights (number), coachReplies (number)

function todayKey(): string {
  return new Date().toISOString().split('T')[0]; // YYYY-MM-DD UTC
}

function categoryFor(type: InsightType): 'insights' | 'coachReplies' {
  return type === 'coach_reply' ? 'coachReplies' : 'insights';
}

function limitFor(type: InsightType): number {
  return type === 'coach_reply' ? COACH_REPLY_DAILY_LIMIT : INSIGHT_DAILY_LIMIT;
}

// ── Public API ────────────────────────────────────────────────────────────────

/**
 * Atomically checks the rate limit for a user+type and increments the counter.
 * Throws [RateLimitError] if the daily limit has been reached.
 *
 * Uses a Firestore transaction so concurrent requests are handled safely.
 */
export async function checkAndIncrementRateLimit(
  userId: string,
  type: InsightType,
): Promise<void> {
  const db = admin.firestore();
  const today = todayKey();
  const category = categoryFor(type);
  const limit = limitFor(type);

  const docRef = db.doc(`rateLimits/${userId}/daily/${today}`);

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(docRef);
    const data = snap.data() ?? {};
    const current = (data[category] as number | undefined) ?? 0;

    if (current >= limit) {
      throw new RateLimitError(
        `Daily ${category === 'coachReplies' ? 'coach reply' : 'insight'} limit of ${limit} reached. ` +
          `Try again tomorrow.`,
        limit,
      );
    }

    tx.set(docRef, { [category]: current + 1 }, { merge: true });
  });
}

// ── Error class ───────────────────────────────────────────────────────────────

export class RateLimitError extends Error {
  readonly code = 'RATE_LIMIT_EXCEEDED';

  constructor(
    message: string,
    public readonly limit: number,
  ) {
    super(message);
    this.name = 'RateLimitError';
  }
}
