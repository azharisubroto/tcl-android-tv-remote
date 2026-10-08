package com.azhari.tclremote.protocol

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.net.InetSocketAddress
import java.net.Socket
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

/**
 * The phone's self-signed client certificate. The TV remembers it after pairing,
 * so it is created once and kept in [file].
 */
class ClientIdentity(private val file: File) {
    private val password = "atvremote".toCharArray()
    private var key: PrivateKey? = null
    private var cert: X509Certificate? = null
    private var context: SSLContext? = null

    val publicKey: RSAPublicKey
        get() = certificate().publicKey as RSAPublicKey

    @Synchronized
    fun sslContext(): SSLContext {
        context?.let { return it }
        val ks = KeyStore.getInstance("PKCS12")
        ks.load(null, null)
        ks.setKeyEntry(ALIAS, privateKey(), password, arrayOf(certificate()))
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(ks, password)
        return SSLContext.getInstance("TLS").also {
            it.init(kmf.keyManagers, arrayOf(TrustAnyCertificate), SecureRandom())
            context = it
        }
    }

    /** Opens a TLS connection; a failed TLS handshake is reported as [HandshakeException]. TVs use self-signed certificates, so any server certificate is accepted. */
    fun connect(host: String, port: Int, timeoutMs: Int = 5000): SSLSocket {
        val raw = Socket()
        try {
            raw.connect(InetSocketAddress(host, port), timeoutMs)
            raw.tcpNoDelay = true
            val socket = sslContext().socketFactory.createSocket(raw, host, port, true) as SSLSocket
            socket.useClientMode = true
            socket.soTimeout = timeoutMs
            try {
                socket.startHandshake()
            } catch (e: IOException) {
                throw HandshakeException(e)
            }
            return socket
        } catch (e: Exception) {
            runCatching { raw.close() }
            throw e
        }
    }

    @Synchronized
    private fun privateKey(): PrivateKey = key ?: load().let { key!! }

    @Synchronized
    private fun certificate(): X509Certificate = cert ?: load().let { cert!! }

    private fun load() {
        if (file.exists()) {
            runCatching {
                val ks = KeyStore.getInstance("PKCS12")
                file.inputStream().use { ks.load(it, password) }
                key = ks.getKey(ALIAS, password) as PrivateKey
                cert = ks.getCertificate(ALIAS) as X509Certificate
                return
            }
        }
        generate()
    }

    private fun generate() {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val pair = kpg.generateKeyPair()
        val now = System.currentTimeMillis()
        val name = X500Name("CN=atvremote/TCL Remote")
        val builder = JcaX509v3CertificateBuilder(
            name,
            BigInteger.valueOf(now),
            Date(now - TimeUnit.DAYS.toMillis(1)),
            Date(now + TimeUnit.DAYS.toMillis(365L * 20)),
            name,
            pair.public,
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(pair.private)
        val certificate = JcaX509CertificateConverter().getCertificate(builder.build(signer))

        val ks = KeyStore.getInstance("PKCS12")
        ks.load(null, null)
        ks.setKeyEntry(ALIAS, pair.private, password, arrayOf(certificate))
        file.parentFile?.mkdirs()
        val tmp = File(file.path + ".tmp")
        tmp.outputStream().use { ks.store(it, password) }
        tmp.renameTo(file)
        key = pair.private
        cert = certificate
    }

    private object TrustAnyCertificate : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    private companion object {
        const val ALIAS = "client"
    }
}

/** The TCP connection worked but TLS failed, usually because the TV doesn't know this phone's certificate. */
class HandshakeException(cause: Throwable) : IOException(cause.message, cause)
