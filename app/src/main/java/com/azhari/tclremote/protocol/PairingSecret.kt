package com.azhari.tclremote.protocol

import java.math.BigInteger
import java.security.MessageDigest
import java.security.interfaces.RSAPublicKey

object PairingSecret {
    private val HEX_CODE = Regex("^[0-9A-F]{6}$")

    fun normalize(code: String): String = code.trim().replace(" ", "").uppercase()

    fun isWellFormed(code: String): Boolean = HEX_CODE.matches(normalize(code))

    /**
     * Returns the secret to send for [code] (the 6 hex characters shown on the TV),
     * or null when the code cannot be right: its first byte is a checksum of the rest.
     */
    fun compute(client: RSAPublicKey, server: RSAPublicKey, code: String): ByteArray? {
        val c = normalize(code)
        if (!HEX_CODE.matches(c)) return null
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(unsignedBytes(client.modulus))
        digest.update(unsignedBytes(client.publicExponent))
        digest.update(unsignedBytes(server.modulus))
        digest.update(unsignedBytes(server.publicExponent))
        digest.update(hexToBytes(c.substring(2)))
        val hash = digest.digest()
        return if ((hash[0].toInt() and 0xff) == c.substring(0, 2).toInt(16)) hash else null
    }

    private fun unsignedBytes(n: BigInteger): ByteArray {
        var hex = n.toString(16)
        if (hex.length % 2 == 1) hex = "0$hex"
        return hexToBytes(hex)
    }

    private fun hexToBytes(hex: String) = ByteArray(hex.length / 2) {
        hex.substring(it * 2, it * 2 + 2).toInt(16).toByte()
    }
}
