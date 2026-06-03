import "dotenv/config";
import { GoogleGenAI } from "@google/genai";
import { Langfuse } from "langfuse";

// ── Gemini ────────────────────────────────────────────────────────────────
const apiKey = process.env.GEMINI_API_KEY;
if (!apiKey) throw new Error("GEMINI_API_KEY is not set");

export const ai = new GoogleGenAI({ apiKey });

// Primary model — streaming chat responses
export const CHAT_MODEL = "gemini-3.1-flash-lite";

// Lightweight model — background tasks (title generation, rolling summaries)
export const LITE_MODEL = "gemini-3.1-flash-lite";

// ── Langfuse ──────────────────────────────────────────────────────────────
export const langfuse = new Langfuse({
  secretKey: process.env.LANGFUSE_SECRET_KEY,
  publicKey: process.env.LANGFUSE_PUBLIC_KEY,
  baseUrl: process.env.LANGFUSE_BASE_URL,
});
