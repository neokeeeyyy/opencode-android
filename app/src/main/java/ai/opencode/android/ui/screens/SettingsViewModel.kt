package ai.opencode.android.ui.screens

import ai.opencode.android.data.api.OpenCodeClient
import ai.opencode.android.data.model.Config
import ai.opencode.android.data.model.Provider
import ai.opencode.android.data.store.SettingsStore
import ai.opencode.android.domain.repository.ConfigRepository
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
    val config: Config? = null,
    val providers: List<Provider> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val openaiKey: String = "",
    val anthropicKey: String = "",
    val openrouterKey: String = "",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val client: OpenCodeClient,
    private val configRepo: ConfigRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadApiKeys()
        observeConnection()
    }

    private fun loadApiKeys() {
        viewModelScope.launch {
            val openai = settingsStore.getApiKey("openai")
            val anthropic = settingsStore.getApiKey("anthropic")
            val openrouter = settingsStore.getApiKey("openrouter")
            _uiState.update {
                it.copy(
                    openaiKey = openai ?: "",
                    anthropicKey = anthropic ?: "",
                    openrouterKey = openrouter ?: "",
                )
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
                if (state == ai.opencode.android.data.api.ConnectionState.Connected) {
                    loadConfig()
                }
            }
        }
    }

    fun loadConfig() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val config = configRepo.getConfig()
                val providers = configRepo.listProviders()
                _uiState.update {
                    it.copy(
                        config = config,
                        providers = providers,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message,
                    )
                }
            }
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
