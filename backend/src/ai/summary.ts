import { ai, CHAT_MODEL } from "./config.js";
import { getMessages } from "../db/messages.js";
import { insertSummary, getLatestSummary } from "../db/summaries.js";

export async function generateSummaryAsync(conversationId: string, coversThroughId: string) {
  try {
    // 1. Fetch all messages up to the coversThroughId
    const allMessages = await getMessages(conversationId);
    const messagesToSummarize = allMessages.filter(m => m.id <= coversThroughId && (m.role === "user" || m.role === "assistant"));
    
    if (messagesToSummarize.length === 0) return;

    // 2. Fetch the previous summary (if any) to include in the context
    const latestSummary = await getLatestSummary(conversationId);

    // 3. Construct the summarization prompt
    let prompt = `You are a helpful AI assistant tasked with summarizing a conversation.
Your goal is to compress the conversation into a concise summary that retains all the important context, facts, decisions, and tone, so that an AI can read your summary and perfectly continue the conversation.

`;
    if (latestSummary) {
      prompt += `Here is the existing summary of the oldest messages:\n${latestSummary.summary}\n\n`;
    }

    prompt += `Here are the more recent messages to append/integrate into the summary:\n`;
    for (const msg of messagesToSummarize) {
      if (latestSummary && msg.id <= latestSummary.covers_through_id) continue;
      const text = msg.content.map(p => p.text).join("");
      prompt += `[${msg.role.toUpperCase()}]: ${text}\n`;
    }

    // 4. Call Gemini to generate the new summary
    const response = await ai.models.generateContent({
      model: CHAT_MODEL,
      contents: prompt,
      config: { temperature: 0.2 },
    });

    const newSummary = response.text ?? "";
    const tokenResponse = await ai.models.countTokens({ model: CHAT_MODEL, contents: newSummary });
    const totalTokens = tokenResponse.totalTokens ?? 0;

    // 5. Save the new summary to the DB
    await insertSummary(conversationId, coversThroughId, newSummary, totalTokens);
    console.log(`[Summary] Generated rolling summary for conv ${conversationId} through ${coversThroughId}. Tokens: ${totalTokens}`);

  } catch (err) {
    console.error("[Summary] Async summarization failed:", err);
  }
}
