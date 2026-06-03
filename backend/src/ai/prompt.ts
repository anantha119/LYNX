import { langfuse } from "./config.js";

/**
 * Keeps the system prompt in memory so chat requests never wait on a
 * network call to Langfuse. The prompt is fetched once at startup and
 * refreshed on a timer in the background.
 */

const FALLBACK_SYSTEM_PROMPT =
  "You are Lynx, a sharp and concise AI assistant focused on helping engineers ship software. Be direct, precise, and practical.";

const REFRESH_INTERVAL_MS = 5 * 60 * 1000; // 5 minutes

// eslint-disable-next-line @typescript-eslint/no-explicit-any
let cachedPrompt: any | undefined; // Langfuse TextPromptClient (used for tracing)
let cachedText = FALLBACK_SYSTEM_PROMPT;
let refreshTimer: ReturnType<typeof setInterval> | undefined;

/** Fetch the latest prompt from Langfuse and update the in-memory copy. */
async function refresh(): Promise<void> {
  try {
    const prompt = await langfuse.getPrompt("lynx-system", undefined, {
      type: "text",
    });
    cachedPrompt = prompt;
    cachedText = prompt.compile() as string;
  } catch (err) {
    // Keep whatever we already have (last good value or the fallback).
    console.warn("[prompt] refresh failed, keeping current prompt:", err);
  }
}

/**
 * Load the prompt once at server startup. Call before serving traffic so the
 * first request already has a real prompt. Also starts the background refresh.
 */
export async function initSystemPrompt(): Promise<void> {
  await refresh();
  refreshTimer = setInterval(refresh, REFRESH_INTERVAL_MS);
  refreshTimer.unref?.(); // don't keep the process alive just for this timer
}

/** The current system prompt — read instantly, no network, no await. */
export function getSystemPrompt(): {
  text: string;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  prompt: any | undefined;
} {
  return { text: cachedText, prompt: cachedPrompt };
}
