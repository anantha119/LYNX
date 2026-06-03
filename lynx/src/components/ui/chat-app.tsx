"use client";

import { useState, useCallback, useEffect } from "react";
import { useUser } from "@auth0/nextjs-auth0/client";
import { cn } from "@/lib/utils";
import { ChatSidebar, type Conversation } from "@/components/ui/chat-sidebar";
import { ChatMessages, type Message } from "@/components/ui/chat-messages";
import { VercelV0Chat } from "@/components/ui/v0-ai-chat";
import { MenuIcon, LogOut } from "lucide-react";

// NEXT_PUBLIC_ prefix exposes this to the browser bundle at build time.
// Set NEXT_PUBLIC_API_URL in .env.local to point at the deployed Cloud Run backend.
// The fallback keeps local dev working with no .env.local change.
const API = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

// Messages fetched per page (initial open + each scroll-up load).
const PAGE_SIZE = 50;

/* ─── Backend shapes ─────────────────────────────────────────────────────── */
type ServerConversation = {
  id: string;
  title: string | null;
  model: string;
  message_count: number;
  last_message_at: string | null;
  created_at: string;
};
type ServerMessage = {
  id: string;
  role: "user" | "assistant" | "system" | "tool";
  content: { type: string; text?: string }[];
  status: string;
  created_at: string;
};

function toConversation(c: ServerConversation): Conversation {
  return {
    id: c.id,
    title: c.title ?? "New conversation",
    preview: "",
    updatedAt: new Date(c.last_message_at ?? c.created_at),
  };
}

function toMessage(m: ServerMessage): Message {
  return {
    id: m.id,
    role: m.role === "assistant" ? "assistant" : "user",
    content: m.content.map((p) => p.text ?? "").join(""),
    timestamp: new Date(m.created_at),
    streaming: m.status === "streaming",
  };
}

/* ─── API calls ──────────────────────────────────────────────────────────── */
function authHeaders(token: string): HeadersInit {
  return { Authorization: `Bearer ${token}` };
}

async function apiListConversations(token: string): Promise<Conversation[]> {
  const res = await fetch(`${API}/v1/conversations`, { headers: authHeaders(token) });
  const data = await res.json();
  return (data.data as ServerConversation[]).map(toConversation);
}

type MessagePage = {
  messages: Message[];
  nextCursor: string | null;
  hasMore: boolean;
};

async function apiGetMessages(
  id: string,
  token: string,
  before?: string
): Promise<MessagePage> {
  const url = new URL(`${API}/v1/conversations/${id}/messages`);
  url.searchParams.set("limit", String(PAGE_SIZE));
  if (before) url.searchParams.set("before", before);
  const res = await fetch(url, { headers: authHeaders(token) });
  const data = await res.json();
  return {
    messages: (data.data as ServerMessage[]).map(toMessage),
    nextCursor: (data.next_cursor as string | null) ?? null,
    hasMore: Boolean(data.has_more),
  };
}

async function apiCreateConversation(token: string): Promise<Conversation> {
  const res = await fetch(`${API}/v1/conversations`, {
    method: "POST",
    headers: authHeaders(token),
  });
  return toConversation((await res.json()) as ServerConversation);
}

async function apiSendMessage(
  id: string,
  content: string,
  token: string,
  onToken: (token: string) => void
): Promise<void> {
  const res = await fetch(`${API}/v1/conversations/${id}/messages`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders(token) },
    body: JSON.stringify({ content }),
  });
  if (!res.body) return;
  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    onToken(decoder.decode(value, { stream: true }));
  }
}

/* ─── Types ──────────────────────────────────────────────────────────────── */
type ConversationMessages = Record<string, Message[]>;

function uid() {
  return Math.random().toString(36).slice(2, 9);
}

/* ─── Chat App ───────────────────────────────────────────────────────────── */
export function ChatApp() {
  const { user } = useUser();
  const [accessToken, setAccessToken] = useState<string>("");
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [activeId, setActiveId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ConversationMessages>({});
  const [pageInfo, setPageInfo] = useState<
    Record<string, { nextCursor: string | null; hasMore: boolean }>
  >({});
  const [loadingOlderId, setLoadingOlderId] = useState<string | null>(null);
  const [mobileOpen, setMobileOpen] = useState(false);

  /* ── Fetch access token once on mount ───────────────────────────────── */
  useEffect(() => {
    fetch("/api/auth/token")
      .then((r) => r.json())
      .then(({ accessToken }) => setAccessToken(accessToken))
      .catch((err) => console.error("failed to get access token:", err));
  }, []);

  /* ── Load the conversation list from the backend once token is ready ── */
  useEffect(() => {
    if (!accessToken) return;
    apiListConversations(accessToken)
      .then(setConversations)
      .catch((err) => console.error("failed to load conversations:", err));
  }, [accessToken]);

  /* ── Helpers ─────────────────────────────────────────────────────────── */
  const activeMessages: Message[] = activeId ? (messages[activeId] ?? []) : [];

  const appendMessage = useCallback(
    (convId: string, msg: Message) => {
      setMessages((prev) => ({
        ...prev,
        [convId]: [...(prev[convId] ?? []), msg],
      }));
    },
    []
  );

  const updateLastMessage = useCallback(
    (convId: string, patch: Partial<Message> | ((prev: Message) => Partial<Message>)) => {
      setMessages((prev) => {
        const msgs = [...(prev[convId] ?? [])];
        if (!msgs.length) return prev;
        const last = msgs[msgs.length - 1];
        const update = typeof patch === "function" ? patch(last) : patch;
        msgs[msgs.length - 1] = { ...last, ...update };
        return { ...prev, [convId]: msgs };
      });
    },
    []
  );

  /* ── Send message ────────────────────────────────────────────────────── */
  const handleSend = useCallback(
    async (text: string) => {
      // Create a new conversation on the server if none is active.
      let convId = activeId;
      if (!convId) {
        const conv = await apiCreateConversation(accessToken);
        convId = conv.id;
        setConversations((prev) => [conv, ...prev]);
        setActiveId(convId);
      }

      const userMsg: Message = {
        id: uid(),
        role: "user",
        content: text,
        timestamp: new Date(),
      };
      appendMessage(convId, userMsg);

      // Add empty assistant message immediately (shows streaming cursor)
      const aiMsg: Message = {
        id: uid(),
        role: "assistant",
        content: "",
        timestamp: new Date(),
        streaming: true,
      };
      appendMessage(convId, aiMsg);

      // Stream the reply from the backend; the server persists both messages.
      try {
        await apiSendMessage(convId, text, accessToken, (token) => {
          updateLastMessage(convId!, (prev) => ({
            content: (prev.content ?? "") + token,
            streaming: true,
          }));
        });
      } catch (err) {
        console.error("send failed:", err);
        updateLastMessage(convId, {
          content: "[Error: failed to get response]",
        });
      }

      updateLastMessage(convId, { streaming: false });

      // Refresh the sidebar so the auto-generated title and ordering appear.
      apiListConversations(accessToken)
        .then(setConversations)
        .catch(() => {});
    },
    [activeId, accessToken, appendMessage, updateLastMessage]
  );

  /* ── New conversation ────────────────────────────────────────────────── */
  const handleNew = useCallback(() => {
    setActiveId(null);
    setMobileOpen(false);
  }, []);

  /* ── Select conversation — load its messages from the backend ────────── */
  const handleSelect = useCallback(
    (id: string) => {
      setActiveId(id);
      setMobileOpen(false);
      // Fetch the first (most recent) page the first time a conversation is opened.
      if (!messages[id]) {
        apiGetMessages(id, accessToken)
          .then(({ messages: msgs, nextCursor, hasMore }) => {
            setMessages((prev) => ({ ...prev, [id]: msgs }));
            setPageInfo((prev) => ({ ...prev, [id]: { nextCursor, hasMore } }));
          })
          .catch((err) => console.error("failed to load messages:", err));
      }
    },
    [messages, accessToken]
  );

  /* ── Load older messages (cursor pagination, scroll-up) ──────────────── */
  const handleLoadOlder = useCallback(
    async (convId: string) => {
      const info = pageInfo[convId];
      if (!info?.hasMore || !info.nextCursor) return;
      if (loadingOlderId === convId) return;

      setLoadingOlderId(convId);
      try {
        const { messages: older, nextCursor, hasMore } = await apiGetMessages(
          convId,
          accessToken,
          info.nextCursor
        );
        // Older messages are oldest-first; prepend them ahead of the current page.
        setMessages((prev) => ({
          ...prev,
          [convId]: [...older, ...(prev[convId] ?? [])],
        }));
        setPageInfo((prev) => ({ ...prev, [convId]: { nextCursor, hasMore } }));
      } catch (err) {
        console.error("failed to load older messages:", err);
      } finally {
        setLoadingOlderId(null);
      }
    },
    [pageInfo, loadingOlderId, accessToken]
  );

  /* ── Render ──────────────────────────────────────────────────────────── */
  return (
    <div className="flex h-screen w-full overflow-hidden bg-[#080808]">
      {/* ── Sidebar ── */}
      <ChatSidebar
        conversations={conversations}
        activeId={activeId}
        onSelect={handleSelect}
        onNew={handleNew}
        mobileOpen={mobileOpen}
        onMobileClose={() => setMobileOpen(false)}
      />

      {/* ── Main area ── */}
      <div className="flex flex-1 flex-col min-w-0 relative">
        {/* Background decorations (only on empty/hero state) */}
        {!activeId && (
          <>
            <div
              className="pointer-events-none absolute inset-0"
              style={{
                backgroundImage:
                  "radial-gradient(circle, rgba(255,255,255,0.045) 1px, transparent 1px)",
                backgroundSize: "28px 28px",
              }}
            />
            <div
              className="pointer-events-none absolute inset-0"
              style={{
                background:
                  "radial-gradient(ellipse 70% 60% at 50% 50%, transparent 40%, #080808 100%)",
              }}
            />
            <div
              className="pointer-events-none absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-[500px] h-[320px] rounded-full opacity-[0.07]"
              style={{
                background:
                  "radial-gradient(ellipse at center, #f59e0b 0%, transparent 70%)",
                filter: "blur(60px)",
              }}
            />
          </>
        )}

        {/* ── Top bar ── */}
        <header
          className={cn(
            "flex items-center justify-between px-4 py-3 flex-shrink-0 z-10",
            activeId && "border-b border-stone-900/80"
          )}
        >
          <div className="flex items-center gap-3">
            {/* Mobile hamburger */}
            <button
              onClick={() => setMobileOpen(true)}
              className="md:hidden p-1.5 rounded text-stone-600 hover:text-stone-300 transition-colors cursor-pointer"
              aria-label="Open sidebar"
            >
              <MenuIcon className="w-4 h-4" />
            </button>

            {activeId && (
              <div className="flex items-center gap-2 min-w-0">
                <span className="flex-shrink-0 w-px h-3.5 bg-amber-400/90" />
                <p className="text-[11px] font-mono tracking-[0.14em] text-stone-200 uppercase truncate">
                  {conversations.find((c) => c.id === activeId)?.title ?? "Conversation"}
                </p>
              </div>
            )}
          </div>

          {/* User info + logout */}
          <div className="flex items-center gap-2">
            {user && (
              <span className="hidden sm:block text-[11px] font-mono text-stone-500 truncate max-w-[160px]">
                {user.email ?? user.name}
              </span>
            )}
            <a
              href="/api/auth/logout"
              className="p-1.5 rounded text-stone-600 hover:text-stone-300 transition-colors"
              title="Sign out"
            >
              <LogOut className="w-4 h-4" />
            </a>
          </div>
        </header>

        {/* ── Content: messages OR hero ── */}
        <div className="flex-1 flex flex-col min-h-0 relative z-10">
          {activeId && activeMessages.length > 0 ? (
            <>
              <ChatMessages
                key={activeId}
                messages={activeMessages}
                hasMore={pageInfo[activeId]?.hasMore ?? false}
                loadingOlder={loadingOlderId === activeId}
                onLoadOlder={() => handleLoadOlder(activeId)}
              />
              <div className="flex-shrink-0 border-t border-stone-900/80 bg-[#080808]">
                <VercelV0Chat
                  onSend={handleSend}
                  showHero={false}
                  placeholder="Continue the conversation…"
                />
              </div>
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center">
              <VercelV0Chat onSend={handleSend} showHero={true} />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
