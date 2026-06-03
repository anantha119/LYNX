"use client";

import { useEffect, useRef } from "react";
import { cn } from "@/lib/utils";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import remarkMath from "remark-math";
import rehypeKatex from "rehype-katex";
import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
import { vscDarkPlus } from "react-syntax-highlighter/dist/esm/styles/prism";
import "katex/dist/katex.min.css";

export type Message = {
  id: string;
  role: "user" | "assistant";
  content: string;
  timestamp: Date;
  streaming?: boolean;
};

interface ChatMessagesProps {
  messages: Message[];
}

function formatTime(date: Date) {
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

/* ─── Markdown renderer ──────────────────────────────────────────────────── */
function MarkdownContent({ content, streaming }: { content: string; streaming?: boolean }) {
  return (
    <div className="prose-lynx">
      <ReactMarkdown
        remarkPlugins={[remarkGfm, remarkMath]}
        rehypePlugins={[rehypeKatex]}
        components={{
          // Code blocks
          code({ className, children, ...props }) {
            const match = /language-(\w+)/.exec(className || "");
            const isBlock = !!match;
            if (isBlock) {
              return (
                <SyntaxHighlighter
                  style={vscDarkPlus}
                  language={match[1]}
                  PreTag="div"
                  customStyle={{
                    margin: "0.75rem 0",
                    borderRadius: "2px",
                    border: "1px solid rgb(41 37 36)",
                    background: "#0a0a0a",
                    fontSize: "12px",
                    lineHeight: "1.6",
                  }}
                  codeTagProps={{ style: { fontFamily: "monospace" } }}
                >
                  {String(children).replace(/\n$/, "")}
                </SyntaxHighlighter>
              );
            }
            return (
              <code
                className="px-1.5 py-0.5 rounded-sm bg-stone-800 border border-stone-700 text-amber-300 text-[11px] font-mono"
                {...props}
              >
                {children}
              </code>
            );
          },
          // Headings
          h1: ({ children }) => (
            <h1 className="text-base font-bold text-stone-100 font-mono mt-4 mb-2 border-b border-stone-800 pb-1">
              {children}
            </h1>
          ),
          h2: ({ children }) => (
            <h2 className="text-sm font-bold text-stone-100 font-mono mt-4 mb-2">
              {children}
            </h2>
          ),
          h3: ({ children }) => (
            <h3 className="text-sm font-semibold text-stone-200 font-mono mt-3 mb-1">
              {children}
            </h3>
          ),
          // Paragraphs
          p: ({ children }) => (
            <p className="text-sm font-mono text-stone-200 leading-relaxed mb-3 last:mb-0">
              {children}
            </p>
          ),
          // Lists
          ul: ({ children }) => (
            <ul className="my-2 space-y-1 pl-4">{children}</ul>
          ),
          ol: ({ children }) => (
            <ol className="my-2 space-y-1 pl-4 list-decimal list-inside">{children}</ol>
          ),
          li: ({ children }) => (
            <li className="text-sm font-mono text-stone-300 leading-relaxed flex gap-2">
              <span className="text-amber-500 flex-shrink-0 mt-0.5">›</span>
              <span>{children}</span>
            </li>
          ),
          // Bold / italic
          strong: ({ children }) => (
            <strong className="font-bold text-stone-100">{children}</strong>
          ),
          em: ({ children }) => (
            <em className="italic text-stone-300">{children}</em>
          ),
          // Horizontal rule
          hr: () => <hr className="border-stone-800 my-4" />,
          // Blockquote
          blockquote: ({ children }) => (
            <blockquote className="border-l-2 border-amber-500/60 pl-3 my-3 text-stone-400 italic">
              {children}
            </blockquote>
          ),
          // Links
          a: ({ href, children }) => (
            <a
              href={href}
              target="_blank"
              rel="noopener noreferrer"
              className="text-amber-400 hover:text-amber-300 underline underline-offset-2"
            >
              {children}
            </a>
          ),
          // Tables
          table: ({ children }) => (
            <div className="overflow-x-auto my-3">
              <table className="w-full text-xs font-mono border border-stone-800">
                {children}
              </table>
            </div>
          ),
          th: ({ children }) => (
            <th className="px-3 py-1.5 text-left text-stone-300 bg-stone-900 border-b border-stone-800 font-semibold">
              {children}
            </th>
          ),
          td: ({ children }) => (
            <td className="px-3 py-1.5 text-stone-400 border-b border-stone-900">
              {children}
            </td>
          ),
        }}
      >
        {content}
      </ReactMarkdown>
      {streaming && (
        <span className="inline-block w-[2px] h-[14px] bg-amber-400 ml-0.5 align-middle animate-pulse" />
      )}
    </div>
  );
}

/* ─── Message list ───────────────────────────────────────────────────────── */
export function ChatMessages({ messages }: ChatMessagesProps) {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  return (
    <div className="flex-1 overflow-y-auto px-4 lg:px-8 py-6 space-y-6">
      {messages.map((msg, i) => (
        <div
          key={msg.id}
          className={cn(
            "flex gap-3 transition-all duration-300",
            msg.role === "user" ? "flex-row-reverse" : "flex-row",
            "animate-in fade-in slide-in-from-bottom-2"
          )}
          style={{ animationDelay: `${i * 30}ms`, animationFillMode: "both" }}
        >
          {/* Avatar */}
          <div className="flex-shrink-0 mt-0.5">
            {msg.role === "assistant" ? (
              <div className="w-6 h-6 flex items-center justify-center">
                <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5">
                  <path
                    d="M12 2l2.8 6.2L21 9l-4.5 4.4 1 6.6L12 17l-5.5 3 1-6.6L3 9l6.2-.8L12 2z"
                    fill="#F59E0B"
                    fillOpacity="0.85"
                  />
                </svg>
              </div>
            ) : (
              <div className="w-6 h-6 rounded-sm bg-stone-800 border border-stone-700 flex items-center justify-center">
                <span className="text-[9px] font-mono font-bold text-stone-300">H</span>
              </div>
            )}
          </div>

          {/* Bubble */}
          <div
            className={cn(
              "flex flex-col gap-1",
              msg.role === "user"
                ? "max-w-[72%] lg:max-w-[60%] items-end"
                : "flex-1 min-w-0 items-start"
            )}
          >
            <div
              className={cn(
                "rounded-sm text-sm font-mono leading-relaxed",
                msg.role === "user"
                  ? "px-4 py-3 bg-stone-800 border border-stone-700 text-stone-100"
                  : "w-full border-l-2 border-amber-500/60 pl-4 pr-2 py-2"
              )}
            >
              {msg.role === "assistant" ? (
                <MarkdownContent content={msg.content} streaming={msg.streaming} />
              ) : (
                <>
                  {msg.content}
                </>
              )}
            </div>
            <span className="text-[9px] font-mono text-stone-700 px-1">
              {formatTime(msg.timestamp)}
            </span>
          </div>
        </div>
      ))}
      <div ref={bottomRef} />
    </div>
  );
}
