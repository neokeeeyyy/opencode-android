package ai.opencode.android.data.api

import ai.opencode.android.data.model.Event
import ai.opencode.android.data.model.SseEnvelope
import io.ktor.client.plugins.sse.sseSession
import io.ktor.client.request.url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
                    client.getSseClient().sseSession(
                        urlString = "${client.serverUrl.value}/event"
                    ) {
                        incoming.collect { sseEvent ->
                            sseEvent.data?.let { data ->
                                parseAndEmit(data)
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        kotlinx.coroutines.delay(3000)
                    }
                }
            }
        }
    }

    private suspend fun parseAndEmit(data: String) {
        try {
            val element = json.parseToJsonElement(data)
            val obj = element.jsonObject

            val payload = obj["payload"]
            if (payload != null) {
                val payloadObj = payload.jsonObject
                val eventType = payloadObj["type"]?.jsonPrimitive?.content

                if (eventType != null) {
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
                val eventType = obj["type"]?.jsonPrimitive?.content
                if (eventType != null) {
                    val event = parseEventByType(eventType, obj)
                    if (event != null) {
                        _events.emit(event)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun parseEventByType(type: String, obj: JsonObject): Event? {
        return try {
            when (type) {
                "session.created" -> json.decodeFromString<Event.SessionCreated>(obj.toString())
                "session.updated" -> json.decodeFromString<Event.SessionUpdated>(obj.toString())
                "session.deleted" -> json.decodeFromString<Event.SessionDeleted>(obj.toString())
                "session.status" -> json.decodeFromString<Event.SessionStatusEvent>(obj.toString())
                "session.error" -> json.decodeFromString<Event.SessionErrorEvent>(obj.toString())
                "session.idle" -> json.decodeFromString<Event.SessionIdle>(obj.toString())
                "session.diff" -> json.decodeFromString<Event.SessionDiff>(obj.toString())
                "session.compacted" -> null

                "message.updated" -> json.decodeFromString<Event.MessageUpdated>(obj.toString())
                "message.removed" -> json.decodeFromString<Event.MessageRemoved>(obj.toString())
                "message.part.updated" -> json.decodeFromString<Event.MessagePartUpdated>(obj.toString())
                "message.part.delta" -> json.decodeFromString<Event.MessagePartDelta>(obj.toString())
                "message.part.removed" -> json.decodeFromString<Event.MessagePartRemoved>(obj.toString())

                "session.next.text.started" -> json.decodeFromString<Event.TextStarted>(obj.toString())
                "session.next.text.delta" -> json.decodeFromString<Event.TextDelta>(obj.toString())
                "session.next.text.ended" -> json.decodeFromString<Event.TextEnded>(obj.toString())

                "session.next.reasoning.started" -> json.decodeFromString<Event.ReasoningStarted>(obj.toString())
                "session.next.reasoning.delta" -> json.decodeFromString<Event.ReasoningDelta>(obj.toString())
                "session.next.reasoning.ended" -> json.decodeFromString<Event.ReasoningEnded>(obj.toString())

                "session.next.tool.input.started" -> json.decodeFromString<Event.ToolInputStarted>(obj.toString())
                "session.next.tool.input.delta" -> json.decodeFromString<Event.ToolInputDelta>(obj.toString())
                "session.next.tool.input.ended" -> json.decodeFromString<Event.ToolInputEnded>(obj.toString())
                "session.next.tool.called" -> json.decodeFromString<Event.ToolCalled>(obj.toString())
                "session.next.tool.progress" -> json.decodeFromString<Event.ToolProgress>(obj.toString())
                "session.next.tool.success" -> json.decodeFromString<Event.ToolSuccess>(obj.toString())
                "session.next.tool.failed" -> json.decodeFromString<Event.ToolFailed>(obj.toString())

                "session.next.step.started" -> json.decodeFromString<Event.StepStarted>(obj.toString())
                "session.next.step.ended" -> json.decodeFromString<Event.StepEnded>(obj.toString())
                "session.next.step.failed" -> json.decodeFromString<Event.StepFailed>(obj.toString())

                "session.next.agent.switched" -> json.decodeFromString<Event.AgentSwitched>(obj.toString())
                "session.next.model.switched" -> json.decodeFromString<Event.ModelSwitched>(obj.toString())

                "permission.asked" -> json.decodeFromString<Event.PermissionAsked>(obj.toString())
                "permission.replied" -> json.decodeFromString<Event.PermissionReplied>(obj.toString())

                "question.asked" -> json.decodeFromString<Event.QuestionAsked>(obj.toString())
                "question.replied" -> json.decodeFromString<Event.QuestionReplied>(obj.toString())

                "session.next.shell.started" -> json.decodeFromString<Event.ShellStarted>(obj.toString())
                "session.next.shell.ended" -> json.decodeFromString<Event.ShellEnded>(obj.toString())

                "session.next.compaction.started" -> json.decodeFromString<Event.CompactionStarted>(obj.toString())
                "session.next.compaction.ended" -> json.decodeFromString<Event.CompactionEnded>(obj.toString())

                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
