# Lynx — Database study sheet

Self-contained reference for the Lynx Postgres layer: schema DDL first, then
every data-access query (the exact statements run by `backend/src/db/`), with
parameters labeled in order and a one-line note on *why* each is written that way.

---

## Schema (DDL)

All tables live in the `lynx` database (Cloud SQL, Postgres 18). `CITEXT` and
`gen_random_uuid()` require the `citext` and `pgcrypto` extensions.

### `users`
```sql
CREATE TABLE users (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email        CITEXT UNIQUE NOT NULL,
  display_name TEXT,
  auth0_sub    TEXT UNIQUE,                 -- added in migration add_auth0_sub.sql
  created_at   TIMESTAMPTZ DEFAULT now(),
  deleted_at   TIMESTAMPTZ                  -- soft delete; never hard-deleted
);
```

### `conversations`
```sql
CREATE TABLE conversations (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES users(id),
  title           TEXT,                        -- null until auto-generated
  model           TEXT NOT NULL,               -- e.g. "gemini-3.1-flash-lite"
  system_prompt   TEXT,
  active_leaf_id  TEXT,                         -- ULID of the current tip message
  message_count   INT NOT NULL DEFAULT 0,       -- denormalized counter
  last_message_at TIMESTAMPTZ,                  -- denormalized, powers sidebar sort
  created_at      TIMESTAMPTZ DEFAULT now(),
  updated_at      TIMESTAMPTZ DEFAULT now(),
  archived_at     TIMESTAMPTZ,                  -- soft archive
  deleted_at      TIMESTAMPTZ,                  -- soft delete
  metadata        JSONB DEFAULT '{}'
);
```

### `messages`
```sql
CREATE TABLE messages (
  id              TEXT PRIMARY KEY,             -- ULID (time-sortable, cursor-friendly)
  conversation_id UUID NOT NULL REFERENCES conversations(id),
  parent_id       TEXT REFERENCES messages(id), -- self-FK; enables branching
  role            TEXT CHECK (role IN ('user','assistant','system','tool')),
  content         JSONB NOT NULL                -- array of typed parts: [{ type, text }]
                  CHECK (jsonb_typeof(content) = 'array'),
  status          TEXT DEFAULT 'complete'
                  CHECK (status IN ('streaming','complete','error','cancelled')),
  token_count     INT,
  model           TEXT,
  created_at      TIMESTAMPTZ DEFAULT now(),
  deleted_at      TIMESTAMPTZ,
  metadata        JSONB DEFAULT '{}',           -- finish_reason, latency, cost, etc.
  -- Generated column for full-text search:
  content_tsv     tsvector GENERATED ALWAYS AS (
                    to_tsvector('english', coalesce(message_text(content), ''))
                  ) STORED
);
```

### `conversation_summaries`
```sql
CREATE TABLE conversation_summaries (
  conversation_id   UUID NOT NULL REFERENCES conversations(id),
  covers_through_id TEXT NOT NULL,              -- ULID: summary covers up to this message
  summary           TEXT NOT NULL,
  token_count       INT NOT NULL,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (conversation_id, covers_through_id)
);
```

### `idempotency_keys` *(table exists; not yet wired into the API)*
```sql
CREATE TABLE idempotency_keys (
  conversation_id UUID REFERENCES conversations(id),
  idempotency_key TEXT,
  message_id      TEXT REFERENCES messages(id),
  created_at      TIMESTAMPTZ DEFAULT now(),
  PRIMARY KEY (conversation_id, idempotency_key)
);
```

### `attachments` *(table exists; upload flow deferred)*
```sql
CREATE TABLE attachments (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  message_id   TEXT REFERENCES messages(id),
  storage_key  TEXT NOT NULL,                   -- Cloud Storage object key
  content_type TEXT NOT NULL,
  size_bytes   BIGINT NOT NULL,
  created_at   TIMESTAMPTZ DEFAULT now()
);
```

### Indexes
Every index is **partial** (`WHERE deleted_at IS NULL`) so soft-deleted rows
stay out of the hot paths and the index itself stays small.

```sql
-- Hot path: load a conversation's recent messages (and the pagination range scan).
-- (conversation_id, id DESC) matches both the WHERE filter and the ORDER BY.
CREATE INDEX idx_messages_conv_id ON messages (conversation_id, id DESC)
  WHERE deleted_at IS NULL;

-- Branch navigation: find the children of a node.
CREATE INDEX idx_messages_parent ON messages (parent_id)
  WHERE deleted_at IS NULL;

-- Sidebar: a user's conversations sorted by last activity. Matches the exact
-- predicate + sort of listConversations.
CREATE INDEX idx_conversations_user_active ON conversations (user_id, last_message_at DESC)
  WHERE deleted_at IS NULL AND archived_at IS NULL;

-- Attachments by message.
CREATE INDEX idx_attachments_message ON attachments (message_id);

-- Full-text search over message content.
CREATE INDEX idx_messages_tsv ON messages USING GIN (content_tsv);
```

---

## Queries

Each query below is the exact statement run by a function in `backend/src/db/`.
Postgres positional params (`$1`, `$2`, ...) are kept; the note above each query
lists what each one is, in order.

---

## conversations.ts

### `createConversation($1 user_id, $2 model, $3 title)`
Insert a new conversation and return the row the app needs back.

```sql
INSERT INTO conversations (user_id, model, title)
VALUES ($1, $2, $3)
RETURNING id, title, model, message_count, last_message_at, created_at;
```

### `listConversations($1 user_id)`
A user's active conversations, most-recently-active first. `NULLS LAST` keeps
brand-new (never-messaged) convos from jumping to the top.

```sql
SELECT id, title, model, message_count, last_message_at, created_at
FROM conversations
WHERE user_id = $1
  AND deleted_at IS NULL
  AND archived_at IS NULL
ORDER BY last_message_at DESC NULLS LAST, created_at DESC;
```

### `getConversation($1 id, $2 user_id)`
One conversation, scoped to its owner (prevents cross-user reads).

```sql
SELECT id, title, model, message_count, last_message_at, created_at
FROM conversations
WHERE id = $1
  AND user_id = $2
  AND deleted_at IS NULL;
```

### `updateTitle($1 id, $2 user_id, $3 title)`
Set the title (auto-title generation). Owner-scoped.

```sql
UPDATE conversations
SET title = $3, updated_at = now()
WHERE id = $1
  AND user_id = $2;
```

---

## messages.ts

### `appendMessage(...)` — one transaction
Runs as a single transaction so the denormalized counters on `conversations`
can never drift from the actual message rows. The ULID (`$1`) is generated in
app code (`ulid()`), not by the DB.

Params: `$1 id (ULID)`, `$2 conversation_id`, `$3 parent_id`, `$4 role`,
`$5 content (JSONB array-of-parts)`, `$6 status`, `$7 model`, `$8 token_count`.

```sql
BEGIN;

INSERT INTO messages
  (id, conversation_id, parent_id, role, content, status, model, token_count)
VALUES
  ($1, $2, $3, $4, $5::jsonb, $6, $7, $8);

-- $1 conversation_id, $2 the message id just inserted (the new active leaf)
UPDATE conversations
SET message_count   = message_count + 1,
    last_message_at = now(),
    updated_at      = now(),
    active_leaf_id  = $2
WHERE id = $1;

COMMIT;
-- (on error: ROLLBACK)
```

### `finalizeAssistantMessage($1 id, $2 full_text as JSONB, $3 token_count)`
Swap the streaming placeholder's empty content for the final text.

```sql
UPDATE messages
SET content = $2::jsonb,
    status = 'complete',
    token_count = $3
WHERE id = $1;
```

### `markMessageError($1 id, $2 partial_text as JSONB)`
Provider failure / disconnect: keep whatever streamed, mark the row errored.

```sql
UPDATE messages
SET content = $2::jsonb,
    status = 'error'
WHERE id = $1;
```

### `getMessages($1 conversation_id)`
ALL messages oldest-first (server-side context assembly path). ULIDs sort by
creation time, so `ORDER BY id ASC` == chronological.

```sql
SELECT id, role, content, status, created_at, token_count
FROM messages
WHERE conversation_id = $1
  AND deleted_at IS NULL
ORDER BY id ASC;
```

### `getMessagesPage` — most recent page (no cursor)
`$1 conversation_id`, `$2 = limit + 1` (fetch one extra to detect `hasMore`).

```sql
SELECT id, role, content, status, created_at, token_count
FROM messages
WHERE conversation_id = $1
  AND deleted_at IS NULL
ORDER BY id DESC
LIMIT $2;
```

### `getMessagesPage` — older page (cursor supplied)
`$1 conversation_id`, `$2 before (cursor ULID)`, `$3 = limit + 1`.
`id < before` == "older than the cursor" (clean range scan on
`idx_messages_conv_id (conversation_id, id DESC)`).

```sql
SELECT id, role, content, status, created_at, token_count
FROM messages
WHERE conversation_id = $1
  AND deleted_at IS NULL
  AND id < $2
ORDER BY id DESC
LIMIT $3;
```

App then drops the extra row, reverses to oldest-first, and uses the oldest
returned id as the next `before` cursor.

### `getActiveLeafId($1 conversation_id)`
Current leaf — used as `parent_id` for the next appended message.

```sql
SELECT active_leaf_id
FROM conversations
WHERE id = $1;
```

---

## summaries.ts

### `getLatestSummary($1 conversation_id)`
Most recent rolling summary. `covers_through_id` is a ULID, so MAX-by-id ==
"covers the most messages".

```sql
SELECT conversation_id, covers_through_id, summary, token_count, created_at
FROM conversation_summaries
WHERE conversation_id = $1
ORDER BY covers_through_id DESC
LIMIT 1;
```

### `insertSummary($1 conversation_id, $2 covers_through_id, $3 summary, $4 token_count)`
Upsert: re-summarizing the same coverage point overwrites the prior summary.

```sql
INSERT INTO conversation_summaries
  (conversation_id, covers_through_id, summary, token_count)
VALUES ($1, $2, $3, $4)
ON CONFLICT (conversation_id, covers_through_id) DO UPDATE
SET summary = EXCLUDED.summary,
    token_count = EXCLUDED.token_count;
```

---

## users.ts

### `getOrCreateUser` — fast path lookup (`$1 auth0_sub`)
Single indexed lookup; hit on every request after first login.

```sql
SELECT id
FROM users
WHERE auth0_sub = $1
  AND deleted_at IS NULL;
```

### `getOrCreateUser` — first-login insert
`$1 email`, `$2 display_name`, `$3 auth0_sub`. `ON CONFLICT` guards the race
where two requests insert the same sub at once; the loser gets the existing row
back instead of erroring.

```sql
INSERT INTO users (email, display_name, auth0_sub)
VALUES ($1, $2, $3)
ON CONFLICT (auth0_sub) DO UPDATE
SET email = EXCLUDED.email
RETURNING id;
```
