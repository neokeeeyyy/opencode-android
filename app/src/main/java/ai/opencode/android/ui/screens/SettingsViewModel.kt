package ai.opencode.android.ui.screens

import ai.opencode.android.data.api.OpenCodeClient
import ai.opencode.android.data.store.SettingsStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isConnected: Boolean = false,
    val serverUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val openaiKey: String = "",
    val anthropicKey: String = "",
    val openrouterKey: String = "",
    val groqKey: String = "",
    val geminiKey: String = "",
    val selectedModel: String = "groq/llama-3.3-70b-versatile",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val client: OpenCodeClient,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadApiKeys()
        loadSelectedModel()
        observeConnection()
    }

    private fun loadApiKeys() {
        viewModelScope.launch {
            val openai = settingsStore.getApiKey("openai")
            val anthropic = settingsStore.getApiKey("anthropic")
            val openrouter = settingsStore.getApiKey("openrouter")
            val groq = settingsStore.getApiKey("groq")
            val gemini = settingsStore.getApiKey("gemini")
            _uiState.update {
                it.copy(
                    openaiKey = openai ?: "",
                    anthropicKey = anthropic ?: "",
                    openrouterKey = openrouter ?: "",
                    groqKey = groq ?: "",
                    geminiKey = gemini ?: "",
                )
            }
        }
    }

    private fun loadSelectedModel() {
        viewModelScope.launch {
            val model = settingsStore.getCurrentModel()
            if (model != null) {
                _uiState.update { it.copy(selectedModel = model) }
            }
        }
    }

    private fun observeConnection() {
        viewModelScope.launch {
            client.connectionState.collect { state ->
                _uiState.update {
                    it.copy(
                        isConnected = state == ai.opencode.android.data.api.ConnectionState.Connected,
                        serverUrl = client.serverUrl.value,
                    )
                }
            }
        }
    }

    fun selectModel(modelId: String) {
        _uiState.update { it.copy(selectedModel = modelId) }
        viewModelScope.launch {
            settingsStore.saveCurrentModel(modelId)
        }
    }

    fun updateOpenaiKey(key: String) {
        _uiState.update { it.copy(openaiKey = key) }
        viewModelScope.launch {
            settingsStore.saveApiKey("openai", key)
        }
    }

    fun updateAnthropicKey(key: String) {
        _uiState.update { it.copy(anthropicKey = key) }
        viewModelScope.launch {
            settingsStore.saveApiKey("anthropic", key)
        }
    }

    fun updateOpenrouterKey(key: String) {
        _uiState.update { it.copy(openrouterKey = key) }
        viewModelScope.launch {
            settingsStore.saveApiKey("openrouter", key)
        }
    }

    fun updateGroqKey(key: String) {
        _uiState.update { it.copy(groqKey = key) }
        viewModelScope.launch {
            settingsStore.saveApiKey("groq", key)
        }
    }

    fun updateGeminiKey(key: String) {
        _uiState.update { it.copy(geminiKey = key) }
        viewModelScope.launch {
            settingsStore.saveApiKey("gemini", key)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
