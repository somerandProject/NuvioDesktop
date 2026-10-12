package com.nuvio.app.core.ui

import androidx.compose.ui.Modifier

internal actual fun Modifier.blockMouseBackForwardButtons(): Modifier = this

internal actual fun Modifier.onMouseBackButton(onBack: () -> Unit): Modifier = this

internal actual fun Modifier.onMouseScroll(handler: () -> Unit): Modifier = this
