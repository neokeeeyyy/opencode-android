package ai.opencode.android.server

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.http.*
import io.ktor.server.sse.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object LocalServer {
    private var server: io.ktor.server.engine.EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()
    private const val PORT = 4096
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var llmClient: LlmClient
    private val toolExecutor = ToolExecutor()

    fun setLlmClient(client: LlmClient) {
        llmClient = client
    }

    fun start() {
        if (::llmClient.isInitialized.not()) {
            throw IllegalStateException("LlmClient not initialized")
        }

        server = embeddedServer(Netty, port = PORT) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }

            install(CORS) {
                anyHost()
                allowHeader(HttpHeaders.ContentType)
            }

            routing {
                // Health
                get("/global/health") {
                    call.respond(mapOf("status" to "ok", "version" to "1.0.0-embedded"))
                }

                // Models
                get("/models") {
                    val models = llmClient.getAllModels().map { (id, info) ->
                        mapOf(
                            "id" to id,
                            "name" to info.displayName,
                            "provider" to info.provider,
                            "free" to info.free.toString(),
                            "temporary" to info.temporary.toString()
                        )
                    }
                    call.respond(models)
                }

                get("/models/free") {
                    val models = llmClient.getFreeModels().map { (id, info) ->
                        mapOf("id" to id, "name" to info.displayName, "provider" to info.provider)
                    }
                    call.respond(models)
                }

                // Sessions
                get("/session") {
                    val sessions = SessionManager.getAllSessions()
                    call.respond(sessions)
                }

                post("/session") {
                    val session = SessionManager.createSession()
                    call.respond(session)
                }

                get("/session/{id}") {
                    val id = call.parameters["id"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest, mapOf("error" to "Missing session ID")
                    )
                    val session = SessionManager.getSession(id)
                    if (session != null) call.respond(session)
                    else call.respond(HttpStatusCode.NotFound, mapOf("error" to "Session not found"))
                }

                delete("/session/{id}") {
                    val id = call.parameters["id"] ?: return@delete call.respond(
                        HttpStatusCode.BadRequest, mapOf("error" to "Missing session ID")
                    )
                    SessionManager.deleteSession(id)
                    call.respond(mapOf("status" to "deleted"))
                }

                // Messages
                get("/session/{id}/message") {
                    val sessionId = call.parameters["id"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest, mapOf("error" to "Missing session ID")
                    )
                    call.respond(MessageStore.getMessages(sessionId))
                }

                post("/session/{id}/message") {
                    val sessionId = call.parameters["id"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest, mapOf("error" to "Missing session ID")
                    )
                    val request = call.receive<MessageRequest>()

                    val userMessage = Message(
                        id = generateId(),
                        sessionId = sessionId,
                        role = "user",
                        content = request.content,
                        timestamp = System.currentTimeMillis()
                    )
                    MessageStore.addMessage(userMessage)

                    serverScope.launch {
                        try {
                            val response = llmClient.chat(
                                messages = MessageStore.getMessages(sessionId).map {
                                    LlmMessage(role = it.role, content = it.content)
                                },
                                model = request.model ?: "groq/llama-3.3-70b-versatile"
                            )

                            val assistantMessage = Message(
                                id = generateId(),
                                sessionId = sessionId,
                                role = "assistant",
                                content = response,
                                timestamp = System.currentTimeMillis()
                            )
                            MessageStore.addMessage(assistantMessage)
                            _events.emit(Json.encodeToString(Message.serializer(), assistantMessage))
                        } catch (e: Exception) {
                            _events.emit("""{"type":"error","error":"${e.message?.replace("\"", "\\\"")}"}""")
                        }
                    }

                    call.respond(userMessage)
                }

                // Streaming chat (SSE) - simplified without Ktor SSE plugin
                get("/session/{id}/stream") {
                    val sessionId = call.parameters["id"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest, mapOf("error" to "Missing session ID")
                    )
                    val model = call.request.queryParameters["model"] ?: "groq/llama-3.3-70b-versatile"

                    val messages = MessageStore.getMessages(sessionId).map {
                        LlmMessage(role = it.role, content = it.content)
                    }

                    call.respond(mapOf(
                        "status" to "connected",
                        "session_id" to sessionId,
                        "model" to model
                    ))
                }

                // Tool execution
                post("/tool/execute") {
                    val request = call.receive<ToolExecutionRequest>()
                    val result = toolExecutor.execute(request.command, request.args)
                    call.respond(result)
                }

                post("/tool/read") {
                    val request = call.receive<ToolReadRequest>()
                    val result = toolExecutor.readFile(request.path)
                    call.respond(result)
                }

                post("/tool/write") {
                    val request = call.receive<ToolWriteRequest>()
                    val result = toolExecutor.writeFile(request.path, request.content)
                    call.respond(result)
                }

                post("/tool/list") {
                    val request = call.receive<ToolListRequest>()
                    val result = toolExecutor.listFiles(request.path)
                    call.respond(result)
                }
            }
        }

        server?.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 5000)
        server = null
        serverScope.cancel()
    }

    private fun generateId(): String {
        return java.util.UUID.randomUUID().toString().replace("-", "").take(24)
    }
}

@kotlinx.serialization.Serializable
data class MessageRequest(
    val content: String,
    val model: String? = null
)

@kotlinx.serialization.Serializable
data class ToolExecutionRequest(
    val command: String,
    val args: List<String> = emptyList()
)

@kotlinx.serialization.Serializable
data class ToolReadRequest(
    val path: String
)

@kotlinx.serialization.Serializable
data class ToolWriteRequest(
    val path: String,
    val content: String
)

@kotlinx.serialization.Serializable
data class ToolListRequest(
    val path: String
)
