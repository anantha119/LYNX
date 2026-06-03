import "dotenv/config";
import { Hono } from "hono";
import { serve } from "@hono/node-server";
import { cors } from "hono/cors";
import { langfuse, CHAT_MODEL } from "./ai/config.js";
import { streamChat, type ChatMessage } from "./ai/chat.js";
import { generateTitle } from "./ai/title.js";
import { initSystemPrompt } from "./ai/prompt.js";
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
  const userMsgId = await insertUserMessage(id, parentId, content);

  // 2. Build the model context from the now-persisted history.
  const history: ChatMessage[] = (await getMessages(id))
    .filter((m) => m.role === "user" || m.role === "assistant")
    .map((m) => ({
      role: m.role as "user" | "assistant",
      content: m.content.map((p) => p.text ?? "").join(""),
    }));

  // 3. Durable assistant row exists before streaming begins.
  const assistantMsgId = await insertAssistantPlaceholder(id, userMsgId, CHAT_MODEL);

  const encoder = new TextEncoder();
  let fullText = "";

  const stream = new ReadableStream({
    async start(controller) {
      try {
        await streamChat(history, (token) => {
          fullText += token;
          controller.enqueue(encoder.encode(token));
        });
        await finalizeAssistantMessage(assistantMsgId, fullText);

        // 4. Auto-title the conversation on its first exchange.
        if (!conv.title) {
          const title = await generateTitle(content);
          await updateTitle(userId, id, title);
        }
      } catch (err) {
        console.error("[messages] stream error:", err);
        await markMessageError(assistantMsgId, fullText);
        controller.enqueue(encoder.encode("\n\n[Error: failed to get response]"));
      } finally {
        controller.close();
      }
    },
  });

  return new Response(stream, {
    headers: {
      "Content-Type": "text/plain; charset=utf-8",
      "Transfer-Encoding": "chunked",
      "Access-Control-Allow-Origin": CORS_ORIGIN,
    },
  });
});

// Cloud Run injects PORT as an env var and expects the container to bind to it.
// Defaulting to 8080 means local dev needs no .env change.
const PORT = Number(process.env.PORT ?? 8080);

// Load the system prompt into memory before serving traffic, then start the
// background refresh. Chat requests read it instantly — no per-request fetch.
await initSystemPrompt();

const server = serve({ fetch: app.fetch, port: PORT, hostname: "0.0.0.0" });
console.log(`Lynx backend running on http://localhost:${PORT}`);

// Graceful shutdown — flush any pending Langfuse traces before exiting so
// observability data is never lost on Ctrl+C, tsx restarts, or Cloud Run stops.
async function shutdown() {
  console.log("\nShutting down — flushing Langfuse traces...");
  server.close();
  await langfuse.shutdown();
  process.exit(0);
}

process.on("SIGINT", shutdown);
process.on("SIGTERM", shutdown);
