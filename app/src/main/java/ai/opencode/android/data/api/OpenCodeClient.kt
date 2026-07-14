package ai.opencode.android.data.api

import ai.opencode.android.data.store.SettingsStore
import ai.opencode.android.data.model.Health
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenCodeClient @Inject constructor(
    private val settingsStore: SettingsStore,
) {
    private var _httpClient: HttpClient? = null
    private var _sseClient: HttpClient? = null

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _serverUrl = MutableStateFlow<String?>(null)
    val serverUrl: StateFlow<String?> = _serverUrl.asStateFlow()

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        classDiscriminator = "type"
        explicitNulls = false
    }

    fun getHttpClient(): HttpClient {
        return _httpClient ?: throw IllegalStateException("Client not connected. Call connect() first.")
    }

    fun getSseClient(): HttpClient {
        return _sseClient ?: throw IllegalStateException("SSE client not connected. Call connect() first.")
    }

    suspend fun connect(url: String) {
        _connectionState.value = ConnectionState.Connecting

        try {
            _httpClient = HttpClient(OkHttp) {
                install(ContentNegotiation) {
                    json(this@OpenCodeClient.json)
                }
                install(SSE)
                install(WebSockets)
                engine {
                    config {
                        connectTimeout(10_000, java.util.concurrent.TimeUnit.MILLISECONDS)
                        readTimeout(30_000, java.util.concurrent.TimeUnit.MILLISECONDS)
                        writeTimeout(10_000, java.util.concurrent.TimeUnit.MILLISECONDS)
                    }
                }
                expectSuccess = false
            }

            _sseClient = HttpClient(OkHttp) {
                install(SSE)
                install(ContentNegotiation) {
                    json(this@OpenCodeClient.json)
                }
                engine {
                    config {
                        connectTimeout(10_000, java.util.concurrent.TimeUnit.MILLISECONDS)
                        readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
                    }
                }
            }

            // Health check before marking as connected
            val health: Health = _httpClient!!.get("$url/global/health").body()
            if (health.status != "ok") {
                throw IllegalStateException("Server health check failed: ${health.status}")
            }

            _serverUrl.value = url
            settingsStore.saveServerUrl(url)
            _connectionState.value = ConnectionState.Connected
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Disconnected
            throw e
        }
    }

    fun disconnect() {
        _httpClient?.close()
        _sseClient?.close()
        _httpClient = null
        _sseClient = null
        _serverUrl.value = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun isConnected(): Boolean = _connectionState.value == ConnectionState.Connected
}

enum class ConnectionState {
    Disconnected,
    Connecting,
    Connected,
}
