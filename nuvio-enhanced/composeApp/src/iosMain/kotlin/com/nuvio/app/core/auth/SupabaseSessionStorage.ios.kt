package com.nuvio.app.core.auth

import platform.Foundation.NSUserDefaults

internal actual object SupabaseSessionStorage {
    private const val SESSION_KEY = "supabase_session"

    actual fun load(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(SESSION_KEY)

    actual fun save(value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = SESSION_KEY)
    }

    actual fun clear() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(SESSION_KEY)
    }
}
