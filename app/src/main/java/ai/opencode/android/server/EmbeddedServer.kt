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
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EmbeddedServer {
    private var server: ApplicationEngine? = null
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()
    private const val PORT = 4096
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var llmClient: LlmClient

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
                get("/global/health") {
                    call.respond(mapOf("status" to "ok"))
                }

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
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing session ID")
                    )
                    val session = SessionManager.getSession(id)
                    if (session != null) {
                        call.respond(session)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Session not found"))
                    }
                }

                delete("/session/{id}") {
                    val id = call.parameters["id"] ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing session ID")
                    )
                    SessionManager.deleteSession(id)
                    call.respond(mapOf("status" to "deleted"))
                }

                get("/session/{id}/message") {
                    val sessionId = call.parameters["id"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing session ID")
                    )
                    val messages = MessageStore.getMessages(sessionId)
                    call.respond(messages)
                }

                post("/session/{id}/message") {
                    val sessionId = call.parameters["id"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing session ID")
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
                            val messages = MessageStore.getMessages(sessionId).map {
                                LlmMessage(role = it.role, content = it.content)
                            }
                            val response = llmClient.chat(messages, request.model ?: "claude-sonnet-4-20250514")

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
                            _events.emit("""{"error":"${e.message?.replace("\"", "\\\"")}"}""")
                        }
                    }

                    call.respond(userMessage)
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
