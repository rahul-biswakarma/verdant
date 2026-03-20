import * as admin from 'firebase-admin';
import { Request } from 'firebase-functions/v2/https';

/**
 * Verifies the Firebase Auth ID token in the Authorization header.
 *
 * @param req - Incoming Firebase Functions request
 * @returns Resolved Firebase UID
 * @throws Error with message 'Unauthorized' if token is missing or invalid
 */
export async function verifyAuthToken(req: Request): Promise<string> {
  const authHeader = req.headers['authorization'] as string | undefined;

  if (!authHeader?.startsWith('Bearer ')) {
    throw new AuthError('Missing or malformed Authorization header. Expected: Bearer <token>');
  }

  const idToken = authHeader.substring(7); // strip "Bearer "

  try {
    const decoded = await admin.auth().verifyIdToken(idToken);
    return decoded.uid;
  } catch (err) {
    throw new AuthError(`Invalid Firebase ID token: ${(err as Error).message}`);
  }
}

export class AuthError extends Error {
  readonly code = 'UNAUTHENTICATED';
  constructor(message: string) {
    super(message);
    this.name = 'AuthError';
  }
}
