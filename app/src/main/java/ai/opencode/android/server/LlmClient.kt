package ai.opencode.android.server

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class LlmMessage(
    val role: String,
    val content: String
)

data class LlmStreamEvent(
    val type: String,
    val content: String? = null,
    val toolCall: ToolCall? = null,
    val done: Boolean = false
)

@Serializable
data class ToolCall(
    val id: String,
    val name: String,
    val arguments: String
)

@Singleton
class LlmClient @Inject constructor(
    @ApplicationContext val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    companion object {
        val FREE_MODELS = mapOf(
            // Groq (gratis con límites generosos)
            "groq/llama-3.3-70b-versatile" to ModelInfo("Groq Llama 3.3 70B", "groq", free = true),
            "groq/llama-3.1-8b-instant" to ModelInfo("Groq Llama 3.1 8B", "groq", free = true),
            "groq/mixtral-8x7b-32768" to ModelInfo("Groq Mixtral 8x7B", "groq", free = true),
            "groq/gemma2-9b-it" to ModelInfo("Groq Gemma 2 9B", "groq", free = true),

            // Google Gemini (gratis)
            "gemini/gemini-2.0-flash" to ModelInfo("Gemini 2.0 Flash", "gemini", free = true),
            "gemini/gemini-2.5-flash" to ModelInfo("Gemini 2.5 Flash", "gemini", free = true),
            "gemini/gemini-2.5-pro" to ModelInfo("Gemini 2.5 Pro", "gemini", free = true),

            // OpenRouter Free Models
            "openrouter/meta-llama/llama-3.3-70b-instruct:free" to ModelInfo("Llama 3.3 70B Free", "openrouter", free = true),
            "openrouter/meta-llama/llama-3.1-8b-instruct:free" to ModelInfo("Llama 3.1 8B Free", "openrouter", free = true),
            "openrouter/mistralai/mistral-7b-instruct:free" to ModelInfo("Mistral 7B Free", "openrouter", free = true),
            "openrouter/google/gemma-2-9b-it:free" to ModelInfo("Gemma 2 9B Free", "openrouter", free = true),
            "openrouter/qwen/qwen-2.5-72b-instruct:free" to ModelInfo("Qwen 2.5 72B Free", "openrouter", free = true),

            // Big Pickle (modelo del equipo opencode)
            "opencode/big-pickle" to ModelInfo("Big Pickle", "opencode", free = true),

            // Modelos temporales/semanales (ejemplos - se actualizan cada semana)
            "opencode/temp/deepseek-r1" to ModelInfo("DeepSeek R1 (temp)", "opencode", free = true, temporary = true),
            "opencode/temp/qwen-coder" to ModelInfo("Qwen Coder (temp)", "opencode", free = true, temporary = true),

            // Paid models
            "claude-sonnet-4-20250514" to ModelInfo("Claude Sonnet 4", "anthropic", free = false),
            "claude-3-5-haiku-20241022" to ModelInfo("Claude 3.5 Haiku", "anthropic", free = false),
            "gpt-4o" to ModelInfo("GPT-4o", "openai", free = false),
            "gpt-4o-mini" to ModelInfo("GPT-4o Mini", "openai", free = false),
        )
    }

    data class ModelInfo(
        val displayName: String,
        val provider: String,
        val free: Boolean,
        val temporary: Boolean = false
    )

    fun getFreeModels(): Map<String, ModelInfo> {
        return FREE_MODELS.filter { it.value.free }
    }

    fun getAllModels(): Map<String, ModelInfo> = FREE_MODELS

    suspend fun chat(messages: List<LlmMessage>, model: String = "groq/llama-3.3-70b-versatile"): String {
        val provider = detectProvider(model)
        val apiKey = getApiKey(provider)
        if (apiKey.isBlank() && provider != "opencode") throw Exception("API key not configured for $provider")

        return when (provider) {
            "openai" -> callOpenAi(messages, model, apiKey)
            "anthropic" -> callAnthropic(messages, model, apiKey)
            "openrouter" -> callOpenRouter(messages, model, apiKey)
            "groq" -> callGroq(messages, model, apiKey)
            "gemini" -> callGemini(messages, model, apiKey)
            "opencode" -> callOpenCodeFree(messages, model)
            else -> throw Exception("Unknown provider for model: $model")
        }
    }

    fun chatStream(
        messages: List<LlmMessage>,
        model: String = "groq/llama-3.3-70b-versatile",
        onEvent: (LlmStreamEvent) -> Unit
    ) {
        val provider = detectProvider(model)
        val apiKey = getApiKey(provider)

        when (provider) {
            "groq" -> streamGroq(messages, model, apiKey, onEvent)
            "openrouter" -> streamOpenRouter(messages, model, apiKey, onEvent)
            "gemini" -> streamGemini(messages, model, apiKey, onEvent)
            else -> {
                // Fallback: get full response then emit
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val response = chat(messages, model)
                        onEvent(LlmStreamEvent(type = "content", content = response))
                        onEvent(LlmStreamEvent(type = "done", done = true))
                    } catch (e: Exception) {
                        onEvent(LlmStreamEvent(type = "error", content = e.message))
                    }
                }
            }
        }
    }

    private fun detectProvider(model: String): String {
        return when {
            model.startsWith("groq/") -> "groq"
            model.startsWith("gemini/") -> "gemini"
            model.startsWith("openrouter/") -> "openrouter"
            model.startsWith("opencode/") -> "opencode"
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

    // === GROQ (Free) ===
    private fun callGroq(messages: List<LlmMessage>, model: String, apiKey: String): String {
        val actualModel = model.removePrefix("groq/")
        val body = buildJsonObject {
            put("model", actualModel)
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
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Groq API error: ${response.code}")

        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        val json = Json.parseToJsonElement(responseBody).jsonObject
        return json["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonObject
            ?.get("content")?.jsonPrimitive?.content
            ?: throw Exception("Invalid response format")
    }

    private fun streamGroq(messages: List<LlmMessage>, model: String, apiKey: String, onEvent: (LlmStreamEvent) -> Unit) {
        val actualModel = model.removePrefix("groq/")
        val body = buildJsonObject {
            put("model", actualModel)
            put("max_tokens", 4096)
            put("stream", true)
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
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val factory = EventSources.createFactory(client)
        factory.newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    onEvent(LlmStreamEvent(type = "done", done = true))
                    return
                }
                try {
                    val json = Json.parseToJsonElement(data).jsonObject
                    val delta = json["choices"]?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("delta")?.jsonObject
                    val content = delta?.get("content")?.jsonPrimitive?.contentOrNull
                    if (content != null) {
                        onEvent(LlmStreamEvent(type = "content", content = content))
                    }
                } catch (_: Exception) {}
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                onEvent(LlmStreamEvent(type = "error", content = t?.message ?: "Stream failed"))
            }
        })
    }

    // === OPENROUTER ===
    private fun callOpenRouter(messages: List<LlmMessage>, model: String, apiKey: String): String {
        val actualModel = model.removePrefix("openrouter/")
        val body = buildJsonObject {
            put("model", actualModel)
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

    private fun streamOpenRouter(messages: List<LlmMessage>, model: String, apiKey: String, onEvent: (LlmStreamEvent) -> Unit) {
        val actualModel = model.removePrefix("openrouter/")
        val body = buildJsonObject {
            put("model", actualModel)
            put("max_tokens", 4096)
            put("stream", true)
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

        val factory = EventSources.createFactory(client)
        factory.newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    onEvent(LlmStreamEvent(type = "done", done = true))
                    return
                }
                try {
                    val json = Json.parseToJsonElement(data).jsonObject
                    val delta = json["choices"]?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("delta")?.jsonObject
                    val content = delta?.get("content")?.jsonPrimitive?.contentOrNull
                    if (content != null) {
                        onEvent(LlmStreamEvent(type = "content", content = content))
                    }
                } catch (_: Exception) {}
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                onEvent(LlmStreamEvent(type = "error", content = t?.message ?: "Stream failed"))
            }
        })
    }

    // === GEMINI (Free) ===
    private fun callGemini(messages: List<LlmMessage>, model: String, apiKey: String): String {
        val actualModel = model.removePrefix("gemini/")
        val contents = buildJsonArray {
            messages.forEach { msg ->
                addJsonObject {
                    put("role", if (msg.role == "assistant") "model" else "user")
                    putJsonArray("parts") {
                        addJsonObject { put("text", msg.content) }
                    }
                }
            }
        }

        val body = buildJsonObject {
            put("contents", contents)
            put("generationConfig", buildJsonObject {
                put("maxOutputTokens", 4096)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/${actualModel}:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Gemini API error: ${response.code}")

        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        val json = Json.parseToJsonElement(responseBody).jsonObject
        return json["candidates"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("content")?.jsonObject
            ?.get("parts")?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("text")?.jsonPrimitive?.content
            ?: throw Exception("Invalid response format")
    }

    private fun streamGemini(messages: List<LlmMessage>, model: String, apiKey: String, onEvent: (LlmStreamEvent) -> Unit) {
        val actualModel = model.removePrefix("gemini/")
        val contents = buildJsonArray {
            messages.forEach { msg ->
                addJsonObject {
                    put("role", if (msg.role == "assistant") "model" else "user")
                    putJsonArray("parts") {
                        addJsonObject { put("text", msg.content) }
                    }
                }
            }
        }

        val body = buildJsonObject {
            put("contents", contents)
            put("generationConfig", buildJsonObject {
                put("maxOutputTokens", 4096)
                put("responseModalities", buildJsonArray { add("TEXT") })
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/${actualModel}:streamGenerateContent?alt=sse&key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val factory = EventSources.createFactory(client)
        factory.newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val json = Json.parseToJsonElement(data).jsonObject
                    val text = json["candidates"]?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("content")?.jsonObject
                        ?.get("parts")?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                    if (text != null) {
                        onEvent(LlmStreamEvent(type = "content", content = text))
                    }
                } catch (_: Exception) {}
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                onEvent(LlmStreamEvent(type = "error", content = t?.message ?: "Stream failed"))
            }

            override fun onClosed(eventSource: EventSource) {
                onEvent(LlmStreamEvent(type = "done", done = true))
            }
        })
    }

    // === OPENAI ===
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

    // === ANTHROPIC ===
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

    // === OPENCODE FREE (Big Pickle, temporales) ===
    private suspend fun callOpenCodeFree(messages: List<LlmMessage>, model: String): String {
        // Para modelos temporales de OpenCode, usamos OpenRouter con endpoints free
        val openRouterKey = getApiKey("openrouter")

        return when {
            model.contains("deepseek") -> {
                // DeepSeek R1 temporal
                val body = buildJsonObject {
                    put("model", "deepseek/deepseek-r1")
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
                callOpenRouterApi("https://openrouter.ai/api/v1/chat/completions", body, openRouterKey)
            }
            model.contains("qwen-coder") -> {
                val body = buildJsonObject {
                    put("model", "qwen/qwen-2.5-coder-32b-instruct")
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
                callOpenRouterApi("https://openrouter.ai/api/v1/chat/completions", body, openRouterKey)
            }
            model.contains("big-pickle") -> {
                // Big Pickle usa Groq con Llama 3.3 70B (mejor rendimiento gratis)
                val body = buildJsonObject {
                    put("model", "llama-3.3-70b-versatile")
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
                callOpenRouterApi("https://api.groq.com/openai/v1/chat/completions", body, getApiKey("groq"))
            }
            else -> throw Exception("Unknown OpenCode model: $model")
        }
    }

    private fun callOpenRouterApi(url: String, body: JsonObject, apiKey: String): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("API error: ${response.code}")

        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        val json = Json.parseToJsonElement(responseBody).jsonObject
        return json["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonObject
            ?.get("content")?.jsonPrimitive?.content
            ?: throw Exception("Invalid response format")
    }
}
