import { ulid } from "ulid";
import { pool, query } from "./pool.js";

export type Role = "user" | "assistant" | "system" | "tool";

export type MessageRow = {
  id: string;
  role: Role;
  content: { type: string; text?: string }[];
  status: string;
  created_at: string;
};

/** Wrap a plain string as the JSONB array-of-parts the schema requires. */
function textContent(text: string) {
  return JSON.stringify([{ type: "text", text }]);
}

/**
 * Insert a message and, in the SAME transaction, bump the conversation's
 * counters and advance its active_leaf_id. Keeping these together means the
 * denormalized counters can never drift from the actual rows.
 * Returns the new message's ULID.
 */
async function appendMessage(opts: {
  conversationId: string;
  parentId: string | null;
  role: Role;
  text: string;
  status: "streaming" | "complete";
  model?: string | null;
  tokenCount?: number | null;
}): Promise<string> {
  const id = ulid();
  const client = await pool.connect();
  try {
    await client.query("BEGIN");
    await client.query(
      `INSERT INTO messages (id, conversation_id, parent_id, role, content, status, model, token_count)
       VALUES ($1, $2, $3, $4, $5::jsonb, $6, $7, $8)`,
      [
        id,
        opts.conversationId,
        opts.parentId,
        opts.role,
        textContent(opts.text),
        opts.status,
        opts.model ?? null,
        opts.tokenCount ?? null,
      ]
    );
    await client.query(
      `UPDATE conversations
       SET message_count = message_count + 1,
           last_message_at = now(),
           updated_at = now(),
           active_leaf_id = $2
       WHERE id = $1`,
      [opts.conversationId, id]
    );
    await client.query("COMMIT");
    return id;
  } catch (e) {
    await client.query("ROLLBACK");
    throw e;
  } finally {
    client.release();
  }
}

/** Persist a completed user message. Returns its id (the new active leaf). */
export function insertUserMessage(
  conversationId: string,
  parentId: string | null,
  text: string
): Promise<string> {
  return appendMessage({
    conversationId,
    parentId,
    role: "user",
    text,
    status: "complete",
  });
}

/**
 * Create the assistant message row up front with status 'streaming' and empty
 * text, so a durable row exists from the moment streaming begins.
 * Returns its id.
 */
export function insertAssistantPlaceholder(
  conversationId: string,
  parentId: string | null,
  model: string
): Promise<string> {
  return appendMessage({
    conversationId,
    parentId,
    role: "assistant",
    text: "",
    status: "streaming",
    model,
  });
}

/** Finalize the assistant row once streaming completes. */
export async function finalizeAssistantMessage(
  id: string,
  fullText: string,
  tokenCount: number | null = null
): Promise<void> {
  await query(
    `UPDATE messages
     SET content = $2::jsonb, status = 'complete', token_count = $3
     WHERE id = $1`,
    [id, textContent(fullText), tokenCount]
  );
}

/** Mark a streaming assistant row as errored (provider failure / disconnect). */
export async function markMessageError(id: string, partialText = ""): Promise<void> {
  await query(
    `UPDATE messages SET content = $2::jsonb, status = 'error' WHERE id = $1`,
    [id, textContent(partialText)]
  );
}

/**
 * Load ALL of a conversation's messages, oldest-first.
 * Used server-side to assemble model context on the send path (will be
 * replaced by budgeted context assembly later). Not for the API read path —
 * use getMessagesPage for that. Linear thread only; excludes soft-deleted rows.
 */
export async function getMessages(conversationId: string): Promise<MessageRow[]> {
  const r = await query<MessageRow>(
    `SELECT id, role, content, status, created_at
     FROM messages
     WHERE conversation_id = $1 AND deleted_at IS NULL
     ORDER BY id ASC`,
    [conversationId]
  );
  return r.rows;
}

export type MessagePage = {
  data: MessageRow[];
  nextCursor: string | null;
  hasMore: boolean;
};

const DEFAULT_PAGE_LIMIT = 50;
const MAX_PAGE_LIMIT = 100;

/**
 * Cursor-paginated message read for the API.
 *
 * Returns the most recent `limit` messages oldest-first, or — when `before`
 * (a message ULID) is supplied — the page of messages immediately older than
 * that cursor. Because ULIDs sort by creation time, `id < before` means
 * "older than", so this is a clean range scan on idx_messages_conv_id
 * (conversation_id, id DESC).
 *
 * `hasMore` / `nextCursor` are derived by fetching one extra row (limit + 1)
 * rather than running a separate COUNT. `nextCursor` is the oldest id in the
 * returned page — pass it back as `before` to load the next older page.
 *
 * Linear thread only (no branching yet); excludes soft-deleted rows.
 */
export async function getMessagesPage(
  conversationId: string,
  opts: { before?: string | null; limit?: number } = {}
): Promise<MessagePage> {
  const limit = Math.min(Math.max(opts.limit ?? DEFAULT_PAGE_LIMIT, 1), MAX_PAGE_LIMIT);

  const params: unknown[] = [conversationId];
  let cursorClause = "";
  if (opts.before) {
    params.push(opts.before);
    cursorClause = `AND id < $${params.length}`;
  }
  params.push(limit + 1); // fetch one extra to detect a further page

  const r = await query<MessageRow>(
    `SELECT id, role, content, status, created_at
     FROM messages
     WHERE conversation_id = $1 AND deleted_at IS NULL ${cursorClause}
     ORDER BY id DESC
     LIMIT $${params.length}`,
    params
  );

  const hasMore = r.rows.length > limit;
  const rows = hasMore ? r.rows.slice(0, limit) : r.rows;
  rows.reverse(); // query returns newest-first; UI renders oldest-first

  return {
    data: rows,
    nextCursor: hasMore ? (rows[0]?.id ?? null) : null,
    hasMore,
  };
}

/** The id of the current leaf message, used as parent_id for the next message. */
export async function getActiveLeafId(conversationId: string): Promise<string | null> {
  const r = await query<{ active_leaf_id: string | null }>(
    `SELECT active_leaf_id FROM conversations WHERE id = $1`,
    [conversationId]
  );
  return r.rows[0]?.active_leaf_id ?? null;
}
