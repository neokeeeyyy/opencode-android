package ai.opencode.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("status")
sealed class ToolState {
    @Serializable
    @SerialName("pending")
    data class Pending(
        val input: kotlinx.serialization.json.JsonElement? = null,
        val raw: String = "",
    ) : ToolState()

    @Serializable
    @SerialName("running")
    data class Running(
        val input: kotlinx.serialization.json.JsonElement? = null,
        val title: String? = null,
        val metadata: Map<String, kotlinx.serialization.json.JsonElement>? = null,
        val time: ToolTime = ToolTime(),
    ) : ToolState()

    @Serializable
    @SerialName("completed")
    data class Completed(
        val input: kotlinx.serialization.json.JsonElement? = null,
        val output: String = "",
        val title: String = "",
        val metadata: Map<String, kotlinx.serialization.json.JsonElement>? = null,
        val time: ToolTime = ToolTime(),
        val attachments: List<Part.File> = emptyList(),
    ) : ToolState()

    @Serializable
    @SerialName("error")
    data class Error(
        val input: kotlinx.serialization.json.JsonElement? = null,
        val error: String = "",
        val metadata: Map<String, kotlinx.serialization.json.JsonElement>? = null,
        val time: ToolTime = ToolTime(),
    ) : ToolState()
}

@Serializable
data class ToolTime(
    val start: Long? = null,
    val end: Long? = null,
    val compacted: Long? = null,
)
