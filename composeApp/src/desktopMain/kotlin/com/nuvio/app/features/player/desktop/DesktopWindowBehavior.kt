package com.nuvio.app.features.player.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Observable desktop window preferences shared between the settings UI and the
 * application window. Backed by [DesktopWindowModeStorage].
 */
internal object DesktopWindowBehavior {
    private val _alwaysOnTop = MutableStateFlow(DesktopWindowModeStorage.loadAlwaysOnTop())
    val alwaysOnTop: StateFlow<Boolean> = _alwaysOnTop.asStateFlow()

    private val _minimizeToTray = MutableStateFlow(DesktopWindowModeStorage.loadMinimizeToTray())
    val minimizeToTray: StateFlow<Boolean> = _minimizeToTray.asStateFlow()

    fun setAlwaysOnTop(enabled: Boolean) {
        if (_alwaysOnTop.value == enabled) return
        _alwaysOnTop.value = enabled
        DesktopWindowModeStorage.saveAlwaysOnTop(enabled)
    }

    fun setMinimizeToTray(enabled: Boolean) {
        if (_minimizeToTray.value == enabled) return
        _minimizeToTray.value = enabled
        DesktopWindowModeStorage.saveMinimizeToTray(enabled)
    }
}
