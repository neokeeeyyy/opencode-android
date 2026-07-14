package ai.opencode.android.data.api

import ai.opencode.android.data.model.Event
import ai.opencode.android.data.model.SseEnvelope
import io.ktor.client.plugins.sse.ClientSSESession
import io.ktor.client.plugins.sse.sseSession
import io.ktor.client.request.url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SseClient @Inject constructor(
    private val client: OpenCodeClient,
) {
    private val _events = MutableSharedFlow<Event>(
        replay = 1,
        extraBufferCapacity = 128,
    )
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private val _rawEvents = MutableSharedFlow<SseEnvelope>(
        replay = 1,
        extraBufferCapacity = 128,
    )
    val rawEvents: SharedFlow<SseEnvelope> = _rawEvents.asSharedFlow()

    private var job: Job? = null
    private var session: ClientSSESession? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        classDiscriminator = "type"
        explicitNulls = false
    }

    fun startListening(scope: CoroutineScope) {
        stop()

        job = scope.launch {
            while (isActive) {
                try {
                    client.getSseClient().sseSession {
                        url("${client.serverUrl.value}/event")
                    }.use { sseSession ->
                        session = sseSession
                        sseSession.incoming.receiveAsFlow().collect { sseEvent ->
                            sseEvent.data?.let { data ->
                                parseAndEmit(data)
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        kotlinx.coroutines.delay(3000)
                    }
                } finally {
                    session = null
                }
            }
        }
    }

    private suspend fun parseAndEmit(data: String) {
        try {
            // Try to parse as envelope first: { directory, payload: { type, ... } }
            val element = json.parseToJsonElement(data)
            val obj = element.jsonObject

            // Check if this is an envelope (has "payload" and "directory")
            val payload = obj["payload"]
            if (payload != null) {
                val payloadObj = payload.jsonObject
                val eventType = payloadObj["type"]?.jsonPrimitive?.content

                if (eventType != null) {
                    // Parse the payload as the specific event type
                    val event = parseEventByType(eventType, payloadObj)
                    if (event != null) {
                        _events.emit(event)
                        _rawEvents.emit(SseEnvelope(
                            directory = obj["directory"]?.jsonPrimitive?.content ?: "",
                            project = obj["project"]?.jsonPrimitive?.content,
                            workspace = obj["workspace"]?.jsonPrimitive?.content,
                            payload = event,
                        ))
                    }
                }
            } else {
                // Maybe it's a direct event (no envelope)
                val eventType = obj["type"]?.jsonPrimitive?.content
                if (eventType != null) {
                    val event = parseEventByType(eventType, obj)
                    if (event != null) {
                        _events.emit(event)
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore unparseable events
        }
    }

    private fun parseEventByType(type: String, obj: JsonObject): Event? {
        return try {
            when (type) {
                // Session lifecycle
                "session.created" -> json.decodeFromToString<Event.SessionCreated>(obj.toString())
                "session.updated" -> json.decodeFromToString<Event.SessionUpdated>(obj.toString())
                "session.deleted" -> json.decodeFromToString<Event.SessionDeleted>(obj.toString())
                "session.status" -> json.decodeFromToString<Event.SessionStatusEvent>(obj.toString())
                "session.error" -> json.decodeFromToString<Event.SessionErrorEvent>(obj.toString())
                "session.idle" -> json.decodeFromToString<Event.SessionIdle>(obj.toString())
                "session.diff" -> json.decodeFromToString<Event.SessionDiff>(obj.toString())
                "session.compacted" -> null // Not critical for UI

                // Message lifecycle
                "message.updated" -> json.decodeFromToString<Event.MessageUpdated>(obj.toString())
                "message.removed" -> json.decodeFromToString<Event.MessageRemoved>(obj.toString())
                "message.part.updated" -> json.decodeFromToString<Event.MessagePartUpdated>(obj.toString())
                "message.part.delta" -> json.decodeFromToString<Event.MessagePartDelta>(obj.toString())
                "message.part.removed" -> json.decodeFromToString<Event.MessagePartRemoved>(obj.toString())

                // Text streaming
                "session.next.text.started" -> json.decodeFromToString<Event.TextStarted>(obj.toString())
                "session.next.text.delta" -> json.decodeFromToString<Event.TextDelta>(obj.toString())
                "session.next.text.ended" -> json.decodeFromToString<Event.TextEnded>(obj.toString())

                // Reasoning streaming
                "session.next.reasoning.started" -> json.decodeFromToString<Event.ReasoningStarted>(obj.toString())
                "session.next.reasoning.delta" -> json.decodeFromToString<Event.ReasoningDelta>(obj.toString())
                "session.next.reasoning.ended" -> json.decodeFromToString<Event.ReasoningEnded>(obj.toString())

                // Tool events
                "session.next.tool.input.started" -> json.decodeFromToString<Event.ToolInputStarted>(obj.toString())
                "session.next.tool.input.delta" -> json.decodeFromToString<Event.ToolInputDelta>(obj.toString())
                "session.next.tool.input.ended" -> json.decodeFromToString<Event.ToolInputEnded>(obj.toString())
                "session.next.tool.called" -> json.decodeFromToString<Event.ToolCalled>(obj.toString())
                "session.next.tool.progress" -> json.decodeFromToString<Event.ToolProgress>(obj.toString())
                "session.next.tool.success" -> json.decodeFromToString<Event.ToolSuccess>(obj.toString())
                "session.next.tool.failed" -> json.decodeFromToString<Event.ToolFailed>(obj.toString())

                // Step events
                "session.next.step.started" -> json.decodeFromToString<Event.StepStarted>(obj.toString())
                "session.next.step.ended" -> json.decodeFromToString<Event.StepEnded>(obj.toString())
                "session.next.step.failed" -> json.decodeFromToString<Event.StepFailed>(obj.toString())

                // Agent/model switch
                "session.next.agent.switched" -> json.decodeFromToString<Event.AgentSwitched>(obj.toString())
                "session.next.model.switched" -> json.decodeFromToString<Event.ModelSwitched>(obj.toString())

                // Permission
                "permission.asked" -> json.decodeFromToString<Event.PermissionAsked>(obj.toString())
                "permission.replied" -> json.decodeFromToString<Event.PermissionReplied>(obj.toString())

                // Question
                "question.asked" -> json.decodeFromToString<Event.QuestionAsked>(obj.toString())
                "question.replied" -> json.decodeFromToString<Event.QuestionReplied>(obj.toString())

                // Shell
                "session.next.shell.started" -> json.decodeFromToString<Event.ShellStarted>(obj.toString())
                "session.next.shell.ended" -> json.decodeFromToString<Event.ShellEnded>(obj.toString())

                // Compaction
                "session.next.compaction.started" -> json.decodeFromToString<Event.CompactionStarted>(obj.toString())
                "session.next.compaction.ended" -> json.decodeFromToString<Event.CompactionEnded>(obj.toString())

                // Ignore non-critical events
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        session = null
    }
}
