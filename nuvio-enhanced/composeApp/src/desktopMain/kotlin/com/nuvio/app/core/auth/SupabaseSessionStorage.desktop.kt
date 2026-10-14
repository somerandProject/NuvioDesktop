package com.nuvio.app.core.auth

import java.util.prefs.Preferences

internal actual object SupabaseSessionStorage {
    private const val SESSION_KEY = "supabase_session"

    private val preferences: Preferences by lazy {
        Preferences.userRoot().node("com/nuvio/app/auth")
    }

    actual fun load(): String? = preferences.get(SESSION_KEY, null)

    actual fun save(value: String) {
        preferences.put(SESSION_KEY, value)
    }

    actual fun clear() {
        preferences.remove(SESSION_KEY)
    }
}
