import { query } from "./pool.js";

export type ConversationRow = {
  id: string;
  title: string | null;
  model: string;
  message_count: number;
  last_message_at: string | null;
  created_at: string;
};

/** Create a new conversation owned by the given user. */
export async function createConversation(
  userId: string,
  model: string,
  title: string | null = null
): Promise<ConversationRow> {
  const r = await query<ConversationRow>(
    `INSERT INTO conversations (user_id, model, title)
     VALUES ($1, $2, $3)
     RETURNING id, title, model, message_count, last_message_at, created_at`,
    [userId, model, title]
  );
  return r.rows[0];
}

/**
 * List a user's conversations, most-recently-active first.
 * Excludes soft-deleted and archived rows (matches idx_conversations_user_active).
 */
export async function listConversations(userId: string): Promise<ConversationRow[]> {
  const r = await query<ConversationRow>(
    `SELECT id, title, model, message_count, last_message_at, created_at
     FROM conversations
     WHERE user_id = $1 AND deleted_at IS NULL AND archived_at IS NULL
     ORDER BY last_message_at DESC NULLS LAST, created_at DESC`,
    [userId]
  );
  return r.rows;
}

/** Fetch a single conversation, scoped to its owner. */
export async function getConversation(
  userId: string,
  id: string
): Promise<ConversationRow | null> {
  const r = await query<ConversationRow>(
    `SELECT id, title, model, message_count, last_message_at, created_at
     FROM conversations
     WHERE id = $1 AND user_id = $2 AND deleted_at IS NULL`,
    [id, userId]
  );
  return r.rows[0] ?? null;
}

/** Set a conversation's title (used by auto-title generation). */
export async function updateTitle(
  userId: string,
  id: string,
  title: string
): Promise<void> {
  await query(
    `UPDATE conversations
     SET title = $3, updated_at = now()
     WHERE id = $1 AND user_id = $2`,
    [id, userId, title]
  );
}
