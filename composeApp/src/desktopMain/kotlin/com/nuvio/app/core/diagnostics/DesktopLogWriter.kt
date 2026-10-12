package com.nuvio.app.core.diagnostics

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import com.nuvio.app.core.storage.DesktopStorage
import java.io.BufferedWriter
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Writes app logs to a rotating file under `<appdata>/logs` in addition to the
 * platform console writer, so the in-app "View logs" screen has real content.
 * Entries are formatted on the calling thread and written by a single daemon
 * writer thread so log lines never block playback or UI threads on disk I/O.
 */
internal object DesktopLogWriter {
    private const val MaxLogBytes = 2L * 1024L * 1024L
    private const val MaxLogFiles = 3
    private const val LogFileName = "nuvio.log"
    private const val MaxQueuedEntries = 2048

    private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val lock = Any()
    private val writerLock = Any()
    private val entryQueue = ArrayBlockingQueue<String>(MaxQueuedEntries)
    @Volatile
    private var writerThread: Thread? = null

    val logDirectory: Path get() = DesktopStorage.rootDir.resolve("logs")
    val logFile: File get() = logDirectory.resolve(LogFileName).toFile()

    fun install() {
        Logger.setLogWriters(listOf(platformLogWriter(), FileLogWriter))
        ensureWriterThread()
        runCatching {
            Runtime.getRuntime().addShutdownHook(
                Thread(
                    {
                        flushRemaining()
                        writerThread?.interrupt()
                    },
                    "nuvio-log-flush",
                ),
            )
        }
    }

    fun readTail(maxLines: Int = 2000): String {
        flushRemaining()
        val file = logFile
        if (!file.exists()) return ""
        return runCatching {
            val lines = file.readLines()
            lines.takeLast(maxLines).joinToString("\n")
        }.getOrDefault("")
    }

    fun clear() {
        flushRemaining()
        synchronized(lock) {
            logFile.delete()
            File(logDirectory.toFile(), "$LogFileName.1").delete()
            File(logDirectory.toFile(), "$LogFileName.2").delete()
        }
    }

    private fun flushRemaining() {
        val deadline = System.currentTimeMillis() + 1000L
        while (entryQueue.isNotEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(5L)
        }
    }

    private fun ensureWriterThread() {
        val existing = writerThread
        if (existing?.isAlive == true) return
        synchronized(writerLock) {
            if (writerThread?.isAlive == true) return
            writerThread = Thread(::writerLoop, "nuvio-log-writer").apply {
                isDaemon = true
                start()
            }
        }
    }

    private fun writerLoop() {
        var writer: BufferedWriter? = null
        try {
            while (true) {
                val entry = entryQueue.poll(250L, TimeUnit.MILLISECONDS) ?: continue
                synchronized(lock) {
                    val directory = logDirectory.toFile()
                    if (!directory.exists()) directory.mkdirs()
                    val file = logFile
                    if (file.exists() && file.length() > MaxLogBytes) {
                        runCatching { writer?.close() }
                        writer = null
                        rotate()
                    }
                    val current = writer ?: file.bufferedWriter().also { writer = it }
                    current.write(entry)
                    current.flush()
                }
            }
        } catch (_: InterruptedException) {
            // Shutdown hook interrupted the writer after draining the queue.
        } finally {
            runCatching { writer?.close() }
        }
    }

    private object FileLogWriter : LogWriter() {
        override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
            platformLogWriter().log(severity, message, tag, throwable)
            writeLine(severity, message, tag, throwable)
        }
    }

    private fun writeLine(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val entry = buildString {
            append(timestampFormat.format(LocalDateTime.now()))
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
        ensureWriterThread()
        if (!entryQueue.offer(entry)) {
            runCatching { entryQueue.offer(entry, 50L, TimeUnit.MILLISECONDS) }
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
