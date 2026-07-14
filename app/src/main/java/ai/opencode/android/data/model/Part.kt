package ai.opencode.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("type")
sealed class Part {
    abstract val id: String
    abstract val sessionID: String
    abstract val messageID: String

    @Serializable
    @SerialName("text")
    data class Text(
        override val id: String = "",
        override val sessionID: String = "",
        override val messageID: String = "",
        val text: String = "",
        val synthetic: Boolean? = null,
        val ignored: Boolean? = null,
        val time: PartTime? = null,
        val metadata: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    ) : Part()

    @Serializable
    @SerialName("tool")
    data class Tool(
        override val id: String = "",
        override val sessionID: String = "",
        override val messageID: String = "",
        val callID: String = "",
        val tool: String = "",
        val state: ToolState = ToolState.Pending(),
        val metadata: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    ) : Part()

    @Serializable
    @SerialName("reasoning")
    data class Reasoning(
        override val id: String = "",
        override val sessionID: String = "",
        override val messageID: String = "",
        val text: String = "",
        val time: PartTime = PartTime(),
        val metadata: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    ) : Part()

    @Serializable
    @SerialName("step-start")
    data class StepStart(
        override val id: String = "",
        override val sessionID: String = "",
        override val messageID: String = "",
        val snapshot: String? = null,
    ) : Part()

    @Serializable
    @SerialName("step-finish")
    data class StepFinish(
        override val id: String = "",
        override val sessionID: String = "",
        override val messageID: String = "",
        val reason: String = "",
        val snapshot: String? = null,
        val cost: Double = 0.0,
        val tokens: TokenUsage = TokenUsage(),
    ) : Part()

    @Serializable
    @SerialName("file")
    data class File(
        override val id: String = "",
        override val sessionID: String = "",
        override val messageID: String = "",
        val mime: String = "",
        val filename: String? = null,
        val url: String = "",
        val source: FilePartSource? = null,
    ) : Part()

    @Serializable
    @SerialName("subtask")
    data class Subtask(
        override val id: String = "",
        override val sessionID: String = "",
        override val messageID: String = "",
        val prompt: String = "",
        val description: String = "",
        val agent: String = "",
        val model: ModelRef? = null,
        val command: String? = null,
    ) : Part()

    @Serializable
    @SerialName("snapshot")
    data class Snapshot(
        override val id: String = "",
        override val sessionID: String = "",
        override val messageID: String = "",
        val snapshot: String = "",
    ) : Part()

    @Serializable
    @SerialName("patch")
    data class Patch(
        override val id: String = "",
        override val sessionID: String = "",
        override val messageID: String = "",
        val hash: String = "",
        val files: List<String> = emptyList(),
    ) : Part()

    @Serializable
    @SerialName("agent")
    data class Agent(
        override val id: String = "",
        override val sessionID: String = "",
        override val messageID: String = "",
        val name: String = "",
        val source: AgentSource? = null,
    ) : Part()

    @Serializable
    @SerialName("retry")
    data class Retry(
        override val id: String = "",
        override val sessionID: String = "",
        override val messageID: String = "",
        val attempt: Int = 0,
        val error: SessionError = SessionError.Unknown(),
        val time: PartTime = PartTime(),
    ) : Part()

    @Serializable
    @SerialName("compaction")
    data class Compaction(
        override val id: String = "",
        override val sessionID: String = "",
        override val messageID: String = "",
        val auto: Boolean = false,
        val overflow: Boolean? = null,
        val tailStartId: String? = null,
    ) : Part()
}

@Serializable
data class PartTime(
    val start: Long? = null,
    val end: Long? = null,
)

@Serializable
data class FilePartSource(
    val value: String? = null,
    val start: Int? = null,
    val end: Int? = null,
)

@Serializable
data class AgentSource(
    val value: String? = null,
    val start: Int? = null,
    val end: Int? = null,
)
