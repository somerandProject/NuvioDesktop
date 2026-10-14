package com.nuvio.app.core.auth

/**
 * Platform storage for the serialized Supabase [UserSession].
 *
 * Supabase-kt's default session manager needs `multiplatform-settings`, which
 * on Android only persists if a default Settings instance has been provided;
 * in practice sessions were being kept in memory, so signing in was lost as
 * soon as the process died. This tiny storage keeps the session in the
 * platform's own preferences instead.
 */
internal expect object SupabaseSessionStorage {
    fun load(): String?
    fun save(value: String)
    fun clear()
}
