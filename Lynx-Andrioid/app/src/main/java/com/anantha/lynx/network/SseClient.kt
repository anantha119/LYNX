package com.anantha.lynx.network

import com.anantha.lynx.model.SendMessageBody
import com.anantha.lynx.model.SseEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class SseException(val code: Int) : Exception("SSE request failed with HTTP $code")

/**
 * Streams the SSE response body of POST /v1/conversations/:id/messages and
 * yields decoded SseEvents as they arrive. Ported from
 * Lynx-IOS/Lynx-IOS/Networking/SSEClient.swift.
 */
object SseClient {
    // A dedicated client with no read timeout — the default would kill a
    // long-lived streaming response.
    private val streamingHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    fun stream(conversationId: String, content: String, accessToken: String): Flow<SseEvent> = callbackFlow {
        val body = json.encodeToString(SendMessageBody.serializer(), SendMessageBody(content))
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(
                AppConfig.API_BASE_URL.toHttpUrl().newBuilder()
                    .addPathSegments("v1/conversations/$conversationId/messages")
                    .build()
            )
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body)
            .build()

        val call = streamingHttpClient.newCall(request)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                close(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        close(SseException(resp.code))
                        return
                    }

                    val parser = SseParser()
                    val source = resp.body.source()
                    try {
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            for (event in parser.feed("$line\n")) {
                                trySend(event)
                            }
                        }
                        close()
                    } catch (e: IOException) {
                        close(e)
                    }
                }
            }
        })

        awaitClose { call.cancel() }
    }.flowOn(Dispatchers.IO)
}
