package com.nuvio.app.features.settings

import androidx.compose.runtime.Composable

/**
 * Desktop window behaviour that only exists on the desktop target:
 * always-on-top, minimize-to-tray and the in-app log viewer. Implemented as a
 * no-op on other platforms so the shared Advanced settings page stays portable.
 */
internal expect object DesktopWindowSettings {
    val isSupported: Boolean

    fun alwaysOnTop(): Boolean
    fun setAlwaysOnTop(enabled: Boolean)

    fun minimizeToTray(): Boolean
    fun setMinimizeToTray(enabled: Boolean)

    fun logsDirectoryPath(): String?
    fun readLogs(): String
    fun clearLogs()
}

@Composable
internal expect fun DesktopWindowSettingsSection(isTablet: Boolean)
