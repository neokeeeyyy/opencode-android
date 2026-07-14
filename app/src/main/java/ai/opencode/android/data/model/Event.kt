package ai.opencode.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement

@Serializable
data class SseEnvelope(
    val directory: String = "",
    val project: String? = null,
    val workspace: String? = null,
    val payload: Event,
)

@Serializable
@JsonClassDiscriminator("type")
sealed class Event {
    // Session lifecycle
    @Serializable
    @SerialName("session.created")
    data class SessionCreated(
        val id: String = "",
        val type: String = "",
        val properties: SessionEventProps = SessionEventProps(),
    ) : Event()

    @Serializable
    @SerialName("session.updated")
    data class SessionUpdated(
        val id: String = "",
        val type: String = "",
        val properties: SessionEventProps = SessionEventProps(),
    ) : Event()

    @Serializable
    @SerialName("session.deleted")
    data class SessionDeleted(
        val id: String = "",
        val type: String = "",
        val properties: SessionEventProps = SessionEventProps(),
    ) : Event()

    @Serializable
    @SerialName("session.status")
    data class SessionStatusEvent(
        val id: String = "",
        val type: String = "",
        val properties: SessionStatusProps = SessionStatusProps(),
    ) : Event()

    @Serializable
    @SerialName("session.error")
    data class SessionErrorEvent(
        val id: String = "",
        val type: String = "",
        val properties: SessionErrorProps = SessionErrorProps(),
    ) : Event()

    @Serializable
    @SerialName("session.idle")
    data class SessionIdle(
        val id: String = "",
        val type: String = "",
        val properties: SessionIdProps = SessionIdProps(),
    ) : Event()

    @Serializable
    @SerialName("session.diff")
    data class SessionDiff(
        val id: String = "",
        val type: String = "",
        val properties: SessionDiffProps = SessionDiffProps(),
    ) : Event()

    // Message lifecycle
    @Serializable
    @SerialName("message.updated")
    data class MessageUpdated(
        val id: String = "",
        val type: String = "",
        val properties: MessageEventProps = MessageEventProps(),
    ) : Event()

    @Serializable
    @SerialName("message.removed")
    data class MessageRemoved(
        val id: String = "",
        val type: String = "",
        val properties: MessageRemovedProps = MessageRemovedProps(),
    ) : Event()

    @Serializable
    @SerialName("message.part.updated")
    data class MessagePartUpdated(
        val id: String = "",
        val type: String = "",
        val properties: MessagePartProps = MessagePartProps(),
    ) : Event()

    @Serializable
    @SerialName("message.part.delta")
    data class MessagePartDelta(
        val id: String = "",
        val type: String = "",
        val properties: MessagePartDeltaProps = MessagePartDeltaProps(),
    ) : Event()

    @Serializable
    @SerialName("message.part.removed")
    data class MessagePartRemoved(
        val id: String = "",
        val type: String = "",
        val properties: MessagePartRemovedProps = MessagePartRemovedProps(),
    ) : Event()

    // Streaming events
    @Serializable
    @SerialName("session.next.text.started")
    data class TextStarted(
        val id: String = "",
        val type: String = "",
        val properties: TextStartedProps = TextStartedProps(),
    ) : Event()

    @Serializable
    @SerialName("session.next.text.delta")
    data class TextDelta(
        val id: String = "",
        val type: String = "",
        val properties: TextDeltaProps = TextDeltaProps(),
    ) : Event()

    @Serializable
    @SerialName("session.next.text.ended")
    data class TextEnded(
        val id: String = "",
        val type: String = "",
        val properties: TextEndedProps = TextEndedProps(),
    ) : Event()

    @Serializable
    @SerialName("session.next.reasoning.started")
    data class ReasoningStarted(
        val id: String = "",
        val type: String = "",
        val properties: ReasoningStartedProps = ReasoningStartedProps(),
    ) : Event()

    @Serializable
    @SerialName("session.next.reasoning.delta")
    data class ReasoningDelta(
        val id: String = "",
        val type: String = "",
        val properties: ReasoningDeltaProps = ReasoningDeltaProps(),
    ) : Event()

    @Serializable
    @SerialName("session.next.reasoning.ended")
    data class ReasoningEnded(
        val id: String = "",
        val type: String = "",
        val properties: ReasoningEndedProps = ReasoningEndedProps(),
    ) : Event()

    // Tool events
    @Serializable
    @SerialName("session.next.tool.input.started")
    data class ToolInputStarted(
        val id: String = "",
        val type: String = "",
        val properties: ToolInputStartedProps = ToolInputStartedProps(),
    ) : Event()

    @Serializable
    @SerialName("session.next.tool.input.delta")
    data class ToolInputDelta(
        val id: String = "",
        val type: String = "",
        val properties: ToolInputDeltaProps = ToolInputDeltaProps(),
    ) : Event()

    @Serializable
    @SerialName("session.next.tool.input.ended")
    data class ToolInputEnded(
        val id: String = "",
        val type: String = "",
        val properties: ToolInputEndedProps = ToolInputEndedProps(),
    ) : Event()

    @Serializable
    @SerialName("session.next.tool.called")
    data class ToolCalled(
        val id: String = "",
        val type: String = "",
        val properties: ToolCalledProps = ToolCalledProps(),
    ) : Event()

    @Serializable
    @SerialName("session.next.tool.progress")
    data class ToolProgress(
        val id: String = "",
        val type: String = "",
        val properties: ToolProgressProps = ToolProgressProps(),
    ) : Event()

    @Serializable
    @SerialName("session.next.tool.success")
    data class ToolSuccess(
        val id: String = "",
        val type: String = "",
        val properties: ToolSuccessProps = ToolSuccessProps(),
    ) : Event()

    @Serializable
    @SerialName("session.next.tool.failed")
    data class ToolFailed(
        val id: String = "",
        val type: String = "",
        val properties: ToolFailedProps = ToolFailedProps(),
    ) : Event()

    // Step events
    @Serializable
    @SerialName("session.next.step.started")
    data class StepStarted(
        val id: String = "",
        val type: String = "",
        val properties: StepStartedProps = StepStartedProps(),
    ) : Event()

    @Serializable
    @SerialName("session.next.step.ended")
    data class StepEnded(
        val id: String = "",
        val type: String = "",
        val properties: StepEndedProps = StepEndedProps(),
    ) : Event()

    @Serializable
    @SerialName("session.next.step.failed")
    data class StepFailed(
        val id: String = "",
        val type: String = "",
        val properties: StepFailedProps = StepFailedProps(),
    ) : Event()

    // Agent/model switch
    @Serializable
    @SerialName("session.next.agent.switched")
    data class AgentSwitched(
        val id: String = "",
        val type: String = "",
        val properties: AgentSwitchedProps = AgentSwitchedProps(),
    ) : Event()

    @Serializable
    @SerialName("session.next.model.switched")
    data class ModelSwitched(
        val id: String = "",
        val type: String = "",
        val properties: ModelSwitchedProps = ModelSwitchedProps(),
    ) : Event()

    // Permission events
    @Serializable
    @SerialName("permission.asked")
    data class PermissionAsked(
        val id: String = "",
        val type: String = "",
        val properties: PermissionAskedProps = PermissionAskedProps(),
    ) : Event()

    @Serializable
    @SerialName("permission.replied")
    data class PermissionReplied(
        val id: String = "",
        val type: String = "",
        val properties: PermissionRepliedProps = PermissionRepliedProps(),
    ) : Event()

    // Question events
    @Serializable
    @SerialName("question.asked")
    data class QuestionAsked(
        val id: String = "",
        val type: String = "",
        val properties: QuestionAskedProps = QuestionAskedProps(),
    ) : Event()

    @Serializable
    @SerialName("question.replied")
    data class QuestionReplied(
        val id: String = "",
        val type: String = "",
        val properties: QuestionRepliedProps = QuestionRepliedProps(),
    ) : Event()

    // Shell events
    @Serializable
    @SerialName("session.next.shell.started")
    data class ShellStarted(
        val id: String = "",
        val type: String = "",
        val properties: ShellStartedProps = ShellStartedProps(),
    ) : Event()

    @Serializable
    @SerialName("session.next.shell.ended")
    data class ShellEnded(
        val id: String = "",
        val type: String = "",
        val properties: ShellEndedProps = ShellEndedProps(),
    ) : Event()

    // Compaction events
    @Serializable
    @SerialName("session.next.compaction.started")
    data class CompactionStarted(
        val id: String = "",
        val type: String = "",
        val properties: CompactionStartedProps = CompactionStartedProps(),
    ) : Event()

    @Serializable
    @SerialName("session.next.compaction.ended")
    data class CompactionEnded(
        val id: String = "",
        val type: String = "",
        val properties: CompactionEndedProps = CompactionEndedProps(),
    ) : Event()

    // Fallback for unknown events
    @Serializable
    @SerialName("unknown")
    data class Unknown(
        val id: String = "",
        val type: String = "",
        val properties: JsonElement? = null,
    ) : Event()
}

// Property classes
@Serializable data class SessionEventProps(val sessionID: String = "", val info: Session? = null)
@Serializable data class SessionStatusProps(val sessionID: String = "", val status: SessionStatus = SessionStatus.Idle())
@Serializable data class SessionIdProps(val sessionID: String = "")
@Serializable data class SessionDiffProps(val sessionID: String = "", val diff: List<SnapshotFileDiff> = emptyList())
@Serializable data class SessionErrorProps(val sessionID: String? = null, val error: SessionError? = null)
@Serializable data class MessageEventProps(val sessionID: String = "", val info: Message? = null)
@Serializable data class MessageRemovedProps(val sessionID: String = "", val messageID: String = "")
@Serializable data class MessagePartProps(val sessionID: String = "", val part: Part? = null, val time: Long = 0)
@Serializable data class MessagePartDeltaProps(val sessionID: String = "", val messageID: String = "", val partID: String = "", val field: String = "", val delta: String = "")
@Serializable data class MessagePartRemovedProps(val sessionID: String = "", val messageID: String = "", val partID: String = "")

@Serializable data class TextStartedProps(val timestamp: Long = 0, val sessionID: String = "", val assistantMessageID: String = "", val textID: String = "")
@Serializable data class TextDeltaProps(val timestamp: Long = 0, val sessionID: String = "", val assistantMessageID: String = "", val textID: String = "", val delta: String = "")
@Serializable data class TextEndedProps(val timestamp: Long = 0, val sessionID: String = "", val assistantMessageID: String = "", val textID: String = "", val text: String = "")

@Serializable data class ReasoningStartedProps(val timestamp: Long = 0, val sessionID: String = "", val assistantMessageID: String = "", val reasoningID: String = "")
@Serializable data class ReasoningDeltaProps(val timestamp: Long = 0, val sessionID: String = "", val assistantMessageID: String = "", val reasoningID: String = "", val delta: String = "")
@Serializable data class ReasoningEndedProps(val timestamp: Long = 0, val sessionID: String = "", val assistantMessageID: String = "", val reasoningID: String = "", val text: String = "")

@Serializable data class ToolInputStartedProps(val timestamp: Long = 0, val sessionID: String = "", val assistantMessageID: String = "", val callID: String = "", val name: String = "")
@Serializable data class ToolInputDeltaProps(val timestamp: Long = 0, val sessionID: String = "", val assistantMessageID: String = "", val callID: String = "", val delta: String = "")
@Serializable data class ToolInputEndedProps(val timestamp: Long = 0, val sessionID: String = "", val assistantMessageID: String = "", val callID: String = "", val text: String = "")
@Serializable data class ToolCalledProps(val timestamp: Long = 0, val sessionID: String = "", val assistantMessageID: String = "", val callID: String = "", val tool: String = "", val input: JsonElement? = null, val provider: ToolProviderInfo? = null)
@Serializable data class ToolProgressProps(val timestamp: Long = 0, val sessionID: String = "", val assistantMessageID: String = "", val callID: String = "", val structured: JsonElement? = null, val content: List<JsonElement> = emptyList())
@Serializable data class ToolSuccessProps(val timestamp: Long = 0, val sessionID: String = "", val assistantMessageID: String = "", val callID: String = "", val structured: JsonElement? = null, val content: List<JsonElement> = emptyList(), val outputPaths: List<String>? = null, val result: JsonElement? = null, val provider: ToolProviderInfo? = null)
@Serializable data class ToolFailedProps(val timestamp: Long = 0, val sessionID: String = "", val assistantMessageID: String = "", val callID: String = "", val error: SessionError? = null, val result: JsonElement? = null, val provider: ToolProviderInfo? = null)
@Serializable data class ToolProviderInfo(val executed: Boolean = false, val metadata: JsonElement? = null)

@Serializable data class StepStartedProps(val timestamp: Long = 0, val sessionID: String = "", val assistantMessageID: String = "", val agent: String = "", val model: ModelRef = ModelRef(), val snapshot: String? = null)
@Serializable data class StepEndedProps(val timestamp: Long = 0, val sessionID: String = "", val assistantMessageID: String = "", val finish: String = "", val cost: Double = 0.0, val tokens: TokenUsage = TokenUsage(), val snapshot: String? = null, val files: List<String>? = null)
@Serializable data class StepFailedProps(val timestamp: Long = 0, val sessionID: String = "", val assistantMessageID: String = "", val error: SessionError? = null)

@Serializable data class AgentSwitchedProps(val timestamp: Long = 0, val sessionID: String = "", val messageID: String = "", val agent: String = "")
@Serializable data class ModelSwitchedProps(val timestamp: Long = 0, val sessionID: String = "", val messageID: String = "", val model: ModelRef = ModelRef())

@Serializable data class PermissionAskedProps(val id: String = "", val sessionID: String = "", val permission: String = "", val patterns: List<String> = emptyList(), val metadata: Map<String, JsonElement>? = null, val always: List<String> = emptyList(), val tool: ToolRef? = null)
@Serializable data class PermissionRepliedProps(val sessionID: String = "", val requestID: String = "", val reply: String = "")
@Serializable data class ToolRef(val messageID: String = "", val callID: String = "")

@Serializable data class QuestionAskedProps(val id: String = "", val sessionID: String = "", val questions: List<JsonElement> = emptyList(), val tool: ToolRef? = null)
@Serializable data class QuestionRepliedProps(val sessionID: String = "", val requestID: String = "", val answers: List<String> = emptyList())

@Serializable data class ShellStartedProps(val timestamp: Long = 0, val sessionID: String = "", val messageID: String = "", val callID: String = "", val command: String = "")
@Serializable data class ShellEndedProps(val timestamp: Long = 0, val sessionID: String = "", val callID: String = "", val output: String = "")

@Serializable data class CompactionStartedProps(val timestamp: Long = 0, val sessionID: String = "", val messageID: String = "", val reason: String = "")
@Serializable data class CompactionEndedProps(val timestamp: Long = 0, val sessionID: String = "", val messageID: String = "", val reason: String = "", val text: String = "", val recent: String = "")
