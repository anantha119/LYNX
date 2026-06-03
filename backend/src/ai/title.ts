import { ai, LITE_MODEL } from "./config.js";

/**
 * Generates a short conversation title from the user's first message.
 * Returns a plain string, max ~50 chars, no quotes or punctuation.
 */
export async function generateTitle(firstUserMessage: string): Promise<string> {
  const prompt = `Generate a short, descriptive title (max 6 words, no quotes, no punctuation at the end) for a conversation that starts with this message:

"${firstUserMessage}"

Reply with only the title, nothing else.`;

  const response = await ai.models.generateContent({
    model: LITE_MODEL,
    contents: [{ role: "user", parts: [{ text: prompt }] }],
    config: { maxOutputTokens: 64, temperature: 0.3 },
  });

  const title = response.candidates?.[0]?.content?.parts?.[0]?.text?.trim() ?? "";

  // Fallback to truncated user message if model returns empty
  return title || firstUserMessage.slice(0, 50);
}
