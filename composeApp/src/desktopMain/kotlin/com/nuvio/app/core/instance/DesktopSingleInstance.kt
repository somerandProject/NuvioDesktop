package com.nuvio.app.core.instance

import com.nuvio.app.core.storage.DesktopStorage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

// NuvioDev and the official Nuvio share %APPDATA%/Nuvio, so the lock file and
// port are NuvioDev-specific: launching official Nuvio must never be treated
// as "this app is already running".
private const val LOCK_FILE_NAME = "nuvio_dev_instance.lock"
private const val RELAY_PORT = 49_872
private const val FORWARD_TIMEOUT_MS = 1_500

/**
 * Single-instance guard.
 *
 * The first launch holds an OS-level file lock for the process lifetime and
 * listens on a loopback socket. Any later launch forwards its URL arguments
 * (e.g. "nuvio://auth/trakt?...") to the running instance and exits instead of
 * starting a second full app that would race over the same stores and tray.
 *
 * [acquire] returns true when the caller should continue as the primary
 * instance; false when this launch was forwarded to the running instance and
 * the process should exit.
 */
object DesktopSingleInstance {

    private var processLock: FileLockHolder? = null
    private var serverSocket: ServerSocket? = null

    private val _incomingUrls = MutableSharedFlow<String>(extraBufferCapacity = 64)

    /** URLs handed over by secondary launches, for the primary to handle. */
    val incomingUrls: SharedFlow<String> = _incomingUrls

    fun acquire(launchArgs: List<String>): Boolean {
        val lockFile = resolveLockFile()
        val holder = runCatching { FileLockHolder.acquire(lockFile) }.getOrNull()
            // Locking unavailable (exotic filesystem): behave as before and allow.
            ?: return true
        if (!holder.isPrimary) {
            holder.close()
            forwardToPrimary(launchArgs)
            return false
        }

        // The lock stays valid as long as the JVM runs; keep the holder alive.
        processLock = holder
        startRelayListener()
        WindowsUrlScheme.register()
        return true
    }

    private fun forwardToPrimary(args: List<String>) {
        runCatching {
            Socket(InetAddress.getLoopbackAddress(), RELAY_PORT).use { socket ->
                socket.soTimeout = FORWARD_TIMEOUT_MS
                val payload = args.filter { it.isNotBlank() }.joinToString(separator = "\n")
                socket.getOutputStream().use { output ->
                    output.write(payload.toByteArray(StandardCharsets.UTF_8))
                    output.flush()
                }
            }
        }
        // Delivery is best-effort: the file lock already guarantees the older
        // instance owns the app, so the secondary exits either way.
    }

    private fun startRelayListener() {
        val server = runCatching {
            ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(InetAddress.getLoopbackAddress(), RELAY_PORT), 4)
            }
        }.getOrNull() ?: return
        serverSocket = server
        thread(name = "nuvio-dev-instance-relay", isDaemon = true) {
            while (!server.isClosed) {
                val client = runCatching { server.accept() }.getOrNull() ?: break
                handleRelayClient(client)
            }
        }
    }

    private fun handleRelayClient(client: Socket) {
        try {
            val lines = client.getInputStream().use { input ->
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
                    buildList {
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line.isNotBlank()) add(line.trim())
                        }
                    }
                }
            }
            lines.forEach { url -> _incomingUrls.tryEmit(url) }
        } catch (_: Exception) {
            // Malformed or truncated payload: the guard itself already worked.
        } finally {
            runCatching { client.close() }
        }
    }

    private fun resolveLockFile(): File =
        DesktopStorage.rootDir.resolve(LOCK_FILE_NAME).toFile()
}

private class FileLockHolder private constructor(
    private val raf: RandomAccessFile,
    private val lock: FileLock?,
) {
    val isPrimary: Boolean = lock != null

    fun close() {
        runCatching { lock?.release() }
        runCatching { raf.close() }
    }

    companion object {
        fun acquire(file: File): FileLockHolder {
            val raf = RandomAccessFile(file, "rw")
            val lock = try {
                raf.channel.tryLock()
            } catch (_: OverlappingFileLockException) {
                null
            }
            return FileLockHolder(raf, lock)
        }
    }
}
