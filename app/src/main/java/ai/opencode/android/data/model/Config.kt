package ai.opencode.android.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Config(
    val provider: Map<String, JsonElement>? = null,
    val model: Map<String, String>? = null,
    val share: ShareConfig? = null,
    val mcp: Map<String, JsonElement>? = null,
)

@Serializable
data class ShareConfig(
    val outputFormat: String? = null,
)
