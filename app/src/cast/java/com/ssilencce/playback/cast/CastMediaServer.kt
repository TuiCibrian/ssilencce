package com.ssilencce.playback.cast

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Minimal HTTP/1.1 server used only while a Cast session is active. It exposes the current queue's
 * audio files and artwork to the Cast receiver over the local network. Every resource URL carries a
 * random token so no filesystem path or content id is guessable, and only registered resources are
 * served.
 */
internal class CastMediaServer(
    private val context: Context,
) {
    private class MediaSource(
        val uri: Uri?,
        val file: File?,
        val mimeType: String,
    )

    private val sources = ConcurrentHashMap<String, MediaSource>()
    private val running = AtomicBoolean(false)
    private var executor: ExecutorService? = null
    private var serverSocket: ServerSocket? = null
    private var acceptThread: Thread? = null

    @Volatile
    private var baseUrl: String? = null

    /** Starts the server on a random port bound to all interfaces. */
    fun start(): Boolean {
        if (running.get()) return baseUrl != null
        val address = findLocalAddress() ?: return false
        val socket =
            try {
                ServerSocket(0)
            } catch (e: IOException) {
                Log.w(TAG, "Could not open the cast media server", e)
                return false
            }
        val pool =
            Executors.newCachedThreadPool { runnable ->
                Thread(runnable, "ssilencce-cast-http").apply { isDaemon = true }
            }
        executor = pool
        serverSocket = socket
        baseUrl = "http://$address:${socket.localPort}"
        running.set(true)
        acceptThread =
            Thread({ acceptLoop(socket, pool) }, "ssilencce-cast-http-accept").apply {
                isDaemon = true
                start()
            }
        return true
    }

    /** Registers an audio file and returns its URL path (without the leading slash). */
    fun registerAudio(
        uri: Uri,
        mimeType: String,
    ): String? {
        if (!running.get()) return null
        val file =
            uri.path
                ?.takeIf { uri.scheme == "file" }
                ?.let { File(it) }
                ?.takeIf { it.isFile && it.canRead() }
        val key = "audio/${UUID.randomUUID()}"
        sources["/$key"] = MediaSource(uri = uri.takeIf { file == null }, file = file, mimeType = mimeType)
        return key
    }

    /** Registers an artwork URI and returns its URL path (without the leading slash). */
    fun registerArtwork(uri: Uri): String? {
        if (!running.get()) return null
        val key = "art/${UUID.randomUUID()}"
        sources["/$key"] = MediaSource(uri = uri, file = null, mimeType = "image/jpeg")
        return key
    }

    fun urlFor(path: String): String? = baseUrl?.let { "$it/$path" }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        baseUrl = null
        runCatching { serverSocket?.close() }
        serverSocket = null
        acceptThread = null
        sources.clear()
        executor?.shutdownNow()
        executor = null
    }

    private fun acceptLoop(
        socket: ServerSocket,
        pool: ExecutorService,
    ) {
        while (running.get()) {
            val client =
                try {
                    socket.accept()
                } catch (e: IOException) {
                    break
                }
            try {
                pool.execute { handleClient(client) }
            } catch (e: RejectedExecutionException) {
                runCatching { client.close() }
            }
        }
    }

    private fun handleClient(client: Socket) {
        try {
            client.use { socket ->
                socket.soTimeout = CLIENT_TIMEOUT_MS
                val input = BufferedInputStream(socket.getInputStream())
                val output = BufferedOutputStream(socket.getOutputStream())
                val requestLine = readLine(input)
                if (requestLine.isNullOrEmpty()) return
                val parts = requestLine.split(' ')
                if (parts.size < 2) {
                    writeStatus(output, "400 Bad Request")
                    return
                }
                val method = parts[0].uppercase()
                val path = parts[1].substringBefore('?')
                var rangeHeader: String? = null
                while (true) {
                    val line = readLine(input) ?: break
                    if (line.isEmpty()) break
                    val separator = line.indexOf(':')
                    if (separator > 0) {
                        val name = line.substring(0, separator).trim()
                        if (name.equals("Range", ignoreCase = true)) {
                            rangeHeader = line.substring(separator + 1).trim()
                        }
                    }
                }
                if (method != "GET" && method != "HEAD") {
                    writeStatus(output, "405 Method Not Allowed", allow = "GET, HEAD")
                    return
                }
                val source =
                    sources[path.removePrefix("/")] ?: run {
                        writeStatus(output, "404 Not Found")
                        return
                    }
                serve(source, method, rangeHeader, output)
            }
        } catch (e: IOException) {
            Log.d(TAG, "Cast media connection closed", e)
        } catch (e: Exception) {
            Log.w(TAG, "Cast media request failed", e)
        }
    }

    private fun serve(
        source: MediaSource,
        method: String,
        rangeHeader: String?,
        output: OutputStream,
    ) {
        val resource =
            try {
                openResource(source)
            } catch (e: Exception) {
                Log.w(TAG, "Could not open cast media source", e)
                null
            }
        if (resource == null) {
            writeStatus(output, "404 Not Found")
            return
        }
        resource.use { opened ->
            val size = opened.length
            val requestedRange = parseRange(rangeHeader, size)
            val start = requestedRange?.first ?: 0L
            val end = requestedRange?.second ?: if (size >= 0) size - 1 else -1L
            val isPartial = requestedRange != null
            val contentLength = if (size >= 0) (end - start + 1).coerceAtLeast(0) else -1L
            val header =
                buildString {
                    append(if (isPartial) "HTTP/1.1 206 Partial Content\r\n" else "HTTP/1.1 200 OK\r\n")
                    append("Content-Type: ${source.mimeType}\r\n")
                    append("Accept-Ranges: bytes\r\n")
                    if (isPartial && size >= 0) {
                        append("Content-Range: bytes $start-$end/$size\r\n")
                    }
                    if (contentLength >= 0) {
                        append("Content-Length: $contentLength\r\n")
                    }
                    append("Cache-Control: no-store\r\n")
                    append("Connection: close\r\n")
                    append("\r\n")
                }
            output.write(header.toByteArray(Charsets.US_ASCII))
            if (method == "HEAD") {
                output.flush()
                return
            }
            opened.openAt(start).use { stream ->
                copyBounded(stream, output, contentLength)
            }
            output.flush()
        }
    }

    private fun openResource(source: MediaSource): OpenResource? {
        val file = source.file
        if (file != null) {
            return OpenResource(file.length()) { start -> FileInputStream(file).apply { channel.position(start) } }
        }
        val uri = source.uri ?: return null
        val afd =
            context.contentResolver.openAssetFileDescriptor(uri, "r")
                ?: return null
        val length = afd.length
        if (length < 0) {
            // Some providers do not expose a length, so the stream is served without range
            // support and without a Content-Length header.
            runCatching { afd.close() }
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            return OpenResource(length = -1L, closeable = stream) { start ->
                skipFully(stream, start)
                stream
            }
        }
        return OpenResource(
            length = length,
            closeable = afd,
        ) { start ->
            val stream = afd.createInputStream()
            skipFully(stream, start)
            stream
        }
    }

    private fun skipFully(
        input: InputStream,
        bytes: Long,
    ) {
        var remaining = bytes
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped > 0) {
                remaining -= skipped
            } else if (input.read() < 0) {
                return
            } else {
                remaining--
            }
        }
    }

    private fun parseRange(
        rangeHeader: String?,
        size: Long,
    ): Pair<Long, Long>? {
        if (rangeHeader == null || size < 0 || !rangeHeader.startsWith("bytes=", ignoreCase = true)) return null
        val value = rangeHeader.substringAfter('=').trim()
        if (value.contains(',')) return null
        val dash = value.indexOf('-')
        if (dash < 0) return null
        val startText = value.substring(0, dash).trim()
        val endText = value.substring(dash + 1).trim()
        return try {
            val start: Long
            val end: Long
            if (startText.isEmpty()) {
                val suffix = endText.toLong().coerceAtLeast(0L)
                if (suffix == 0L) return null
                start = (size - suffix).coerceAtLeast(0L)
                end = size - 1
            } else {
                start = startText.toLong()
                end = endText.toLongOrNull()?.coerceAtMost(size - 1) ?: (size - 1)
            }
            if (start < 0 || start > end || start >= size) null else start to end
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun copyBounded(
        input: InputStream,
        output: OutputStream,
        remaining: Long,
    ) {
        val buffer = ByteArray(BUFFER_SIZE)
        var left = remaining
        while (left != 0L) {
            val toRead = if (left < 0) buffer.size else minOf(buffer.size.toLong(), left).toInt()
            val read = input.read(buffer, 0, toRead)
            if (read < 0) break
            output.write(buffer, 0, read)
            if (left > 0) left -= read
        }
    }

    private fun writeStatus(
        output: OutputStream,
        status: String,
        allow: String? = null,
    ) {
        val response =
            buildString {
                append("HTTP/1.1 $status\r\n")
                if (allow != null) append("Allow: $allow\r\n")
                append("Content-Length: 0\r\n")
                append("Connection: close\r\n")
                append("\r\n")
            }
        output.write(response.toByteArray(Charsets.US_ASCII))
        output.flush()
    }

    private fun readLine(input: InputStream): String? {
        val builder = StringBuilder()
        while (true) {
            val value = input.read()
            if (value < 0) return if (builder.isEmpty()) null else builder.toString()
            if (value == '\n'.code) return builder.toString().trimEnd('\r')
            if (value == '\r'.code) continue
            if (builder.length >= MAX_HEADER_LINE_LENGTH) return null
            builder.append(value.toChar())
        }
    }

    private fun findLocalAddress(): String? {
        val interfaces =
            runCatching {
                NetworkInterface
                    .getNetworkInterfaces()
                    .toList()
                    .filter { networkInterface ->
                        runCatching { networkInterface.isUp && !networkInterface.isLoopback }.getOrDefault(false)
                    }
            }.getOrDefault(emptyList())
        val addresses =
            interfaces
                .flatMap { networkInterface ->
                    networkInterface.inetAddresses
                        .toList()
                        .filterIsInstance<Inet4Address>()
                        .filter { !it.isLoopbackAddress && !it.isLinkLocalAddress }
                        .mapNotNull { address -> address.hostAddress?.let { host -> networkInterface.name to host } }
                }.filter { (_, host) -> host.contains('.') }

        fun preferred(name: String) = name.startsWith("wlan") || name.startsWith("ap") || name.startsWith("eth")
        return addresses
            .firstOrNull { (name, host) ->
                preferred(name) &&
                    (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172."))
            }?.second
            ?: addresses.firstOrNull { (name, _) -> preferred(name) }?.second
            ?: addresses
                .firstOrNull { (_, host) ->
                    host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")
                }?.second
            ?: addresses.firstOrNull()?.second
    }

    private class OpenResource(
        val length: Long,
        private val closeable: Closeable? = null,
        private val objectFactory: (Long) -> InputStream,
    ) : Closeable {
        fun openAt(start: Long): InputStream = objectFactory(start)

        override fun close() {
            closeable?.close()
        }
    }

    companion object {
        private const val TAG = "CastMediaServer"
        private const val CLIENT_TIMEOUT_MS = 20_000
        private const val MAX_HEADER_LINE_LENGTH = 8 * 1024
        private const val BUFFER_SIZE = 64 * 1024
    }
}
