package ai.opencode.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("role")
sealed class Message {
    abstract val id: String
    abstract val sessionID: String
    abstract val time: MessageTime
}

@Serializable
@SerialName("user")
data class UserMessage(
    override val id: String = "",
    override val sessionID: String = "",
    override val time: MessageTime = MessageTime(),
    val agent: String = "",
    val model: ModelRef = ModelRef(),
    val format: OutputFormat? = null,
    val summary: UserMessageSummary? = null,
    val system: String? = null,
    val tools: Map<String, Boolean>? = null,
) : Message()

@Serializable
@SerialName("assistant")
data class AssistantMessage(
    override val id: String = "",
    override val sessionID: String = "",
    override val time: MessageTime = MessageTime(),
    val parentID: String = "",
    val modelID: String = "",
    val providerID: String = "",
    val mode: String = "",
    val agent: String = "",
    val path: MessagePath = MessagePath(),
    val summary: Boolean? = null,
    val cost: Double = 0.0,
    val tokens: TokenUsage = TokenUsage(),
    val structured: kotlinx.serialization.json.JsonElement? = null,
    val variant: String? = null,
    val finish: String? = null,
    val error: SessionError? = null,
) : Message()

@Serializable
data class MessageTime(
    val created: Long = 0,
    val completed: Long? = null,
)

@Serializable
data class OutputFormat(
    val type: String = "text",
    val schema: kotlinx.serialization.json.JsonElement? = null,
    val retryCount: Int? = null,
)

@Serializable
data class UserMessageSummary(
    val title: String? = null,
    val body: String? = null,
    val diffs: List<SnapshotFileDiff> = emptyList(),
)

@Serializable
data class MessagePath(
    val cwd: String = "",
    val root: String = "",
)

@Serializable
@JsonClassDiscriminator("type")
sealed class SessionError {
    @Serializable
    @SerialName("unknown")
    data class Unknown(val message: String = "") : SessionError()

    @Serializable
    @SerialName("provider_auth")
    data class ProviderAuth(val message: String = "") : SessionError()

    @Serializable
    @SerialName("message_output_length")
    data class MessageOutputLength(val message: String = "") : SessionError()

    @Serializable
    @SerialName("message_aborted")
    data class MessageAborted(val message: String = "") : SessionError()

    @Serializable
    @SerialName("structured_output")
    data class StructuredOutput(val message: String = "") : SessionError()

    @Serializable
    @SerialName("context_overflow")
    data class ContextOverflow(val message: String = "") : SessionError()

    @Serializable
    @SerialName("content_filter")
    data class ContentFilter(val message: String = "") : SessionError()

    @Serializable
    @SerialName("api")
    data class Api(val message: String = "") : SessionError()
}
