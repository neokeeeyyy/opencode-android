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
    val isChecking: Boolean = true,
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
}
