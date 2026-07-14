package ai.opencode.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("type")
sealed class SessionStatus {
    @Serializable
    @SerialName("idle")
    data class Idle(val sessionID: String = "") : SessionStatus()

    @Serializable
    @SerialName("busy")
    data class Busy(val sessionID: String = "") : SessionStatus()

    @Serializable
    @SerialName("retry")
    data class Retry(
        val sessionID: String = "",
        val attempt: Int = 0,
        val message: String = "",
        val action: kotlinx.serialization.json.JsonElement? = null,
        val next: Long = 0,
    ) : SessionStatus()
}
