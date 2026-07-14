package ai.opencode.android.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Health(
    val status: String = "",
    val version: String = "",
    val uptime: Long = 0,
)
