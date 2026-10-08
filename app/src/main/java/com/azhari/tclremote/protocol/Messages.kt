package com.azhari.tclremote.protocol

/** Builders for the pairing (polo) messages sent on port 6467. Field numbers follow polo.proto. */
object PairingMessages {
    const val STATUS_OK = 200

    // OuterMessage fields
    const val F_STATUS = 2
    const val F_REQUEST_ACK = 11
    const val F_OPTIONS = 20
    const val F_CONFIGURATION_ACK = 31
    const val F_SECRET_ACK = 41

    private const val ENCODING_HEXADECIMAL = 3
    private const val ROLE_INPUT = 1
    private const val CODE_LENGTH = 6

    fun pairingRequest(clientName: String) = outer {
        message(10) {
            string(1, "atvremote")
            string(2, clientName)
        }
    }

    fun options() = outer {
        message(20) {
            message(1) { encoding() }
            int(3, ROLE_INPUT)
        }
    }

    fun configuration() = outer {
        message(30) {
            message(1) { encoding() }
            int(2, ROLE_INPUT)
        }
    }

    fun secret(secret: ByteArray) = outer {
        message(40) { bytes(1, secret) }
    }

    private fun ProtoWriter.encoding() {
        int(1, ENCODING_HEXADECIMAL)
        int(2, CODE_LENGTH)
    }

    private fun outer(block: ProtoWriter.() -> Unit): ByteArray =
        ProtoWriter().int(1, 2).int(F_STATUS, STATUS_OK).apply(block).toByteArray()
}

/** Builders for RemoteMessage (port 6466). Field numbers follow remotemessage.proto. */
object RemoteMessages {
    // RemoteMessage fields
    const val F_CONFIGURE = 1
    const val F_SET_ACTIVE = 2
    const val F_ERROR = 3
    const val F_PING_REQUEST = 8
    const val F_IME_KEY_INJECT = 20
    const val F_IME_BATCH_EDIT = 21
    const val F_IME_SHOW_REQUEST = 22
    const val F_VOICE_BEGIN = 30
    const val F_START = 40
    const val F_SET_VOLUME_LEVEL = 50

    // Feature bits announced in remote_configure / remote_set_active
    const val FEATURE_PING = 1
    const val FEATURE_KEY = 2
    const val FEATURE_IME = 4
    const val FEATURE_VOICE = 8
    const val FEATURE_POWER = 32
    const val FEATURE_VOLUME = 64
    const val FEATURE_APP_LINK = 512
    const val ALL_FEATURES = FEATURE_PING or FEATURE_KEY or FEATURE_IME or FEATURE_VOICE or
        FEATURE_POWER or FEATURE_VOLUME or FEATURE_APP_LINK

    fun configure(features: Int, model: String, vendor: String) = remote {
        message(1) {
            int(1, features)
            message(2) {
                string(1, model)
                string(2, vendor)
                int(3, 1)
                string(4, "1")
                string(5, "atvremote")
                string(6, "1.0.0")
            }
        }
    }

    fun setActive(features: Int) = remote { message(2) { int(1, features) } }

    fun pingResponse(value: Int) = remote { message(9) { int(1, value) } }

    fun key(keyCode: Int, direction: Int) = remote {
        message(10) {
            int(1, keyCode)
            int(2, direction)
        }
    }

    fun text(imeCounter: Int, fieldCounter: Int, text: String) = remote {
        message(21) {
            int(1, imeCounter)
            int(2, fieldCounter)
            message(3) {
                int(1, 1)
                message(2) {
                    int(1, text.length - 1)
                    int(2, text.length - 1)
                    string(3, text)
                }
            }
        }
    }

    fun voiceBegin(sessionId: Int) = remote { message(30) { int(1, sessionId) } }

    fun voicePayload(sessionId: Int, samples: ByteArray) = remote {
        message(31) {
            int(1, sessionId)
            bytes(2, samples)
        }
    }

    fun voiceEnd(sessionId: Int) = remote { message(32) { int(1, sessionId) } }

    fun appLink(link: String) = remote { message(90) { string(1, link) } }

    private fun remote(block: ProtoWriter.() -> Unit): ByteArray = ProtoWriter().apply(block).toByteArray()
}
