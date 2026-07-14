package ai.opencode.android.domain.repository

import ai.opencode.android.data.api.OpenCodeApi
import ai.opencode.android.data.model.Config
import ai.opencode.android.data.model.Session
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val api: OpenCodeApi,
) {
    suspend fun list(): List<Session> = api.listSessions()

    suspend fun create(): Session = api.createSession()

    suspend fun get(id: String): Session = api.getSession(id)

    suspend fun delete(id: String) = api.deleteSession(id)
}
