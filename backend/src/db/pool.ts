import "dotenv/config";
import { Pool } from "pg";

/**
 * Shared PostgreSQL connection pool.
 *
 * Locally we connect through the Cloud SQL Auth Proxy, which listens on
 * 127.0.0.1:5432 and tunnels securely to the `lynx-db` Cloud SQL instance.
 * In production (Cloud Run) the same env vars point at the managed connector.
 */
export const pool = new Pool({
  host: process.env.DB_HOST,
  port: Number(process.env.DB_PORT ?? 5432),
  database: process.env.DB_NAME,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  max: 10,
  idleTimeoutMillis: 30_000,
});

pool.on("error", (err) => {
  console.error("[db] unexpected idle client error:", err);
});

/** Convenience query helper. */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function query<T extends Record<string, any> = Record<string, any>>(
  text: string,
  params?: unknown[]
) {
  return pool.query<T>(text, params);
}
