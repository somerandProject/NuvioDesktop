package com.nuvio.app.core.diagnostics

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import com.nuvio.app.core.storage.DesktopStorage
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes app logs to a rotating file under `<appdata>/logs` in addition to the
 * platform console writer, so the in-app "View logs" screen has real content.
 */
internal object DesktopLogWriter {
    private const val MaxLogBytes = 2L * 1024L * 1024L
    private const val MaxLogFiles = 3
    private const val LogFileName = "nuvio.log"

    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val lock = Any()

    val logDirectory: Path get() = DesktopStorage.rootDir.resolve("logs")
    val logFile: File get() = logDirectory.resolve(LogFileName).toFile()

    fun install() {
        Logger.setLogWriters(listOf(platformLogWriter(), FileLogWriter))
    }

    fun readTail(maxLines: Int = 2000): String {
        val file = logFile
        if (!file.exists()) return ""
        return runCatching {
            val lines = file.readLines()
            lines.takeLast(maxLines).joinToString("\n")
        }.getOrDefault("")
    }

    fun clear() {
        synchronized(lock) {
            logFile.delete()
            File(logDirectory.toFile(), "$LogFileName.1").delete()
            File(logDirectory.toFile(), "$LogFileName.2").delete()
        }
    }

    private object FileLogWriter : LogWriter() {
        override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
            platformLogWriter().log(severity, message, tag, throwable)
            writeLine(severity, message, tag, throwable)
        }
    }

    private fun writeLine(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        synchronized(lock) {
            runCatching {
                val directory = logDirectory.toFile()
                if (!directory.exists()) directory.mkdirs()
                val file = logFile
                if (file.exists() && file.length() > MaxLogBytes) {
                    rotate()
                }
                val entry = buildString {
                    append(timestampFormat.format(Date()))
                    append(' ')
                    append(severity.name.padEnd(7))
                    if (tag.isNotBlank()) append(" [").append(tag).append(']')
                    append(' ')
                    append(message)
                    if (throwable != null) {
                        append('\n')
                        append(stackTraceOf(throwable))
                    }
                    append('\n')
                }
                file.appendText(entry)
            }
        }
    }

    private fun rotate() {
        val directory = logDirectory.toFile()
        File(directory, "$LogFileName.${MaxLogFiles - 1}").delete()
        for (index in MaxLogFiles - 2 downTo 1) {
            val source = File(directory, "$LogFileName.$index")
            if (source.exists()) source.renameTo(File(directory, "$LogFileName.${index + 1}"))
        }
        logFile.renameTo(File(directory, "$LogFileName.1"))
    }

    private fun stackTraceOf(throwable: Throwable): String {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        return writer.toString().trimEnd()
    }
}
