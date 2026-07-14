package ai.opencode.android.di

import ai.opencode.android.data.api.OpenCodeApi
import ai.opencode.android.data.api.OpenCodeClient
import ai.opencode.android.data.api.SseClient
import ai.opencode.android.data.store.SettingsStore
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
    fun provideOpenCodeApi(
        client: OpenCodeClient,
    ): OpenCodeApi {
        return OpenCodeApi(client)
    }

    @Provides
    @Singleton
    fun provideSseClient(
        client: OpenCodeClient,
    ): SseClient {
        return SseClient(client)
    }
}
