package ai.opencode.android.domain.repository

import ai.opencode.android.data.api.OpenCodeApi
import ai.opencode.android.data.model.Config
import ai.opencode.android.data.model.Provider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepository @Inject constructor(
    private val api: OpenCodeApi,
) {
    suspend fun getConfig(): Config = api.getConfig()

    suspend fun updateConfig(config: Config) = api.updateConfig(config)

    suspend fun listProviders(): List<Provider> = api.listProviders()
}
