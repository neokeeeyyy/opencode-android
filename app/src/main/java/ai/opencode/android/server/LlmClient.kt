package ai.opencode.android.server

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmClient @Inject constructor(
    @ApplicationContext val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun chat(messages: List<LlmMessage>, model: String = "claude-sonnet-4-20250514"): String {
        val provider = detectProvider(model)
        val apiKey = getApiKey(provider)
        if (apiKey.isBlank()) throw Exception("API key not configured for $provider")

        return when (provider) {
            "openai" -> callOpenAi(messages, model, apiKey)
            "anthropic" -> callAnthropic(messages, model, apiKey)
            "openrouter" -> callOpenRouter(messages, model, apiKey)
            else -> throw Exception("Unknown provider for model: $model")
        }
    }

    private fun detectProvider(model: String): String {
        return when {
            model.startsWith("gpt-") || model.startsWith("o1") || model.startsWith("o3") -> "openai"
            model.startsWith("claude-") -> "anthropic"
            model.contains("/") -> "openrouter"
            else -> "anthropic"
        }
    }

    private fun getApiKey(provider: String): String {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return prefs.getString("api_key_$provider", "") ?: ""
    }

    private fun callOpenAi(messages: List<LlmMessage>, model: String, apiKey: String): String {
        val body = buildJsonObject {
            put("model", model)
            put("max_tokens", 4096)
            putJsonArray("messages") {
                messages.forEach { msg ->
                    addJsonObject {
                        put("role", msg.role)
                        put("content", msg.content)
                    }
                }
            }
        }

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("OpenAI API error: ${response.code}")

        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        val json = Json.parseToJsonElement(responseBody).jsonObject
        return json["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonObject
            ?.get("content")?.jsonPrimitive?.content
            ?: throw Exception("Invalid response format")
    }

    private fun callAnthropic(messages: List<LlmMessage>, model: String, apiKey: String): String {
        val systemMsg = messages.lastOrNull { it.role == "system" }?.content
        val chatMessages = messages.filter { it.role != "system" }

        val body = buildJsonObject {
            put("model", model)
            put("max_tokens", 4096)
            systemMsg?.let { put("system", it) }
            putJsonArray("messages") {
                chatMessages.forEach { msg ->
                    addJsonObject {
                        put("role", msg.role)
                        put("content", msg.content)
                    }
                }
            }
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Anthropic API error: ${response.code}")

        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        val json = Json.parseToJsonElement(responseBody).jsonObject
        return json["content"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("text")?.jsonPrimitive?.content
            ?: throw Exception("Invalid response format")
    }

    private fun callOpenRouter(messages: List<LlmMessage>, model: String, apiKey: String): String {
        val body = buildJsonObject {
            put("model", model)
            put("max_tokens", 4096)
            putJsonArray("messages") {
                messages.forEach { msg ->
                    addJsonObject {
                        put("role", msg.role)
                        put("content", msg.content)
                    }
                }
            }
        }

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("OpenRouter API error: ${response.code}")

        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        val json = Json.parseToJsonElement(responseBody).jsonObject
        return json["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonObject
            ?.get("content")?.jsonPrimitive?.content
            ?: throw Exception("Invalid response format")
    }
}

@Serializable
data class LlmMessage(
    val role: String,
    val content: String
)
