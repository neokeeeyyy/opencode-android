package ai.opencode.android.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val id: String,
    val slug: String = "",
    val projectID: String = "",
    val workspaceID: String? = null,
    val directory: String = "",
    val path: String? = null,
    val parentID: String? = null,
    val title: String = "",
    val agent: String? = null,
    val model: ModelRef? = null,
    val version: String = "",
    val summary: SessionSummary? = null,
    val cost: Double? = null,
    val tokens: TokenUsage? = null,
    val share: ShareInfo? = null,
    val metadata: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    val time: SessionTime = SessionTime(),
    val permission: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    val revert: RevertState? = null,
)

@Serializable
data class ModelRef(
    val id: String = "",
    val providerID: String = "",
    val variant: String? = null,
)

@Serializable
data class SessionTime(
    val created: Long = 0,
    val updated: Long = 0,
    val compacting: Long? = null,
    val archived: Long? = null,
)

@Serializable
data class SessionSummary(
    val additions: Int = 0,
    val deletions: Int = 0,
    val files: Int = 0,
    val diffs: List<SnapshotFileDiff>? = null,
)

@Serializable
data class TokenUsage(
    val total: Long? = null,
    val input: Long = 0,
    val output: Long = 0,
    val reasoning: Long = 0,
    val cache: CacheUsage = CacheUsage(),
)

@Serializable
data class CacheUsage(
    val read: Long = 0,
    val write: Long = 0,
)

@Serializable
data class ShareInfo(
    val url: String = "",
)

@Serializable
data class SnapshotFileDiff(
    val file: String? = null,
    val patch: String? = null,
    val additions: Int = 0,
    val deletions: Int = 0,
    val status: String? = null, // "added", "deleted", "modified"
)

@Serializable
data class RevertState(
    val messageID: String = "",
    val partID: String? = null,
    val snapshot: String? = null,
    val diff: String? = null,
    val files: List<SnapshotFileDiff>? = null,
)
