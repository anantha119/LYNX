import { query } from "./pool.js";

export type ConversationSummary = {
  conversation_id: string;
  covers_through_id: string;
  summary: string;
  token_count: number;
  created_at: string;
};

export async function getLatestSummary(conversationId: string): Promise<ConversationSummary | null> {
  const r = await query<ConversationSummary>(
    `SELECT conversation_id, covers_through_id, summary, token_count, created_at
     FROM conversation_summaries
     WHERE conversation_id = $1
     ORDER BY covers_through_id DESC
     LIMIT 1`,
    [conversationId]
  );
  return r.rows[0] ?? null;
}

export async function insertSummary(
  conversationId: string,
  coversThroughId: string,
  summary: string,
  tokenCount: number
): Promise<void> {
  await query(
    `INSERT INTO conversation_summaries (conversation_id, covers_through_id, summary, token_count)
     VALUES ($1, $2, $3, $4)
     ON CONFLICT (conversation_id, covers_through_id) DO UPDATE
     SET summary = EXCLUDED.summary, token_count = EXCLUDED.token_count`,
    [conversationId, coversThroughId, summary, tokenCount]
  );
}
