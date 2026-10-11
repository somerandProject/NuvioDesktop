package com.nuvio.app.features.player.desktop

import java.awt.AWTException
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.ActionListener
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 * Minimal system-tray integration so the window can be hidden to the tray and
 * restored later. Every method is a safe no-op when the platform reports no
 * system tray (headless, or desktops without a tray).
 */
internal object DesktopTrayManager {
    @Volatile
    private var trayIcon: TrayIcon? = null

    val isSupported: Boolean
        get() = runCatching { SystemTray.isSupported() }.getOrDefault(false)

    fun install(
        tooltip: String,
        onRestore: () -> Unit,
        onExit: () -> Unit,
    ) {
        if (!isSupported) return
        if (trayIcon != null) return
        runCatching {
            val image = loadTrayImage()
            val popup = PopupMenu().apply {
                add(MenuItem("Open Nuvio").apply {
                    addActionListener(ActionListener { onRestore() })
                })
                addSeparator()
                add(MenuItem("Quit").apply {
                    addActionListener(ActionListener { onExit() })
                })
            }
            val icon = TrayIcon(image, tooltip, popup).apply {
                isImageAutoSize = true
                addActionListener(ActionListener { onRestore() })
            }
            SystemTray.getSystemTray().add(icon)
            trayIcon = icon
        }.onFailure { error ->
            if (error !is AWTException) throw error
        }
    }

    fun remove() {
        val icon = trayIcon ?: return
        runCatching { SystemTray.getSystemTray().remove(icon) }
        trayIcon = null
    }

    private fun loadTrayImage(): BufferedImage {
        val candidates = listOf(
            "icons/app-icon-original-transparent.png",
            "icons/app-icon-graphite-transparent.png",
        )
        for (path in candidates) {
            val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(path) ?: continue
            stream.use { input ->
                ImageIO.read(input)?.let { return it }
            }
        }
        return BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
    }
}
