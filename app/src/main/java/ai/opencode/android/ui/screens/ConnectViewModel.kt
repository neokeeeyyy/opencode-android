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

data class ConnectUiState(
    val serverUrl: String = "",
    val isConnecting: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = false,
)

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val client: OpenCodeClient,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    init {
        loadSavedUrl()
    }

    private fun loadSavedUrl() {
        viewModelScope.launch {
            val savedUrl = settingsStore.serverUrl.first()
            if (savedUrl != null) {
                _uiState.update { it.copy(serverUrl = savedUrl) }
            }
        }
    }

    fun updateUrl(url: String) {
        _uiState.update { it.copy(serverUrl = url, error = null) }
    }

    fun connect() {
        val url = _uiState.value.serverUrl.trim()
        if (url.isBlank()) {
            _uiState.update { it.copy(error = "Enter a server URL") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, error = null) }
            try {
                client.connect(url)
                _uiState.update { it.copy(isConnecting = false, isConnected = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        error = e.message ?: "Connection failed",
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
