package com.nuvio.app.core.ui

import androidx.compose.ui.Modifier

/**
 * Consumes the mouse Back/Forward buttons so they never leak into the UI as
 * clicks or navigation. Desktop-only behavior; no-op on touch platforms.
 */
internal expect fun Modifier.blockMouseBackForwardButtons(): Modifier

/**
 * Runs [onBack] when the (unconsumed) mouse Back button is pressed.
 * Desktop-only behavior; no-op on touch platforms.
 */
internal expect fun Modifier.onMouseBackButton(onBack: () -> Unit): Modifier

/**
 * Runs [handler] when the mouse wheel scrolls. Desktop-only behavior;
 * no-op on touch platforms.
 */
internal expect fun Modifier.onMouseScroll(handler: () -> Unit): Modifier
