package com.nuvio.app.core.auth

import android.content.Context
import android.content.SharedPreferences

internal actual object SupabaseSessionStorage {
    private const val PREFERENCES_NAME = "nuvio_supabase_session"
    private const val SESSION_KEY = "session"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    actual fun load(): String? = preferences?.getString(SESSION_KEY, null)

    actual fun save(value: String) {
        preferences?.edit()?.putString(SESSION_KEY, value)?.apply()
    }

    actual fun clear() {
        preferences?.edit()?.remove(SESSION_KEY)?.apply()
    }
}
