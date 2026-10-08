package com.azhari.tclremote.protocol

import java.io.Closeable
import java.io.IOException
import java.security.interfaces.RSAPublicKey
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket

class WrongCodeException : IOException("Wrong pairing code")
class PairingRejectedException(status: Int) : IOException("TV rejected pairing (status $status)")

/**
 * Pairing on port 6467. [start] makes the TV show a 6-character code,
 * [finish] sends the secret derived from it. Calls block, so run them off the main thread.
 */
class PairingSession(
    private val identity: ClientIdentity,
    private val host: String,
    private val clientName: String,
    private val port: Int = 6467,
) : Closeable {
    private var socket: SSLSocket? = null
    private var serverKey: RSAPublicKey? = null

    /** The TV's name, read from its certificate (e.g. "CN=atvremote/xx/xx/TCL TV/MAC"). */
    var tvName: String? = null
        private set

    fun start() {
        val s = identity.connect(host, port, timeoutMs = 10_000)
        socket = s
        val cert = s.session.peerCertificates[0] as X509Certificate
        serverKey = cert.publicKey as RSAPublicKey
        tvName = nameFromSubject(cert.subjectX500Principal.name)
        exchange(PairingMessages.pairingRequest(clientName), PairingMessages.F_REQUEST_ACK)
        exchange(PairingMessages.options(), PairingMessages.F_OPTIONS)
        exchange(PairingMessages.configuration(), PairingMessages.F_CONFIGURATION_ACK)
    }

    /**
     * Throws [WrongCodeException] when the code fails its checksum; the session stays usable for another try.
     * Throws [PairingRejectedException] when the TV refuses it; pairing must then start over.
     */
    fun finish(code: String) {
        val key = serverKey ?: throw IOException("Pairing not started")
        val secret = PairingSecret.compute(identity.publicKey, key, code) ?: throw WrongCodeException()
        exchange(PairingMessages.secret(secret), PairingMessages.F_SECRET_ACK)
    }

    private fun exchange(message: ByteArray, expectedField: Int) {
        val s = socket ?: throw IOException("Not connected")
        Framing.write(s.outputStream, message)
        val reply = ProtoMessage.parse(Framing.read(s.inputStream))
        val status = reply.int(PairingMessages.F_STATUS)
        if (status != PairingMessages.STATUS_OK) throw PairingRejectedException(status)
        if (!reply.has(expectedField)) throw IOException("Unexpected pairing reply")
    }

    override fun close() {
        runCatching { socket?.close() }
        socket = null
    }

    companion object {
        fun nameFromSubject(subject: String): String? {
            val cn = subject.split(",").map { it.trim() }
                .firstOrNull { it.startsWith("CN=") }?.removePrefix("CN=") ?: return null
            val parts = cn.split("/")
            return if (parts.size > 2) parts[parts.size - 2] else null
        }
    }
}
