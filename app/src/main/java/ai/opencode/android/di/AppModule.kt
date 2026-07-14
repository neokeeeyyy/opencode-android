package ai.opencode.android.di

import ai.opencode.android.data.api.OpenCodeClient
import ai.opencode.android.data.store.SettingsStore
import ai.opencode.android.server.LocalServer
import ai.opencode.android.server.LlmClient
import ai.opencode.android.server.SessionManager
import ai.opencode.android.server.MessageStore
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsStore(
        @ApplicationContext context: Context,
    ): SettingsStore {
        return SettingsStore(context)
    }

    @Provides
    @Singleton
    fun provideOpenCodeClient(
        settingsStore: SettingsStore,
    ): OpenCodeClient {
        return OpenCodeClient(settingsStore)
    }

    @Provides
    @Singleton
    fun provideLlmClient(
        @ApplicationContext context: Context,
    ): LlmClient {
        return LlmClient(context)
    }

    @Provides
    @Singleton
    fun provideLocalServer(
        llmClient: LlmClient,
    ): LocalServer {
        SessionManager.init(llmClient.context)
        MessageStore.init(llmClient.context)
        LocalServer.setLlmClient(llmClient)
        return LocalServer
    }
}
