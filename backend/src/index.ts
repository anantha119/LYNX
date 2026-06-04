import "dotenv/config";
import { Hono } from "hono";
import { serve } from "@hono/node-server";
import { cors } from "hono/cors";
import { ai, langfuse, CHAT_MODEL } from "./ai/config.js";
import { streamChat, type ChatMessage } from "./ai/chat.js";
import { generateTitle } from "./ai/title.js";
import { initSystemPrompt } from "./ai/prompt.js";
import { generateSummaryAsync } from "./ai/summary.js";
import { authMiddleware } from "./middleware/auth.js";
import {
  createConversation,
  listConversations,
  getConversation,
  updateTitle,
} from "./db/conversations.js";
import {
  insertUserMessage,
  insertAssistantPlaceholder,
  finalizeAssistantMessage,
  markMessageError,
  getMessages,
  getMessagesPage,
  getActiveLeafId,
} from "./db/messages.js";
import { getLatestSummary } from "./db/summaries.js";

type Env = { Variables: { userId: string } };

const app = new Hono<Env>();

// CORS origin is environment-driven so the same image works locally and in Cloud Run.
// Locally defaults to the Next.js dev server; set CORS_ORIGIN in Cloud Run env vars
// when the frontend is deployed to a different origin.
const CORS_ORIGIN = process.env.CORS_ORIGIN ?? "http://localhost:3000";
app.use("*", cors({ origin: CORS_ORIGIN }));

// Health check — unauthenticated, probed by Cloud Run on startup and by load balancers.
// Must be registered before the auth middleware so no Bearer token is required.
app.get("/healthz", (c) => c.json({ status: "ok" }));

// Verify Auth0 JWT and populate c.var.userId on all /v1/* routes
app.use("/v1/*", authMiddleware);

/* ── GET /v1/conversations ───────────────────────────────────────────────────
   Returns the authenticated user's conversations, most-recently-active first.
─────────────────────────────────────────────────────────────────────────── */
app.get("/v1/conversations", async (c) => {
  const userId = c.get("userId");
  const rows = await listConversations(userId);
  return c.json({ data: rows });
});

/* ── POST /v1/conversations ──────────────────────────────────────────────────
   Creates a new (empty) conversation. Returns it.
─────────────────────────────────────────────────────────────────────────── */
app.post("/v1/conversations", async (c) => {
  const userId = c.get("userId");
  const conv = await createConversation(userId, CHAT_MODEL);
  return c.json(conv, 201);
});

/* ── GET /v1/conversations/:id/messages ──────────────────────────────────────
   Cursor-paginated, oldest-first within the page.
   Query: ?limit=50&before=<message_id>
     - no cursor → the most recent `limit` messages
     - before=<ulid> → the page of messages older than that cursor
   Returns: { data, next_cursor, has_more }. Pass next_cursor back as `before`
   to load the next older page.
─────────────────────────────────────────────────────────────────────────── */
app.get("/v1/conversations/:id/messages", async (c) => {
  const userId = c.get("userId");
  const id = c.req.param("id");
  const conv = await getConversation(userId, id);
  if (!conv) return c.json({ error: "not found" }, 404);

  const before = c.req.query("before") ?? null;
  const limitParam = Number(c.req.query("limit"));
  const limit = Number.isFinite(limitParam) && limitParam > 0 ? limitParam : undefined;

  const page = await getMessagesPage(id, { before, limit });
  return c.json({
    data: page.data,
    next_cursor: page.nextCursor,
    has_more: page.hasMore,
  });
});

/* ── POST /v1/conversations/:id/messages ─────────────────────────────────────
   Body:    { content: string }
   Returns: streaming plain text — the assistant reply, one token at a time.

   Persistence: the user message is committed first, then an assistant
   row is created with status 'streaming' before any tokens flow, then it is
   finalized to 'complete' once the stream ends. Nothing acknowledged lives
   only in memory.
─────────────────────────────────────────────────────────────────────────── */
app.post("/v1/conversations/:id/messages", async (c) => {
  const userId = c.get("userId");
  const id = c.req.param("id");
  const { content } = await c.req.json<{ content: string }>();

  const conv = await getConversation(userId, id);
  if (!conv) return c.json({ error: "not found" }, 404);

  // 1. Persist the user message (advances the conversation's active leaf).
  const parentId = await getActiveLeafId(id);
  const userTokensRes = await ai.models.countTokens({ model: CHAT_MODEL, contents: content });
  const userMsgId = await insertUserMessage(id, parentId, content, userTokensRes.totalTokens ?? 0);

  // 2. Build the model context using Context Assembly & Token Budgeting
  const allMessages = await getMessages(id);
  const latestSummary = await getLatestSummary(id);

  const MAX_CONTEXT_TOKENS = 8000;
  const systemPromptTokens = 500;
  const summaryTokens = latestSummary?.token_count ?? 0;
  
  let budget = MAX_CONTEXT_TOKENS - systemPromptTokens - summaryTokens;
  const recentMessages: typeof allMessages = [];
  let reachedSummaryBoundary = false;
  
  for (let i = allMessages.length - 1; i >= 0; i--) {
    const msg = allMessages[i];
    if (msg.role !== "user" && msg.role !== "assistant") continue;

    if (latestSummary && msg.id <= latestSummary.covers_through_id) {
      reachedSummaryBoundary = true;
      break;
    }

    const msgTokens = msg.token_count ?? 500;
    if (budget - msgTokens < 0 && recentMessages.length > 0) {
      break;
    }

    budget -= msgTokens;
    recentMessages.unshift(msg);
  }

  const history: ChatMessage[] = [];
  if (latestSummary) {
    history.push({
      role: "user",
      content: `[System Note: Below is a summary of the older messages in this conversation.]\n\n${latestSummary.summary}`
    });
  }

  for (const msg of recentMessages) {
    history.push({
      role: msg.role as "user" | "assistant",
      content: msg.content.map((p) => p.text ?? "").join(""),
    });
  }

  // 2b. If we dropped messages that aren't summarized yet, kick off summarization async
  if (!reachedSummaryBoundary && allMessages.length > recentMessages.length && recentMessages.length > 0) {
    // The message just before the first one we included is our coversThrough boundary
    const oldestIncludedIndex = allMessages.findIndex(m => m.id === recentMessages[0].id);
    if (oldestIncludedIndex > 0) {
      const coversThroughId = allMessages[oldestIncludedIndex - 1].id;
      generateSummaryAsync(id, coversThroughId).catch(err => 
        console.error("[Summary] Async generation failed:", err)
      );
    }
  }

  // 3. Durable assistant row exists before streaming begins.
  const assistantMsgId = await insertAssistantPlaceholder(id, userMsgId, CHAT_MODEL);

  const encoder = new TextEncoder();
  let fullText = "";

  const stream = new ReadableStream({
    async start(controller) {
      try {
        await streamChat(history, (token) => {
          fullText += token;
          controller.enqueue(encoder.encode(`data: ${JSON.stringify({ type: "token", text: token })}\n\n`));
        });
        const assistantTokensRes = await ai.models.countTokens({ model: CHAT_MODEL, contents: fullText });
        await finalizeAssistantMessage(assistantMsgId, fullText, assistantTokensRes.totalTokens ?? 0);

        // 4. Auto-title the conversation on its first exchange.
        if (!conv.title) {
          generateTitle(content)
            .then((title) => {
              updateTitle(userId, id, title);
              controller.enqueue(encoder.encode(`data: ${JSON.stringify({ type: "title", title })}\n\n`));
              controller.close();
            })
            .catch((err) => {
              console.error("[messages] async title error:", err);
              controller.close();
            });
        } else {
          controller.close();
        }
      } catch (err) {
        console.error("[messages] stream error:", err);
        await markMessageError(assistantMsgId, fullText);
        controller.enqueue(encoder.encode(`data: ${JSON.stringify({ type: "token", text: "\\n\\n[Error: failed to get response]" })}\n\n`));
        controller.close();
      }
    },
  });

  return new Response(stream, {
    headers: {
      "Content-Type": "text/event-stream; charset=utf-8",
      "Cache-Control": "no-cache",
      "Connection": "keep-alive",
      "Access-Control-Allow-Origin": CORS_ORIGIN,
    },
  });
});

// Cloud Run injects PORT as an env var and expects the container to bind to it.
// Defaulting to 8080 means local dev needs no .env change.
const PORT = Number(process.env.PORT ?? 8080);

// Load the system prompt into memory before serving traffic, then start the
// app server.
let server: ReturnType<typeof serve> | undefined;

async function startServer() {
  await initSystemPrompt();

  try {
    const { query } = await import("./db/pool.js");
    await query(`
      CREATE TABLE IF NOT EXISTS conversation_summaries (
        conversation_id     UUID NOT NULL REFERENCES conversations(id),
        covers_through_id   TEXT NOT NULL,
        summary             TEXT NOT NULL,
        token_count         INT NOT NULL,
        created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
        PRIMARY KEY (conversation_id, covers_through_id)
      );
    `);
    console.log("[db] conversation_summaries table verified.");
  } catch (err) {
    console.error("[db] Failed to run migration:", err);
  }

  server = serve({
    fetch: app.fetch,
    port: PORT,
  });
  console.log(`[lynx-backend] Running on http://localhost:${PORT}`);
}

startServer();

// Graceful shutdown — flush any pending Langfuse traces before exiting so
// observability data is never lost on Ctrl+C, tsx restarts, or Cloud Run stops.
async function shutdown() {
  console.log("\nShutting down — flushing Langfuse traces...");
  if (server) server.close();
  await langfuse.shutdown();
  process.exit(0);
}

process.on("SIGINT", shutdown);
process.on("SIGTERM", shutdown);
