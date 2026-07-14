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

data class HomeUiState(
    val hasApiKey: Boolean = false,
    val hasServerUrl: Boolean = false,
    val isConnected: Boolean = false,
    val isChecking: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val client: OpenCodeClient,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        checkState()
        connectToEmbeddedServer()
    }

    private fun checkState() {
        viewModelScope.launch {
            val serverUrl = settingsStore.serverUrl.first()
            _uiState.update {
                it.copy(
                    hasServerUrl = !serverUrl.isNullOrBlank(),
                    isChecking = false,
                )
            }
        }
    }

    private fun connectToEmbeddedServer() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isChecking = true) }
                client.connect("http://127.0.0.1:4096")
                _uiState.update {
                    it.copy(
                        isConnected = true,
                        hasServerUrl = true,
                        isChecking = false,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isConnected = false,
                        isChecking = false,
                        error = "Failed to connect: ${e.message}",
                    )
                }
            }
        }
    }
}
