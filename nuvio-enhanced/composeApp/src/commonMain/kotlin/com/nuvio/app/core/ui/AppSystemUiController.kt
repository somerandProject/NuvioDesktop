package com.nuvio.app.core.ui

internal expect object AppSystemUiController {
    fun setStatusBarVisible(visible: Boolean)

    /**
     * When active, the app hides every system bar regardless of the status-bar
     * setting, so the player stays fullscreen even if the setting is toggled on.
     */
    fun setImmersiveMode(active: Boolean)
}
