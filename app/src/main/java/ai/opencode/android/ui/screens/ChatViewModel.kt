package ai.opencode.android.ui.screens

import ai.opencode.android.data.api.ConnectionState
import ai.opencode.android.data.api.OpenCodeClient
import ai.opencode.android.data.store.SettingsStore
import ai.opencode.android.server.LocalServer
import ai.opencode.android.server.LlmClient
import ai.opencode.android.server.Message
import ai.opencode.android.server.SessionManager
import ai.opencode.android.server.MessageStore
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
    val sessions: List<ai.opencode.android.server.Session> = emptyList(),
    val currentSessionId: String? = null,
    val messages: List<Message> = emptyList(),
    val currentModel: String = "groq/llama-3.3-70b-versatile",
    val isSending: Boolean = false,
    val streamingText: String? = null,
    val error: String? = null,
    val isInitializing: Boolean = false,
) {
    val canSend: Boolean
        get() = currentSessionId != null && !isSending

    val isBusy: Boolean
        get() = isSending
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val client: OpenCodeClient,
    private val llmClient: LlmClient,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val streamingAccumulator = StringBuilder()

    init {
        loadModel()
        observeLocalEvents()
    }

    private fun loadModel() {
        viewModelScope.launch {
            val model = settingsStore.getCurrentModel()
            if (model != null) {
                _uiState.update { it.copy(currentModel = model) }
            }
        }
    }

    private fun observeLocalEvents() {
        viewModelScope.launch {
            LocalServer.events.collect { eventJson ->
                try {
                    val json = kotlinx.serialization.json.Json.parseToJsonElement(eventJson)
                    val jsonObj = json as? kotlinx.serialization.json.JsonObject

                    if (jsonObj?.containsKey("type") == true) {
                        when (jsonObj["type"]?.toString()?.removeSurrounding("\"")) {
                            "error" -> {
                                _uiState.update {
                                    it.copy(
                                        error = jsonObj["error"]?.toString()?.removeSurrounding("\""),
                                        isSending = false,
                                    )
                                }
                            }
                        }
                    } else {
                        // It's a message
                        val message = kotlinx.serialization.json.Json.decodeFromString<Message>(eventJson)
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages + message,
                                isSending = false,
                                streamingText = null,
                            )
                        }
                        streamingAccumulator.clear()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun connect(serverUrl: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isInitializing = true) }
                client.connect(serverUrl)
                val sessions = SessionManager.getAllSessions()
                _uiState.update {
                    it.copy(
                        connectionState = ConnectionState.Connected,
                        sessions = sessions,
                        isInitializing = false,
                    )
                }
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

    fun disconnect() {
        client.disconnect()
        streamingAccumulator.clear()
        _uiState.update {
            ChatUiState(connectionState = ConnectionState.Disconnected)
        }
    }

    fun createSession() {
        viewModelScope.launch {
            try {
                val session = SessionManager.createSession()
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
                val messages = MessageStore.getMessages(sessionId)
                _uiState.update { it.copy(messages = messages) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            try {
                SessionManager.deleteSession(sessionId)
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

                // Add user message
                val userMessage = Message(
                    id = java.util.UUID.randomUUID().toString().replace("-", "").take(24),
                    sessionId = sessionId,
                    role = "user",
                    content = content,
                    timestamp = System.currentTimeMillis()
                )
                MessageStore.addMessage(userMessage)
                _uiState.update { state ->
                    state.copy(messages = state.messages + userMessage)
                }

                // Stream response
                val messages = MessageStore.getMessages(sessionId).map {
                    ai.opencode.android.server.LlmMessage(role = it.role, content = it.content)
                }

                llmClient.chatStream(messages, _uiState.value.currentModel) { event ->
                    viewModelScope.launch {
                        when (event.type) {
                            "content" -> {
                                streamingAccumulator.append(event.content)
                                _uiState.update { state ->
                                    state.copy(streamingText = streamingAccumulator.toString())
                                }
                            }
                            "done" -> {
                                if (streamingAccumulator.isNotEmpty()) {
                                    val assistantMessage = Message(
                                        id = java.util.UUID.randomUUID().toString().replace("-", "").take(24),
                                        sessionId = sessionId,
                                        role = "assistant",
                                        content = streamingAccumulator.toString(),
                                        timestamp = System.currentTimeMillis()
                                    )
                                    MessageStore.addMessage(assistantMessage)
                                    _uiState.update { state ->
                                        state.copy(
                                            messages = state.messages + assistantMessage,
                                            streamingText = null,
                                            isSending = false,
                                        )
                                    }
                                    streamingAccumulator.clear()
                                }
                            }
                            "error" -> {
                                _uiState.update { state ->
                                    state.copy(
                                        error = event.content,
                                        isSending = false,
                                        streamingText = null,
                                    )
                                }
                                streamingAccumulator.clear()
                            }
                        }
                    }
                }
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

    fun selectModel(modelId: String) {
        _uiState.update { it.copy(currentModel = modelId) }
        viewModelScope.launch {
            settingsStore.saveCurrentModel(modelId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
