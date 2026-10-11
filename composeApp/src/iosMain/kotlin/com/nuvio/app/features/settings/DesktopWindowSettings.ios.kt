package com.nuvio.app.features.settings

import androidx.compose.runtime.Composable

internal actual object DesktopWindowSettings {
    actual val isSupported: Boolean = false

    actual fun alwaysOnTop(): Boolean = false
    actual fun setAlwaysOnTop(enabled: Boolean) = Unit

    actual fun minimizeToTray(): Boolean = false
    actual fun setMinimizeToTray(enabled: Boolean) = Unit

    actual fun logsDirectoryPath(): String? = null
    actual fun readLogs(): String = ""
    actual fun clearLogs() = Unit
}

@Composable
internal actual fun DesktopWindowSettingsSection(isTablet: Boolean) = Unit
