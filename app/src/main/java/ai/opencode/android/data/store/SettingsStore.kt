package ai.opencode.android.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val SERVER_URL = stringPreferencesKey("server_url")
        private val CURRENT_MODEL = stringPreferencesKey("current_model")
        private val CURRENT_AGENT = stringPreferencesKey("current_agent")
        private val WORKING_DIRECTORY = stringPreferencesKey("working_directory")
    }

    val serverUrl: Flow<String?> = context.dataStore.data.map { it[SERVER_URL] }
    val currentModel: Flow<String?> = context.dataStore.data.map { it[CURRENT_MODEL] }
    val currentAgent: Flow<String?> = context.dataStore.data.map { it[CURRENT_AGENT] }
    val workingDirectory: Flow<String?> = context.dataStore.data.map { it[WORKING_DIRECTORY] }

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { it[SERVER_URL] = url }
    }

    suspend fun saveCurrentModel(model: String) {
        context.dataStore.edit { it[CURRENT_MODEL] = model }
    }

    suspend fun saveCurrentAgent(agent: String) {
        context.dataStore.edit { it[CURRENT_AGENT] = agent }
    }

    suspend fun saveWorkingDirectory(dir: String) {
        context.dataStore.edit { it[WORKING_DIRECTORY] = dir }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
