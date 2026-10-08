package com.azhari.tclremote.protocol

import java.io.Closeable
import java.io.IOException
import java.net.SocketException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket
import kotlin.concurrent.thread

/** The TV doesn't recognise this phone's certificate: pair first. */
class NeedsPairingException(cause: Throwable) : IOException("Pairing required", cause)

/**
 * Remote control connection on port 6466. [connect] blocks until the TV is ready;
 * afterwards a background thread answers pings and reports state to [listener].
 * Send methods block briefly on the socket, so call them off the main thread.
 */
class RemoteSession(
    private val identity: ClientIdentity,
    private val host: String,
    private val deviceModel: String,
    private val deviceVendor: String,
    private val listener: Listener,
    private val port: Int = 6466,
) : Closeable {

    interface Listener {
        fun onPowerChanged(isOn: Boolean) {}
        fun onVolumeChanged(level: Int, max: Int, muted: Boolean) {}
        fun onCurrentAppChanged(packageName: String) {}
        fun onKeyboardRequested() {}
        fun onDisconnected(error: Throwable?) {}
    }

    @Volatile private var socket: SSLSocket? = null
    @Volatile private var closed = false
    private val writeLock = Any()
    private var features = RemoteMessages.ALL_FEATURES
    @Volatile private var imeCounter = 0
    @Volatile private var fieldCounter = 0
    private val voiceBegins = ArrayBlockingQueue<Int>(1)

    val supportsVoice: Boolean get() = features and RemoteMessages.FEATURE_VOICE != 0

    fun connect() {
        val s = try {
            identity.connect(host, port, timeoutMs = 5000)
        } catch (e: HandshakeException) {
            throw NeedsPairingException(e)
        }
        socket = s
        try {
            s.soTimeout = 10_000
            while (!handle(ProtoMessage.parse(Framing.read(s.inputStream)))) Unit
            // The TV pings every 5 seconds; a longer silence means the link is gone.
            s.soTimeout = 16_000
        } catch (e: IOException) {
            close()
            // With TLS 1.3 the TV rejects an unknown certificate only after the handshake.
            if (e is SSLException || e is SocketException) throw NeedsPairingException(e)
            throw e
        } catch (e: Exception) {
            close()
            throw e
        }
        thread(name = "tv-remote-reader", isDaemon = true) { readLoop(s) }
    }

    fun sendKey(keyCode: Int, direction: Int = TvKeys.SHORT) = send(RemoteMessages.key(keyCode, direction))

    fun sendText(text: String) {
        if (text.isNotEmpty()) send(RemoteMessages.text(imeCounter, fieldCounter, text))
    }

    /** Opens an app by link (https://…, market://…) or by package name. */
    fun launchApp(linkOrPackage: String) {
        val link = if (linkOrPackage.contains("://")) linkOrPackage else "market://launch?id=$linkOrPackage"
        send(RemoteMessages.appLink(link))
    }

    /** Starts voice search on the TV and returns the voice session id, or null if the TV didn't answer. */
    fun startVoice(timeoutMs: Long = 2000): Int? {
        voiceBegins.clear()
        sendKey(TvKeys.SEARCH)
        val id = voiceBegins.poll(timeoutMs, TimeUnit.MILLISECONDS) ?: return null
        send(RemoteMessages.voiceBegin(id))
        return id
    }

    /** Streams 16-bit mono PCM at 8 kHz. */
    fun sendVoice(sessionId: Int, pcm: ByteArray) {
        var i = 0
        while (i < pcm.size) {
            var chunk = pcm.copyOfRange(i, minOf(pcm.size, i + VOICE_CHUNK_SIZE))
            if (chunk.size < VOICE_CHUNK_MIN_SIZE) chunk = chunk.copyOf(VOICE_CHUNK_MIN_SIZE)
            send(RemoteMessages.voicePayload(sessionId, chunk))
            i += VOICE_CHUNK_SIZE
        }
    }

    fun endVoice(sessionId: Int) = send(RemoteMessages.voiceEnd(sessionId))

    override fun close() {
        closed = true
        runCatching { socket?.close() }
    }

    private fun send(message: ByteArray) {
        val s = socket ?: throw IOException("Not connected")
        try {
            synchronized(writeLock) { Framing.write(s.outputStream, message) }
        } catch (e: IOException) {
            disconnect(e)
            throw e
        }
    }

    private fun readLoop(s: SSLSocket) {
        try {
            while (!closed) handle(ProtoMessage.parse(Framing.read(s.inputStream)))
        } catch (e: Exception) {
            disconnect(e)
        }
    }

    private fun disconnect(error: Throwable) {
        if (closed) return
        close()
        listener.onDisconnected(error)
    }

    /** Handles one message from the TV. Returns true for remote_start, which means the TV is ready. */
    private fun handle(msg: ProtoMessage): Boolean {
        when {
            msg.has(RemoteMessages.F_CONFIGURE) -> {
                val supported = msg.message(RemoteMessages.F_CONFIGURE).int(1)
                if (supported != 0) features = RemoteMessages.ALL_FEATURES and supported
                send(RemoteMessages.configure(features, deviceModel, deviceVendor))
            }
            msg.has(RemoteMessages.F_SET_ACTIVE) -> send(RemoteMessages.setActive(features))
            msg.has(RemoteMessages.F_PING_REQUEST) ->
                send(RemoteMessages.pingResponse(msg.message(RemoteMessages.F_PING_REQUEST).int(1)))
            msg.has(RemoteMessages.F_IME_KEY_INJECT) -> {
                val app = msg.message(RemoteMessages.F_IME_KEY_INJECT).message(1).string(12)
                if (app.isNotEmpty()) listener.onCurrentAppChanged(app)
            }
            msg.has(RemoteMessages.F_IME_BATCH_EDIT) -> {
                val edit = msg.message(RemoteMessages.F_IME_BATCH_EDIT)
                imeCounter = edit.int(1)
                fieldCounter = edit.int(2)
            }
            msg.has(RemoteMessages.F_IME_SHOW_REQUEST) -> listener.onKeyboardRequested()
            msg.has(RemoteMessages.F_VOICE_BEGIN) ->
                voiceBegins.offer(msg.message(RemoteMessages.F_VOICE_BEGIN).int(1))
            msg.has(RemoteMessages.F_SET_VOLUME_LEVEL) -> {
                val v = msg.message(RemoteMessages.F_SET_VOLUME_LEVEL)
                listener.onVolumeChanged(level = v.int(7), max = v.int(6), muted = v.bool(8))
            }
            msg.has(RemoteMessages.F_START) -> {
                listener.onPowerChanged(msg.message(RemoteMessages.F_START).bool(1))
                return true
            }
        }
        return false
    }

    private companion object {
        const val VOICE_CHUNK_SIZE = 20 * 1024
        const val VOICE_CHUNK_MIN_SIZE = 8 * 1024
    }
}
