package ai.opencode.android.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Provider(
    val id: String = "",
    val name: String = "",
    val models: List<ProviderModel> = emptyList(),
)

@Serializable
data class ProviderModel(
    val id: String = "",
    val name: String = "",
    val attachment: Boolean = false,
    val reasoning: Boolean = false,
    val toolCall: Boolean = true,
    val context: Long = 0,
    val maxTokens: Long = 0,
)
