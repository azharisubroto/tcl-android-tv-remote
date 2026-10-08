package com.azhari.tclremote.protocol

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

/**
 * A tiny protobuf encoder/decoder, just enough for the Android TV Remote v2 messages.
 * Avoids pulling in the protobuf runtime and code generator.
 */
class ProtoWriter {
    private val out = ByteArrayOutputStream()

    fun int(field: Int, value: Int): ProtoWriter = varint(field, value.toLong())

    fun bool(field: Int, value: Boolean): ProtoWriter = varint(field, if (value) 1 else 0)

    fun string(field: Int, value: String): ProtoWriter = bytes(field, value.toByteArray(Charsets.UTF_8))

    fun bytes(field: Int, value: ByteArray): ProtoWriter {
        tag(field, WIRE_LEN)
        writeVarint(out, value.size.toLong())
        out.write(value)
        return this
    }

    fun message(field: Int, block: ProtoWriter.() -> Unit): ProtoWriter =
        bytes(field, ProtoWriter().apply(block).toByteArray())

    fun toByteArray(): ByteArray = out.toByteArray()

    private fun varint(field: Int, value: Long): ProtoWriter {
        tag(field, WIRE_VARINT)
        writeVarint(out, value)
        return this
    }

    private fun tag(field: Int, wireType: Int) = writeVarint(out, ((field shl 3) or wireType).toLong())
}

/** A decoded message: field number to raw values (Long for varints, ByteArray for length-delimited). */
class ProtoMessage private constructor(private val fields: Map<Int, List<Any>>) {

    fun has(field: Int): Boolean = fields.containsKey(field)

    fun int(field: Int): Int = long(field).toInt()

    fun bool(field: Int): Boolean = long(field) != 0L

    fun string(field: Int): String = bytes(field)?.toString(Charsets.UTF_8) ?: ""

    fun bytes(field: Int): ByteArray? = fields[field]?.lastOrNull() as? ByteArray

    fun message(field: Int): ProtoMessage = bytes(field)?.let { parse(it) } ?: EMPTY

    private fun long(field: Int): Long = fields[field]?.lastOrNull() as? Long ?: 0L

    companion object {
        private val EMPTY = ProtoMessage(emptyMap())

        fun parse(data: ByteArray): ProtoMessage {
            val fields = HashMap<Int, MutableList<Any>>()
            var pos = 0
            fun readVarint(): Long {
                var result = 0L
                var shift = 0
                while (true) {
                    if (pos >= data.size) throw ProtoException("Truncated varint")
                    val b = data[pos++].toInt() and 0xff
                    result = result or ((b and 0x7f).toLong() shl shift)
                    if (b and 0x80 == 0) return result
                    shift += 7
                    if (shift >= 64) throw ProtoException("Varint too long")
                }
            }
            while (pos < data.size) {
                val key = readVarint()
                val field = (key ushr 3).toInt()
                val value: Any = when ((key and 7).toInt()) {
                    WIRE_VARINT -> readVarint()
                    WIRE_LEN -> {
                        val len = readVarint().toInt()
                        if (len < 0 || pos + len > data.size) throw ProtoException("Truncated field")
                        data.copyOfRange(pos, pos + len).also { pos += len }
                    }
                    WIRE_FIXED64 -> { pos += 8; continue }
                    WIRE_FIXED32 -> { pos += 4; continue }
                    else -> throw ProtoException("Unsupported wire type in key $key")
                }
                fields.getOrPut(field) { ArrayList(1) }.add(value)
            }
            return ProtoMessage(fields)
        }
    }
}

class ProtoException(message: String) : java.io.IOException(message)

/** Messages on the wire are prefixed with their length as a varint. */
object Framing {
    fun write(out: OutputStream, message: ByteArray) {
        val buf = ByteArrayOutputStream(message.size + 5)
        writeVarint(buf, message.size.toLong())
        buf.write(message)
        out.write(buf.toByteArray())
        out.flush()
    }

    fun read(input: InputStream): ByteArray {
        var len = 0
        var shift = 0
        while (true) {
            val b = input.read()
            if (b < 0) throw EOFException("Connection closed by TV")
            len = len or ((b and 0x7f) shl shift)
            if (b and 0x80 == 0) break
            shift += 7
            if (shift > 28) throw ProtoException("Bad message length")
        }
        val data = ByteArray(len)
        var read = 0
        while (read < len) {
            val n = input.read(data, read, len - read)
            if (n < 0) throw EOFException("Connection closed by TV")
            read += n
        }
        return data
    }
}

private const val WIRE_VARINT = 0
private const val WIRE_FIXED64 = 1
private const val WIRE_LEN = 2
private const val WIRE_FIXED32 = 5

private fun writeVarint(out: OutputStream, value: Long) {
    var v = value
    while (v and 0x7fL.inv() != 0L) {
        out.write(((v and 0x7f) or 0x80).toInt())
        v = v ushr 7
    }
    out.write(v.toInt())
}
