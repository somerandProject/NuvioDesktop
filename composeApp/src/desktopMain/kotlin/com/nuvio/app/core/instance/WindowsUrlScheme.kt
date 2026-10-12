package com.nuvio.app.core.instance

import java.io.File

// Registers the "nuvio" URL scheme for this user (HKCU) so browser-launched
// deep links land back in the desktop app. Only "nuvio" is registered: the
// official Nuvio and the Stremio client do not claim it on Windows, while
// "stremio" is commonly owned by the Stremio desktop app and must not be
// hijacked. HKCU needs no elevation, and registration is idempotent.
object WindowsUrlScheme {

    private const val SCHEME = "nuvio"

    fun register() {
        if (!isWindows()) return
        val executable = launcherExecutablePath() ?: return

        runCatching {
            reg(
                "add",
                "HKCU\\Software\\Classes\\$SCHEME",
                "/ve",
                "/d",
                "URL:Nuvio",
                "/f",
            )
            reg(
                "add",
                "HKCU\\Software\\Classes\\$SCHEME",
                "/v",
                "URL Protocol",
                "/d",
                "",
                "/f",
            )
            reg(
                "add",
                "HKCU\\Software\\Classes\\$SCHEME\\shell\\open\\command",
                "/ve",
                "/d",
                "\"$executable\" \"%1\"",
                "/f",
            )
        }
    }

    private fun reg(vararg arguments: String): Boolean = runCatching {
        val process = ProcessBuilder(listOf("reg", *arguments))
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        process.waitFor()
        process.exitValue() == 0
    }.getOrDefault(false)

    // jpackage sets jpackage.app-path to the launched executable; fall back to
    // the process command for Gradle-run development builds.
    private fun launcherExecutablePath(): String? =
        System.getProperty("jpackage.app-path")?.takeIf { it.isNotBlank() }
            ?: ProcessHandle.current().info().command().orElse(null)
                ?.takeIf { command -> File(command).extension.equals("exe", ignoreCase = true) }

    private fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().lowercase().contains("win")
}
