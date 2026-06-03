"use client";

import { useEffect, useRef, useCallback, useState } from "react";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "@/lib/utils";
import {
  ImageIcon,
  FileUp,
  Frame,
  MonitorIcon,
  CircleUserRound,
  ArrowUpIcon,
  Paperclip,
  PlusIcon,
} from "lucide-react";

/* ─── Auto-resize hook ───────────────────────────────────────────────────── */
interface UseAutoResizeTextareaProps {
  minHeight: number;
  maxHeight?: number;
}

function useAutoResizeTextarea({ minHeight, maxHeight }: UseAutoResizeTextareaProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const adjustHeight = useCallback(
    (reset?: boolean) => {
      const textarea = textareaRef.current;
      if (!textarea) return;
      if (reset) {
        textarea.style.height = `${minHeight}px`;
        return;
      }
      textarea.style.height = `${minHeight}px`;
      const newHeight = Math.max(
        minHeight,
        Math.min(textarea.scrollHeight, maxHeight ?? Number.POSITIVE_INFINITY)
      );
      textarea.style.height = `${newHeight}px`;
    },
    [minHeight, maxHeight]
  );

  useEffect(() => {
    const textarea = textareaRef.current;
    if (textarea) textarea.style.height = `${minHeight}px`;
  }, [minHeight]);

  useEffect(() => {
    const handleResize = () => adjustHeight();
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, [adjustHeight]);

  return { textareaRef, adjustHeight };
}

/* ─── Main component ─────────────────────────────────────────────────────── */
interface VercelV0ChatProps {
  /** Called with the trimmed message text when user hits Send / Enter */
  onSend?: (message: string) => void;
  /** Show the hero headline + action chips (landing / empty state) */
  showHero?: boolean;
  /** Placeholder override for in-conversation use */
  placeholder?: string;
}

export function VercelV0Chat({
  onSend,
  showHero = true,
  placeholder,
}: VercelV0ChatProps) {
  const [value, setValue] = useState("");
  const [mounted, setMounted] = useState(false);
  const { textareaRef, adjustHeight } = useAutoResizeTextarea({
    minHeight: 60,
    maxHeight: 200,
  });

  useEffect(() => {
    const t = setTimeout(() => setMounted(true), 50);
    return () => clearTimeout(t);
  }, []);

  const submit = () => {
    if (!value.trim()) return;
    onSend?.(value.trim());
    setValue("");
    adjustHeight(true);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      submit();
    }
  };

  return (
    <div
      className={cn(
        "flex flex-col w-full transition-all duration-700",
        showHero
          ? "items-center max-w-3xl mx-auto px-4 gap-10 py-12"
          : "px-4 gap-0 py-4",
        mounted ? "opacity-100 translate-y-0" : "opacity-0 translate-y-6"
      )}
    >
      {/* Headline — only in hero / empty state */}
      {showHero && (
        <div className="text-center space-y-3">
          <h1 className="text-4xl sm:text-5xl font-display font-bold text-stone-100 leading-tight">
            What can I help
            <br />
            <span className="text-amber-400">you ship?</span>
          </h1>
        </div>
      )}

      {/* Input panel */}
      <div className="w-full">
        {/* Terminal chrome border */}
        <div className="relative">
          {/* Corner marks */}
          <span className="absolute -top-px -left-px w-3 h-3 border-t border-l border-amber-500/60 z-10" />
          <span className="absolute -top-px -right-px w-3 h-3 border-t border-r border-amber-500/60 z-10" />
          <span className="absolute -bottom-px -left-px w-3 h-3 border-b border-l border-amber-500/60 z-10" />
          <span className="absolute -bottom-px -right-px w-3 h-3 border-b border-r border-amber-500/60 z-10" />

          <div className="relative bg-[#0e0e0e] border border-stone-800 rounded-sm overflow-hidden">
            {/* Scanline overlay */}
            <div
              className="pointer-events-none absolute inset-0 z-0 opacity-[0.03]"
              style={{
                backgroundImage:
                  "repeating-linear-gradient(0deg, transparent, transparent 2px, #fff 2px, #fff 3px)",
                backgroundSize: "100% 3px",
              }}
            />

            {/* Textarea */}
            <div className="relative z-10 overflow-y-auto">
              <Textarea
                ref={textareaRef}
                value={value}
                onChange={(e) => {
                  setValue(e.target.value);
                  adjustHeight();
                }}
                onKeyDown={handleKeyDown}
                placeholder={placeholder ?? "Describe what you want to build…"}
                className={cn(
                  "w-full px-5 py-4",
                  "resize-none bg-transparent border-none",
                  "text-stone-200 text-sm font-mono leading-relaxed",
                  "focus:outline-none focus-visible:ring-0 focus-visible:ring-offset-0",
                  "placeholder:text-stone-600 placeholder:text-sm placeholder:font-mono",
                  "min-h-[60px]",
                  "caret-amber-400"
                )}
                style={{ overflow: "hidden" }}
              />
            </div>

            {/* Toolbar */}
            <div className="relative z-10 flex items-center justify-between px-3 py-2.5 border-t border-stone-800/80">
              <div className="flex items-center gap-1">
                <ToolButton icon={<Paperclip className="w-3.5 h-3.5" />} label="Attach" />
              </div>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  className="flex items-center gap-1.5 px-2.5 py-1 rounded-sm text-xs text-stone-500 border border-dashed border-stone-700 hover:border-stone-500 hover:text-stone-300 transition-all duration-150 font-mono"
                >
                  <PlusIcon className="w-3 h-3" />
                  Project
                </button>

                {/* Send button */}
                <button
                  type="button"
                  onClick={submit}
                  className={cn(
                    "w-7 h-7 rounded-sm flex items-center justify-center transition-all duration-200 border cursor-pointer",
                    value.trim()
                      ? "bg-amber-400 border-amber-400 text-black shadow-[0_0_16px_rgba(251,191,36,0.4)]"
                      : "border-stone-700 text-stone-600 cursor-not-allowed"
                  )}
                  aria-label="Send"
                >
                  <ArrowUpIcon className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Char count hint */}
        {value.length > 0 && (
          <p className="mt-2 text-right text-[10px] font-mono text-stone-600 pr-1 transition-opacity">
            {value.length} chars · ⏎ to send
          </p>
        )}

        {/* Action chips — only shown in hero mode */}
        {showHero && (
          <div className="flex flex-wrap items-center justify-center gap-2 mt-6">
            <ActionButton
              icon={<ImageIcon className="w-3.5 h-3.5" />}
              label="Clone a Screenshot"
              onClick={() => onSend?.("Clone a screenshot for me")}
            />
            <ActionButton
              icon={<Frame className="w-3.5 h-3.5" />}
              label="Import from Figma"
              onClick={() => onSend?.("Import a Figma design")}
            />
            <ActionButton
              icon={<FileUp className="w-3.5 h-3.5" />}
              label="Upload a Project"
              onClick={() => onSend?.("Upload and analyze my project")}
            />
            <ActionButton
              icon={<MonitorIcon className="w-3.5 h-3.5" />}
              label="Landing Page"
              onClick={() => onSend?.("Build me a landing page")}
            />
            <ActionButton
              icon={<CircleUserRound className="w-3.5 h-3.5" />}
              label="Sign Up Form"
              onClick={() => onSend?.("Build me a sign up form")}
            />
          </div>
        )}
      </div>
    </div>
  );
}

/* ─── Sub-components ─────────────────────────────────────────────────────── */
interface ActionButtonProps {
  icon: React.ReactNode;
  label: string;
  onClick?: () => void;
}

function ActionButton({ icon, label, onClick }: ActionButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "group flex items-center gap-2 px-3.5 py-1.5",
        "bg-[#0e0e0e] border border-stone-800 rounded-sm",
        "text-stone-500 text-xs font-mono cursor-pointer",
        "hover:border-amber-500/40 hover:text-amber-400 hover:bg-stone-900",
        "transition-all duration-200"
      )}
    >
      <span className="group-hover:text-amber-400 transition-colors">{icon}</span>
      {label}
    </button>
  );
}

interface ToolButtonProps {
  icon: React.ReactNode;
  label: string;
}

function ToolButton({ icon, label }: ToolButtonProps) {
  return (
    <button
      type="button"
      className="group flex items-center gap-1 p-1.5 rounded-sm text-stone-600 hover:text-stone-300 hover:bg-stone-800/60 transition-all duration-150"
    >
      {icon}
      <span className="text-[10px] font-mono opacity-0 group-hover:opacity-100 transition-opacity max-w-0 group-hover:max-w-xs overflow-hidden">
        {label}
      </span>
    </button>
  );
}
