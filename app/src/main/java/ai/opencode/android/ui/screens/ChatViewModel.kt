package ai.opencode.android.ui.screens

import ai.opencode.android.data.api.ConnectionState
import ai.opencode.android.data.api.OpenCodeClient
import ai.opencode.android.data.api.SseClient
import ai.opencode.android.data.model.AssistantMessage
import ai.opencode.android.data.model.Event
import ai.opencode.android.data.model.Message
import ai.opencode.android.data.model.ModelRef
import ai.opencode.android.data.model.Part
import ai.opencode.android.data.model.Session
import ai.opencode.android.data.model.SessionStatus

import ai.opencode.android.data.model.UserMessage
import ai.opencode.android.data.store.SettingsStore
import ai.opencode.android.domain.repository.SessionRepository
import ai.opencode.android.domain.usecase.SendMessageUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val sessions: List<Session> = emptyList(),
    val currentSessionId: String? = null,
    val messages: List<Message> = emptyList(),
    val currentModel: String? = null,
    val currentAgent: String? = null,
    val isSending: Boolean = false,
    val sessionStatus: SessionStatus = SessionStatus.Idle(),
    val streamingText: String? = null,
    val pendingPermissionId: String? = null,
    val pendingPermissionTool: String? = null,
    val pendingPermissionDesc: String? = null,
    val error: String? = null,
    val isInitializing: Boolean = false,
) {
    val currentSession: Session?
        get() = sessions.find { it.id == currentSessionId }

    val canSend: Boolean
        get() = connectionState == ConnectionState.Connected &&
                currentSessionId != null &&
                !isSending

    val isBusy: Boolean
        get() = sessionStatus is SessionStatus.Busy

    val isRetry: Boolean
        get() = sessionStatus is SessionStatus.Retry
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val client: OpenCodeClient,
    private val api: ai.opencode.android.data.api.OpenCodeApi,
    private val sseClient: SseClient,
    private val sessionRepo: SessionRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Accumulator for streaming text deltas
    private val streamingAccumulator = StringBuilder()

    init {
        observeConnectionState()
        observeSseEvents()
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            client.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
                if (state == ConnectionState.Connected) {
                    onConnected()
                }
            }
        }
    }

    private fun observeSseEvents() {
        viewModelScope.launch {
            sseClient.events.collect { event ->
                handleEvent(event)
            }
        }
    }

    private fun handleEvent(event: Event) {
        when (event) {
            // Session lifecycle
            is Event.SessionCreated -> {
                event.properties.info?.let { session ->
                    _uiState.update { state ->
                        state.copy(sessions = state.sessions + session)
                    }
                }
            }
            is Event.SessionUpdated -> {
                event.properties.info?.let { session ->
                    _uiState.update { state ->
                        state.copy(
                            sessions = state.sessions.map {
                                if (it.id == session.id) session else it
                            }
                        )
                    }
                }
            }
            is Event.SessionDeleted -> {
                val sessionId = event.properties.sessionID
                _uiState.update { state ->
                    state.copy(
                        sessions = state.sessions.filter { it.id != sessionId },
                        currentSessionId = if (state.currentSessionId == sessionId) null else state.currentSessionId,
                        messages = if (state.currentSessionId == sessionId) emptyList() else state.messages,
                    )
                }
            }
            is Event.SessionStatusEvent -> {
                _uiState.update { state ->
                    state.copy(sessionStatus = event.properties.status)
                }
            }
            is Event.SessionIdle -> {
                // Session became idle - finish streaming
                if (streamingAccumulator.isNotEmpty()) {
                    finalizeStreamingText()
                }
                _uiState.update { state ->
                    state.copy(
                        sessionStatus = SessionStatus.Idle(event.properties.sessionID),
                        isSending = false,
                    )
                }
            }
            is Event.SessionErrorEvent -> {
                val errorMsg = event.properties.error?.let {
                    when (it) {
                        is ai.opencode.android.data.model.SessionError.ProviderAuth -> it.message
                        is ai.opencode.android.data.model.SessionError.Unknown -> it.message
                        is ai.opencode.android.data.model.SessionError.MessageOutputLength -> "Output length exceeded"
                        is ai.opencode.android.data.model.SessionError.MessageAborted -> "Message aborted"
                        is ai.opencode.android.data.model.SessionError.ContextOverflow -> "Context overflow"
                        is ai.opencode.android.data.model.SessionError.ContentFilter -> "Content filtered"
                        is ai.opencode.android.data.model.SessionError.Api -> it.message
                        else -> "Unknown error"
                    }
                } ?: "Session error"
                _uiState.update { state ->
                    state.copy(
                        error = errorMsg,
                        isSending = false,
                        sessionStatus = SessionStatus.Idle(event.properties.sessionID ?: ""),
                    )
                }
            }

            // Message lifecycle
            is Event.MessageUpdated -> {
                event.properties.info?.let { message ->
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map {
                                if (it.id == message.id) message else it
                            }
                        )
                    }
                }
            }
            is Event.MessageRemoved -> {
                val messageId = event.properties.messageID
                _uiState.update { state ->
                    state.copy(messages = state.messages.filter { it.id != messageId })
                }
            }
            is Event.MessagePartUpdated -> {
                // Part was fully updated (e.g., tool completed)
                // The message.updated event will handle the full message update
            }
            is Event.MessagePartDelta -> {
                // Delta on a specific part - update text in streaming
                val delta = event.properties.delta
                val field = event.properties.field
                if (field == "text" && delta.isNotEmpty()) {
                    streamingAccumulator.append(delta)
                    _uiState.update { state ->
                        state.copy(streamingText = streamingAccumulator.toString())
                    }
                }
            }

            // Text streaming (session.next.text.*)
            is Event.TextStarted -> {
                streamingAccumulator.clear()
                _uiState.update { state ->
                    state.copy(streamingText = "")
                }
            }
            is Event.TextDelta -> {
                streamingAccumulator.append(event.properties.delta)
                _uiState.update { state ->
                    state.copy(streamingText = streamingAccumulator.toString())
                }
            }
            is Event.TextEnded -> {
                // Text generation completed
                finalizeStreamingText()
            }

            // Reasoning streaming
            is Event.ReasoningStarted -> {
                // Could show reasoning in UI
            }
            is Event.ReasoningDelta -> {
                // Could accumulate reasoning text
            }
            is Event.ReasoningEnded -> {
                // Reasoning completed
            }

            // Tool events
            is Event.ToolCalled -> {
                // Tool was invoked
            }
            is Event.ToolSuccess -> {
                // Tool completed successfully
            }
            is Event.ToolFailed -> {
                // Tool failed
            }

            // Permission
            is Event.PermissionAsked -> {
                _uiState.update { state ->
                    state.copy(
                        pendingPermissionId = event.properties.id,
                        pendingPermissionTool = event.properties.permission,
                        pendingPermissionDesc = event.properties.patterns.joinToString(", "),
                    )
                }
            }
            is Event.PermissionReplied -> {
                _uiState.update { state ->
                    state.copy(
                        pendingPermissionId = null,
                        pendingPermissionTool = null,
                        pendingPermissionDesc = null,
                    )
                }
            }

            // Agent/model switch
            is Event.AgentSwitched -> {
                _uiState.update { it.copy(currentAgent = event.properties.agent) }
            }
            is Event.ModelSwitched -> {
                val model = event.properties.model
                _uiState.update { it.copy(currentModel = model.id) }
            }

            // Step events
            is Event.StepStarted -> {
                _uiState.update { state ->
                    state.copy(
                        currentAgent = event.properties.agent,
                        currentModel = event.properties.model.id,
                    )
                }
            }
            is Event.StepEnded -> {
                // Step completed
            }

            // Compaction
            is Event.CompactionStarted -> {
                // Could show compaction indicator
            }
            is Event.CompactionEnded -> {
                // Compaction completed
            }

            else -> { /* Ignore other events */ }
        }
    }

    private fun finalizeStreamingText() {
        val text = streamingAccumulator.toString()
        if (text.isNotEmpty()) {
            // The message.updated event will handle the final message
            // Just clear the streaming state
        }
        streamingAccumulator.clear()
        _uiState.update { it.copy(streamingText = null) }
    }

    private fun onConnected() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isInitializing = true) }
                val sessions = sessionRepo.list()
                _uiState.update { it.copy(sessions = sessions, isInitializing = false) }

                sseClient.startListening(viewModelScope)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message,
                        isInitializing = false,
                    )
                }
            }
        }
    }

    fun connect(serverUrl: String) {
        viewModelScope.launch {
            try {
                client.connect(serverUrl)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun disconnect() {
        sseClient.stop()
        client.disconnect()
        streamingAccumulator.clear()
        _uiState.update {
            ChatUiState(connectionState = ConnectionState.Disconnected)
        }
    }

    fun createSession() {
        viewModelScope.launch {
            try {
                val session = sessionRepo.create()
                _uiState.update { state ->
                    state.copy(
                        sessions = state.sessions + session,
                        currentSessionId = session.id,
                        messages = emptyList(),
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun selectSession(sessionId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(currentSessionId = sessionId, messages = emptyList(), streamingText = null) }
                streamingAccumulator.clear()
                val messages = api.listMessages(sessionId)
                _uiState.update { it.copy(messages = messages) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            try {
                sessionRepo.delete(sessionId)
                _uiState.update { state ->
                    state.copy(
                        sessions = state.sessions.filter { it.id != sessionId },
                        currentSessionId = if (state.currentSessionId == sessionId) null else state.currentSessionId,
                        messages = if (state.currentSessionId == sessionId) emptyList() else state.messages,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun sendMessage(content: String) {
        val sessionId = _uiState.value.currentSessionId ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSending = true, streamingText = null) }
                streamingAccumulator.clear()
                sendMessageUseCase(sessionId, content)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        error = e.message,
                    )
                }
            }
        }
    }

    fun abortSession() {
        val sessionId = _uiState.value.currentSessionId ?: return
        viewModelScope.launch {
            try {
                api.abortSession(sessionId)
                _uiState.update { it.copy(isSending = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun approvePermission() {
        val permissionId = _uiState.value.pendingPermissionId ?: return
        viewModelScope.launch {
            try {
                api.replyPermission(permissionId, "once")
                _uiState.update { state ->
                    state.copy(
                        pendingPermissionId = null,
                        pendingPermissionTool = null,
                        pendingPermissionDesc = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun denyPermission() {
        val permissionId = _uiState.value.pendingPermissionId ?: return
        viewModelScope.launch {
            try {
                api.replyPermission(permissionId, "reject")
                _uiState.update { state ->
                    state.copy(
                        pendingPermissionId = null,
                        pendingPermissionTool = null,
                        pendingPermissionDesc = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
