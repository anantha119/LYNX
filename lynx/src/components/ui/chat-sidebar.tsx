"use client";

import { cn } from "@/lib/utils";
import {
  PlusIcon,
  MessageSquare,
  Settings,
  ChevronRight,
  X,
} from "lucide-react";

import { useUser } from "@auth0/nextjs-auth0/client";

export type UserProfile = NonNullable<ReturnType<typeof useUser>["user"]>;

export type Conversation = {
  id: string;
  title: string;
  preview: string;
  updatedAt: Date;
};

interface ChatSidebarProps {
  conversations: Conversation[];
  activeId: string | null;
  onSelect: (id: string) => void;
  onNew: () => void;
  mobileOpen: boolean;
  onMobileClose: () => void;
  user?: UserProfile;
}

/* ─── Group conversations by time ───────────────────────────────────────── */
function groupConversations(convs: Conversation[]) {
  const now = new Date();
  const today: Conversation[] = [];
  const yesterday: Conversation[] = [];
  const week: Conversation[] = [];
  const older: Conversation[] = [];

  convs.forEach((c) => {
    const diff = (now.getTime() - c.updatedAt.getTime()) / 86400000;
    if (diff < 1) today.push(c);
    else if (diff < 2) yesterday.push(c);
    else if (diff < 7) week.push(c);
    else older.push(c);
  });

  return [
    { label: "Today", items: today },
    { label: "Yesterday", items: yesterday },
    { label: "Past 7 days", items: week },
    { label: "Older", items: older },
  ].filter((g) => g.items.length > 0);
}

/* ─── Sidebar ────────────────────────────────────────────────────────────── */
export function ChatSidebar({
  conversations,
  activeId,
  onSelect,
  onNew,
  mobileOpen,
  onMobileClose,
  user,
}: ChatSidebarProps) {
  const groups = groupConversations(conversations);
  const displayName = user?.name || user?.nickname || user?.email || "User";
  const initial = displayName.charAt(0).toUpperCase();

  return (
    <>
      {/* Mobile overlay */}
      {mobileOpen && (
        <div
          className="fixed inset-0 bg-black/60 z-20 md:hidden"
          onClick={onMobileClose}
        />
      )}

      {/* Sidebar panel */}
      <aside
        className={cn(
          "fixed md:relative z-30 md:z-auto inset-y-0 left-0",
          "w-60 flex flex-col h-full",
          "bg-[#060606] border-r border-stone-900",
          "transition-transform duration-300 ease-in-out",
          mobileOpen ? "translate-x-0" : "-translate-x-full md:translate-x-0"
        )}
      >
        {/* ── Top: Logo ── */}
        <div className="flex items-center justify-between px-4 py-4 border-b border-stone-900">
          <div className="flex items-center gap-2.5">
            <div className="w-6 h-6 flex items-center justify-center">
              {/* Amber star mark */}
              <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5">
                <path
                  d="M12 2l2.8 6.2L21 9l-4.5 4.4 1 6.6L12 17l-5.5 3 1-6.6L3 9l6.2-.8L12 2z"
                  fill="#F59E0B"
                  fillOpacity="0.9"
                />
              </svg>
            </div>
            <span className="font-display font-bold text-sm tracking-wide text-stone-100">
              LYNX
            </span>
          </div>

          {/* Close on mobile */}
          <button
            onClick={onMobileClose}
            className="md:hidden p-1 rounded text-stone-600 hover:text-stone-300 transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* ── New chat button ── */}
        <div className="px-3 py-3">
          <button
            onClick={onNew}
            className={cn(
              "w-full flex items-center gap-2 px-3 py-2 rounded-sm",
              "border border-dashed border-stone-800",
              "text-stone-500 text-xs font-mono",
              "hover:border-amber-500/40 hover:text-amber-400 hover:bg-stone-900/60",
              "transition-all duration-150 cursor-pointer"
            )}
          >
            <PlusIcon className="w-3.5 h-3.5" />
            New conversation
          </button>
        </div>

        {/* ── Conversation list ── */}
        <div className="flex-1 overflow-y-auto px-2 pb-2 scrollbar-thin scrollbar-thumb-stone-800 scrollbar-track-transparent">
          {groups.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full gap-2 py-8">
              <MessageSquare className="w-6 h-6 text-stone-800" />
              <p className="text-[10px] font-mono text-stone-700 text-center leading-relaxed">
                No conversations yet.
                <br />
                Start a new one above.
              </p>
            </div>
          ) : (
            groups.map((group) => (
              <div key={group.label} className="mb-3">
                {/* Group label */}
                <p className="text-[9px] font-mono uppercase tracking-[0.2em] text-stone-700 px-2 py-1.5">
                  {group.label}
                </p>

                {/* Items */}
                {group.items.map((conv) => {
                  const isActive = conv.id === activeId;
                  return (
                    <button
                      key={conv.id}
                      onClick={() => onSelect(conv.id)}
                      className={cn(
                        "group w-full flex items-start gap-2 px-2 py-2 rounded-sm text-left",
                        "transition-all duration-150 cursor-pointer",
                        isActive
                          ? "bg-stone-900/80 border-l-2 border-amber-400 pl-[6px]"
                          : "border-l-2 border-transparent hover:bg-stone-900/40 hover:border-stone-700"
                      )}
                    >
                      <div className="flex-1 min-w-0">
                        <p
                          className={cn(
                            "text-xs font-mono truncate leading-tight",
                            isActive ? "text-stone-100" : "text-stone-400 group-hover:text-stone-200"
                          )}
                        >
                          {conv.title}
                        </p>
                        <p className="text-[10px] font-mono text-stone-700 truncate mt-0.5">
                          {conv.preview}
                        </p>
                      </div>
                      <ChevronRight
                        className={cn(
                          "w-3 h-3 flex-shrink-0 mt-0.5 transition-opacity",
                          isActive
                            ? "text-amber-400 opacity-100"
                            : "text-stone-700 opacity-0 group-hover:opacity-100"
                        )}
                      />
                    </button>
                  );
                })}
              </div>
            ))
          )}
        </div>

        {/* ── Bottom: user row ── */}
        <div className="px-3 py-3 border-t border-stone-900">
          <div className="flex items-center gap-2.5">
            {/* Avatar */}
            <div className="w-6 h-6 rounded-sm bg-amber-500/20 border border-amber-500/30 flex items-center justify-center flex-shrink-0 overflow-hidden">
              {user?.picture ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={user.picture} alt={displayName} className="w-full h-full object-cover" />
              ) : (
                <span className="text-[10px] font-mono font-bold text-amber-400">{initial}</span>
              )}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-xs font-mono text-stone-300 truncate">{displayName}</p>
              <p className="text-[9px] font-mono text-stone-700 truncate">Free plan</p>
            </div>
            <button className="p-1 rounded text-stone-700 hover:text-stone-400 transition-colors cursor-pointer">
              <Settings className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </aside>
    </>
  );
}
