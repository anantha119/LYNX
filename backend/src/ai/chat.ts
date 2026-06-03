import { ai, CHAT_MODEL, langfuse } from "./config.js";
import { getSystemPrompt } from "./prompt.js";

export type ChatMessage = {
  role: "user" | "assistant";
  content: string;
};

/**
 * Streams a Gemini reply for a conversation turn.
 * - Reads the system prompt from the in-memory cache (no per-request network call)
 * - Traces the generation in Langfuse for observability
 * - Calls onToken for each text chunk as it arrives
 * - Returns the full completed text
 */
export async function streamChat(
  history: ChatMessage[],
  onToken: (token: string) => void | Promise<void>
): Promise<string> {
  // System prompt is loaded at startup and refreshed in the background,
  // so reading it here is instant and never blocks the response.
  const { text: systemInstruction, prompt: langfusePrompt } = getSystemPrompt();

  // Start a Langfuse trace — all observability is fire-and-forget so it
  // can never block or hang the streaming response
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let generation: any | undefined;
  try {
    const trace = langfuse.trace({ name: "chat" });
    generation = trace.generation({
      name: "gemini-stream",
      model: CHAT_MODEL,
      input: history,
      ...(langfusePrompt ? { prompt: langfusePrompt } : {}),
    });
  } catch (err) {
    console.warn("[Langfuse] trace init failed:", err);
  }

  // Gemini expects the "model" role for assistant turns
  const contents = history.map((m) => ({
    role: m.role === "assistant" ? "model" : "user",
    parts: [{ text: m.content }],
  }));

  const response = await ai.models.generateContentStream({
    model: CHAT_MODEL,
    contents,
    config: {
      systemInstruction,
      maxOutputTokens: 8192,
      temperature: 0.7,
    },
  });

  let fullText = "";
  for await (const chunk of response) {
    const token = chunk.candidates?.[0]?.content?.parts?.[0]?.text ?? "";
    if (token) {
      fullText += token;
      await onToken(token);
    }
  }

  // Close the Langfuse generation — fire and forget, never block the response
  try {
    generation?.end({ output: fullText });
    langfuse.flushAsync().catch(() => {}); // intentionally not awaited
  } catch (err) {
    console.warn("[Langfuse] flush failed:", err);
  }

  return fullText;
}
