package ai.opencode.android.data.api

import ai.opencode.android.data.model.Config
import ai.opencode.android.data.model.Health
import ai.opencode.android.data.model.Message
import ai.opencode.android.data.model.Part
import ai.opencode.android.data.model.Provider
import ai.opencode.android.data.model.Session
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenCodeApi @Inject constructor(
    private val client: OpenCodeClient,
) {
    private fun url(path: String): String {
        val base = client.serverUrl.value ?: throw IllegalStateException("Not connected")
        return "$base$path"
    }

    // Health
    suspend fun health(): Health {
        return client.getHttpClient().get(url("/global/health")).body()
    }

    // Sessions (v1 endpoints)
    suspend fun listSessions(): List<Session> {
        return client.getHttpClient().get(url("/session")).body()
    }

    suspend fun createSession(): Session {
        return client.getHttpClient().post(url("/session")).body()
    }

    suspend fun getSession(id: String): Session {
        return client.getHttpClient().get(url("/session/$id")).body()
    }

    suspend fun updateSession(id: String, title: String? = null): Session {
        return client.getHttpClient().patch(url("/session/$id")) {
            contentType(ContentType.Application.Json)
            val body = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
            if (title != null) body["title"] = JsonPrimitive(title)
            setBody(JsonObject(body))
        }.body()
    }

    suspend fun deleteSession(id: String) {
        client.getHttpClient().delete(url("/session/$id"))
    }

    // Messages (v1 endpoints)
    suspend fun listMessages(sessionId: String): List<Message> {
        return client.getHttpClient().get(url("/session/$sessionId/message")).body()
    }

    suspend fun getMessage(sessionId: String, messageId: String): Message {
        return client.getHttpClient().get(url("/session/$sessionId/message/$messageId")).body()
    }

    suspend fun sendMessage(sessionId: String, content: String): Message {
        return client.getHttpClient().post(url("/session/$sessionId/message")) {
            contentType(ContentType.Application.Json)
            setBody(JsonObject(mapOf(
                "parts" to JsonArray(listOf(
                    JsonObject(mapOf(
                        "type" to JsonPrimitive("text"),
                        "text" to JsonPrimitive(content),
                    ))
                ))
            )))
        }.body()
    }

    suspend fun abortSession(sessionId: String) {
        client.getHttpClient().post(url("/session/$sessionId/abort"))
    }

    // Config (v1 endpoints)
    suspend fun getConfig(): Config {
        return client.getHttpClient().get(url("/config")).body()
    }

    suspend fun updateConfig(config: Config) {
        client.getHttpClient().patch(url("/config")) {
            contentType(ContentType.Application.Json)
            setBody(config)
        }
    }

    // Providers
    suspend fun listProviders(): List<Provider> {
        return client.getHttpClient().get(url("/provider")).body()
    }

    // Agents
    suspend fun listAgents(): JsonArray {
        return client.getHttpClient().get(url("/agent")).body()
    }

    // Skills
    suspend fun listSkills(): JsonArray {
        return client.getHttpClient().get(url("/skill")).body()
    }

    // Files
    suspend fun readFile(path: String): String {
        return client.getHttpClient().get(url("/file/content")) {
            parameter("path", path)
        }.body()
    }

    suspend fun listFiles(path: String): JsonArray {
        return client.getHttpClient().get(url("/file")) {
            parameter("path", path)
        }.body()
    }

    // Tools
    suspend fun listToolIds(): JsonArray {
        return client.getHttpClient().get(url("/experimental/tool/ids")).body()
    }

    // Permissions (v1)
    suspend fun replyPermission(requestId: String, reply: String) {
        client.getHttpClient().post(url("/permission/$requestId/reply")) {
            contentType(ContentType.Application.Json)
            setBody(JsonObject(mapOf("reply" to JsonPrimitive(reply))))
        }
    }

    // Questions (v1)
    suspend fun replyQuestion(requestId: String, answers: List<String>) {
        client.getHttpClient().post(url("/question/$requestId/reply")) {
            contentType(ContentType.Application.Json)
            setBody(JsonObject(mapOf(
                "answers" to JsonArray(answers.map { JsonPrimitive(it) })
            )))
        }
    }

    suspend fun rejectQuestion(requestId: String) {
        client.getHttpClient().post(url("/question/$requestId/reject"))
    }
}


