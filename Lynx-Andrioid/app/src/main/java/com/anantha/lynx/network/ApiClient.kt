package com.anantha.lynx.network

import com.anantha.lynx.model.ChatMessage
import com.anantha.lynx.model.Conversation
import com.anantha.lynx.model.ConversationListResponse
import com.anantha.lynx.model.MessagesPageResponse
import com.anantha.lynx.model.ServerConversation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class ApiError : Exception() {
    data class Http(val code: Int) : ApiError()
    object NotFound : ApiError()
    data class Decoding(override val cause: Throwable) : ApiError()
}

/**
 * REST client for the Lynx backend (see backend/src/index.ts). A native app
 * is not subject to CORS, so this talks to Cloud Run directly.
 * Ported from Lynx-IOS/Lynx-IOS/Networking/APIClient.swift.
 */
class ApiClient(private val accessToken: String) {

    private val json = Json { ignoreUnknownKeys = true }

    private fun authorizedRequest(path: String, method: String = "GET"): Request.Builder =
        Request.Builder()
            .url(AppConfig.API_BASE_URL.toHttpUrl().newBuilder().addPathSegments(path.trimStart('/')).build())
            .method(method, null)
            .addHeader("Authorization", "Bearer $accessToken")

    suspend fun listConversations(): List<Conversation> {
        val response = execute(authorizedRequest("v1/conversations").build())
        val decoded = decode<ConversationListResponse>(response)
        return decoded.data.map(::Conversation)
    }

    suspend fun createConversation(): Conversation {
        val response = execute(authorizedRequest("v1/conversations", "POST").build())
        val decoded = decode<ServerConversation>(response)
        return Conversation(decoded)
    }

    data class MessagePage(
        val messages: List<ChatMessage>,
        val nextCursor: String?,
        val hasMore: Boolean,
    )

    suspend fun getMessages(conversationId: String, limit: Int = 50, before: String? = null): MessagePage {
        val urlBuilder = AppConfig.API_BASE_URL.toHttpUrl().newBuilder()
            .addPathSegments("v1/conversations/$conversationId/messages")
            .addQueryParameter("limit", limit.toString())
        if (before != null) urlBuilder.addQueryParameter("before", before)

        val request = Request.Builder()
            .url(urlBuilder.build())
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = execute(request)
        val decoded = decode<MessagesPageResponse>(response)
        return MessagePage(
            messages = decoded.data.map(::ChatMessage),
            nextCursor = decoded.nextCursor,
            hasMore = decoded.hasMore,
        )
    }

    private suspend fun execute(request: Request): Response {
        val response = sharedHttpClient.newCall(request).await()
        if (response.code == 404) throw ApiError.NotFound
        if (response.code !in 200..299) throw ApiError.Http(response.code)
        return response
    }

    private inline fun <reified T> decode(response: Response): T {
        val body = response.use { it.body.string() }
        return try {
            json.decodeFromString(body)
        } catch (e: Exception) {
            throw ApiError.Decoding(e)
        }
    }

    companion object {
        val sharedHttpClient: OkHttpClient = OkHttpClient.Builder().build()
    }
}

suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }
    })
    continuation.invokeOnCancellation { cancel() }
}
