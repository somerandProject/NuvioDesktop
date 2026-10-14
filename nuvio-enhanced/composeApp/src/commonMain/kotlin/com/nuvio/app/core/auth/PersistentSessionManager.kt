package com.nuvio.app.core.auth

import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json

/**
 * A [SessionManager] that persists the Supabase session through
 * [SupabaseSessionStorage] so a signed-in user stays signed in across app
 * restarts.
 */
internal class PersistentSessionManager : SessionManager {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override suspend fun saveSession(session: UserSession) {
        runCatching { SupabaseSessionStorage.save(json.encodeToString(UserSession.serializer(), session)) }
    }

    override suspend fun loadSession(): UserSession? {
        val stored = SupabaseSessionStorage.load() ?: return null
        return runCatching { json.decodeFromString(UserSession.serializer(), stored) }.getOrNull()
    }

    override suspend fun deleteSession() {
        SupabaseSessionStorage.clear()
    }
}
