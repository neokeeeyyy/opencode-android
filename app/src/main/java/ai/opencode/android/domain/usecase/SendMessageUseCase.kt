package ai.opencode.android.domain.usecase

import ai.opencode.android.data.api.OpenCodeApi
import ai.opencode.android.data.model.Message
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val api: OpenCodeApi,
) {
    suspend operator fun invoke(sessionId: String, content: String): Message {
        return api.sendMessage(sessionId, content)
    }
}
