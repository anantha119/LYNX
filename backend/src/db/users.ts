import { query } from "./pool.js";

/**
 * Returns the UUID for a given Auth0 sub, creating the user on first login.
 * Fast path: single indexed lookup. Slow path (first login only): inserts the user
 * after fetching their profile from Auth0's UserInfo endpoint.
 */
export async function getOrCreateUser(
  auth0Sub: string,
  accessToken: string
): Promise<string> {
  const existing = await query<{ id: string }>(
    `SELECT id FROM users WHERE auth0_sub = $1 AND deleted_at IS NULL`,
    [auth0Sub]
  );
  if (existing.rows[0]) return existing.rows[0].id;

  // First login — fetch profile from Auth0 UserInfo to get email/name.
  const res = await fetch(`https://${process.env.AUTH0_DOMAIN}/userinfo`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  const profile = await res.json() as { email?: string; name?: string; nickname?: string };

  const email = profile.email ?? `${auth0Sub}@placeholder.lynx`;
  const displayName = profile.name ?? profile.nickname ?? null;

  const inserted = await query<{ id: string }>(
    `INSERT INTO users (email, display_name, auth0_sub)
     VALUES ($1, $2, $3)
     ON CONFLICT (auth0_sub) DO UPDATE SET email = EXCLUDED.email
     RETURNING id`,
    [email, displayName, auth0Sub]
  );
  return inserted.rows[0].id;
}
