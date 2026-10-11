package com.nuvio.app.features.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import com.nuvio.app.core.diagnostics.DesktopLogWriter
import com.nuvio.app.core.ui.DialogButton
import com.nuvio.app.core.ui.DialogButtons
import com.nuvio.app.core.ui.DialogButtonStyle
import com.nuvio.app.core.ui.DialogSurface
import com.nuvio.app.features.player.desktop.DesktopTrayManager
import com.nuvio.app.features.player.desktop.DesktopWindowBehavior
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_close
import nuvio.composeapp.generated.resources.settings_advanced_section_window
import nuvio.composeapp.generated.resources.settings_window_always_on_top
import nuvio.composeapp.generated.resources.settings_window_always_on_top_description
import nuvio.composeapp.generated.resources.settings_window_clear_logs
import nuvio.composeapp.generated.resources.settings_window_logs
import nuvio.composeapp.generated.resources.settings_window_logs_description
import nuvio.composeapp.generated.resources.settings_window_minimize_to_tray
import nuvio.composeapp.generated.resources.settings_window_minimize_to_tray_description
import nuvio.composeapp.generated.resources.settings_window_minimize_to_tray_description_unsupported
import nuvio.composeapp.generated.resources.settings_window_view_logs
import org.jetbrains.compose.resources.stringResource
import java.awt.Desktop
import java.io.File

internal actual object DesktopWindowSettings {
    actual val isSupported: Boolean = true

    actual fun alwaysOnTop(): Boolean = DesktopWindowBehavior.alwaysOnTop.value
    actual fun setAlwaysOnTop(enabled: Boolean) = DesktopWindowBehavior.setAlwaysOnTop(enabled)

    actual fun minimizeToTray(): Boolean = DesktopWindowBehavior.minimizeToTray.value
    actual fun setMinimizeToTray(enabled: Boolean) = DesktopWindowBehavior.setMinimizeToTray(enabled)

    actual fun logsDirectoryPath(): String? =
        runCatching { DesktopLogWriter.logDirectory.toFile().absolutePath }.getOrNull()

    actual fun readLogs(): String = DesktopLogWriter.readTail()

    actual fun clearLogs() = DesktopLogWriter.clear()
}

@Composable
internal actual fun DesktopWindowSettingsSection(isTablet: Boolean) {
    var alwaysOnTop by remember { mutableStateOf(DesktopWindowSettings.alwaysOnTop()) }
    var minimizeToTray by remember { mutableStateOf(DesktopWindowSettings.minimizeToTray()) }
    var showLogs by remember { mutableStateOf(false) }
    val traySupported = remember { DesktopTrayManager.isSupported }

    SettingsSection(
        title = stringResource(Res.string.settings_advanced_section_window),
        isTablet = isTablet,
    ) {
        SettingsGroup(isTablet = isTablet) {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_window_always_on_top),
                description = stringResource(Res.string.settings_window_always_on_top_description),
                checked = alwaysOnTop,
                isTablet = isTablet,
                onCheckedChange = {
                    alwaysOnTop = it
                    DesktopWindowSettings.setAlwaysOnTop(it)
                },
            )
            SettingsGroupDivider(isTablet = isTablet)
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_window_minimize_to_tray),
                description = stringResource(
                    if (traySupported) {
                        Res.string.settings_window_minimize_to_tray_description
                    } else {
                        Res.string.settings_window_minimize_to_tray_description_unsupported
                    },
                ),
                checked = minimizeToTray && traySupported,
                enabled = traySupported,
                isTablet = isTablet,
                onCheckedChange = {
                    minimizeToTray = it
                    DesktopWindowSettings.setMinimizeToTray(it)
                },
            )
            SettingsGroupDivider(isTablet = isTablet)
            SettingsNavigationRow(
                title = stringResource(Res.string.settings_window_logs),
                description = stringResource(Res.string.settings_window_logs_description),
                icon = Icons.Rounded.Description,
                isTablet = isTablet,
                trailingContent = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.OpenInNew,
                        contentDescription = stringResource(Res.string.settings_window_view_logs),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                onClick = { showLogs = true },
            )
        }
    }

    if (showLogs) {
        LogViewerDialog(
            isTablet = isTablet,
            onDismiss = { showLogs = false },
        )
    }
}

@Composable
private fun LogViewerDialog(
    isTablet: Boolean,
    onDismiss: () -> Unit,
) {
    var logs by remember { mutableStateOf(DesktopWindowSettings.readLogs()) }
    DialogSurface(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.settings_window_logs),
    ) {
        Text(
            text = logs.ifBlank { "No logs yet." },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DialogButtons {
            DialogButton(
                text = stringResource(Res.string.settings_window_clear_logs),
                onClick = {
                    DesktopWindowSettings.clearLogs()
                    logs = ""
                },
            )
            SettingsLogFolderButton()
            DialogButton(
                text = stringResource(Res.string.action_close),
                onClick = onDismiss,
                style = DialogButtonStyle.Primary,
            )
        }
    }
}

@Composable
private fun SettingsLogFolderButton() {
    val path = remember { DesktopWindowSettings.logsDirectoryPath() }
    if (path == null) return
    DialogButton(
        text = stringResource(Res.string.settings_window_view_logs),
        onClick = {
            runCatching {
                val directory = File(path)
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(directory)
                }
            }
        },
    )
}
