package com.azhari.tclremote

import android.annotation.SuppressLint
import android.app.Application
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azhari.tclremote.data.DEFAULT_APPS
import com.azhari.tclremote.data.Tv
import com.azhari.tclremote.data.TvApp
import com.azhari.tclremote.data.TvDiscovery
import com.azhari.tclremote.data.TvStore
import com.azhari.tclremote.protocol.ClientIdentity
import com.azhari.tclremote.protocol.NeedsPairingException
import com.azhari.tclremote.protocol.PairingSession
import com.azhari.tclremote.protocol.RemoteSession
import com.azhari.tclremote.protocol.WrongCodeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed interface Connection {
    data object Idle : Connection
    data object Connecting : Connection
    data object Connected : Connection
    data object NeedsPairing : Connection
    data class Failed(val message: String) : Connection
}

sealed interface Pairing {
    data object Starting : Pairing
    data class EnterCode(val error: String? = null) : Pairing
    data object Checking : Pairing
}

data class Volume(val level: Int, val max: Int, val muted: Boolean)

data class RemoteUi(
    val tv: Tv? = null,
    val connection: Connection = Connection.Idle,
    val pairing: Pairing? = null,
    val isOn: Boolean? = null,
    val volume: Volume? = null,
    val currentApp: String = "",
    /** Goes up each time the TV focuses a text field, so the phone can open its keyboard. */
    val keyboardRequests: Int = 0,
    val voiceSupported: Boolean = true,
    val listening: Boolean = false,
    val notice: String? = null,
)

class RemoteViewModel(app: Application) : AndroidViewModel(app) {
    private val store = TvStore(app)
    private val identity = ClientIdentity(File(app.filesDir, "client.p12"))
    private val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val commands = Dispatchers.IO.limitedParallelism(1)

    private val _ui = MutableStateFlow(RemoteUi())
    val ui: StateFlow<RemoteUi> = _ui.asStateFlow()

    private val _saved = MutableStateFlow(store.savedTvs())
    val saved: StateFlow<List<Tv>> = _saved.asStateFlow()

    private val _apps = MutableStateFlow(DEFAULT_APPS + store.customApps())
    val apps: StateFlow<List<TvApp>> = _apps.asStateFlow()

    val discovered: StateFlow<List<Tv>> = TvDiscovery(app).discover()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @Volatile private var session: RemoteSession? = null
    @Volatile private var pairingSession: PairingSession? = null
    private var connectJob: Job? = null
    private var voiceJob: Job? = null
    private var visible = true

    val isConnected: Boolean get() = _ui.value.connection == Connection.Connected

    init {
        val last = store.lastHost
        _saved.value.firstOrNull { it.host == last }?.let { open(it) }
    }

    // --- TV selection ---

    fun open(tv: Tv) {
        disconnect()
        store.lastHost = tv.host
        _ui.value = RemoteUi(tv = tv)
        connect()
    }

    fun leave() {
        disconnect()
        store.lastHost = null
        _ui.value = RemoteUi()
    }

    fun forget(tv: Tv) {
        store.removeTv(tv)
        _saved.value = store.savedTvs()
    }

    fun onAppVisible() {
        visible = true
        val c = _ui.value.connection
        if (_ui.value.tv != null && (c is Connection.Failed || c == Connection.Idle)) connect()
    }

    /** Closing the link while the app is in the background saves battery; it reconnects on return. */
    fun onAppHidden() {
        visible = false
        if (_ui.value.pairing != null) return
        stopVoice()
        connectJob?.cancel()
        session?.close()
        session = null
        _ui.update { if (it.tv != null) it.copy(connection = Connection.Idle) else it }
    }

    // --- Connection ---

    fun connect() {
        val tv = _ui.value.tv ?: return
        if (_ui.value.connection == Connection.Connecting) return
        _ui.update { it.copy(connection = Connection.Connecting) }
        connectJob = viewModelScope.launch(Dispatchers.IO) { connectWithRetry(tv) }
    }

    private suspend fun connectWithRetry(tv: Tv) {
        var attempt = 0
        while (currentCoroutineContext().isActive) {
            session?.close()
            val s = RemoteSession(identity, tv.host, Build.MODEL, Build.MANUFACTURER, listenerFor(tv))
            try {
                s.connect()
                session = s
                store.saveTv(tv)
                _saved.value = store.savedTvs()
                _ui.update { it.copy(connection = Connection.Connected, voiceSupported = s.supportsVoice) }
                return
            } catch (e: NeedsPairingException) {
                _ui.update { it.copy(connection = Connection.NeedsPairing) }
                startPairing()
                return
            } catch (e: Exception) {
                if (++attempt >= 3 || !currentCoroutineContext().isActive) {
                    _ui.update { it.copy(connection = Connection.Failed(describe(e))) }
                    return
                }
                delay(1_000L * attempt)
            }
        }
    }

    private fun listenerFor(tv: Tv) = object : RemoteSession.Listener {
        override fun onPowerChanged(isOn: Boolean) = updateFor(tv) { it.copy(isOn = isOn) }
        override fun onVolumeChanged(level: Int, max: Int, muted: Boolean) =
            updateFor(tv) { it.copy(volume = Volume(level, max, muted)) }
        override fun onCurrentAppChanged(packageName: String) = updateFor(tv) { it.copy(currentApp = packageName) }
        override fun onKeyboardRequested() = updateFor(tv) { it.copy(keyboardRequests = it.keyboardRequests + 1) }
        override fun onDisconnected(error: Throwable?) {
            if (_ui.value.tv != tv) return
            session = null
            _ui.update { it.copy(connection = Connection.Idle, listening = false) }
            if (visible) connect()
        }
    }

    private fun updateFor(tv: Tv, change: (RemoteUi) -> RemoteUi) = _ui.update { if (it.tv == tv) change(it) else it }

    private fun disconnect() {
        stopVoice()
        connectJob?.cancel()
        session?.close()
        session = null
        pairingSession?.close()
        pairingSession = null
    }

    private fun describe(e: Exception): String = when (e) {
        is ConnectException, is NoRouteToHostException, is SocketTimeoutException, is UnknownHostException ->
            "TV tidak terjangkau. Pastikan TV menyala dan HP ada di Wi‑Fi yang sama."
        else -> "Koneksi ke TV terputus (${e.message ?: e.javaClass.simpleName})."
    }

    // --- Pairing ---

    /** Forgets the old pairing on this side and asks the TV for a new code. */
    fun repair() {
        disconnect()
        _ui.update { it.copy(connection = Connection.NeedsPairing) }
        startPairing()
    }

    private fun startPairing(error: String? = null) {
        val tv = _ui.value.tv ?: return
        _ui.update { it.copy(pairing = Pairing.Starting) }
        viewModelScope.launch(Dispatchers.IO) {
            pairingSession?.close()
            val p = PairingSession(identity, tv.host, deviceName.ifEmpty { "HP Android" })
            try {
                p.start()
                pairingSession = p
                val name = p.tvName
                if (name != null && tv.name == tv.host) {
                    val renamed = tv.copy(name = name)
                    _ui.update { if (it.tv == tv) it.copy(tv = renamed) else it }
                }
                _ui.update { it.copy(pairing = Pairing.EnterCode(error)) }
            } catch (e: Exception) {
                p.close()
                _ui.update {
                    it.copy(
                        pairing = null,
                        connection = Connection.Failed("Tidak bisa memulai pairing. ${describe(e)}"),
                    )
                }
            }
        }
    }

    fun submitCode(code: String) {
        val p = pairingSession ?: return
        _ui.update { it.copy(pairing = Pairing.Checking) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                p.finish(code)
                p.close()
                pairingSession = null
                _ui.update { it.copy(pairing = null, connection = Connection.Idle) }
                connect()
            } catch (e: WrongCodeException) {
                _ui.update { it.copy(pairing = Pairing.EnterCode("Kode tidak cocok. Periksa lagi kode di layar TV.")) }
            } catch (e: Exception) {
                p.close()
                pairingSession = null
                startPairing("Kode ditolak TV. Masukkan kode baru yang muncul di TV.")
            }
        }
    }

    fun cancelPairing() {
        pairingSession?.close()
        pairingSession = null
        _ui.update {
            it.copy(pairing = null, connection = Connection.Failed("Pairing dibatalkan. Ketuk Pasangkan untuk mencoba lagi."))
        }
    }

    // --- Commands ---

    fun key(keyCode: Int) = send { it.sendKey(keyCode) }

    fun sendText(text: String) = send { it.sendText(text) }

    fun launchApp(app: TvApp) = send { it.launchApp(app.link) }

    fun addApp(app: TvApp) {
        store.addApp(app)
        _apps.value = DEFAULT_APPS + store.customApps()
    }

    fun removeApp(app: TvApp) {
        store.removeApp(app)
        _apps.value = DEFAULT_APPS + store.customApps()
    }

    fun isCustomApp(app: TvApp) = app !in DEFAULT_APPS

    fun clearNotice() = _ui.update { it.copy(notice = null) }

    private fun send(action: (RemoteSession) -> Unit) {
        val s = session
        if (s == null) {
            connect()
            return
        }
        viewModelScope.launch(commands) { runCatching { action(s) } }
    }

    // --- Voice ---

    /** Streams the microphone to the TV's voice search until [stopVoice]. Needs RECORD_AUDIO. */
    @SuppressLint("MissingPermission")
    fun startVoice() {
        val s = session ?: return
        if (voiceJob?.isActive == true) return
        _ui.update { it.copy(listening = true) }
        voiceJob = viewModelScope.launch(Dispatchers.IO) {
            var sessionId: Int? = null
            var recorder: AudioRecord? = null
            try {
                sessionId = runCatching { s.startVoice() }.getOrNull()
                if (sessionId == null) {
                    _ui.update { it.copy(notice = "TV tidak merespons perintah suara.") }
                    return@launch
                }
                val minBuffer = AudioRecord.getMinBufferSize(
                    VOICE_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                )
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    VOICE_SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minBuffer, VOICE_CHUNK * 2),
                )
                recorder.startRecording()
                val buffer = ByteArray(VOICE_CHUNK)
                val endAt = System.currentTimeMillis() + VOICE_MAX_MS
                while (isActive && System.currentTimeMillis() < endAt) {
                    val n = recorder.read(buffer, 0, buffer.size)
                    if (n > 0) s.sendVoice(sessionId, buffer.copyOf(n))
                }
            } catch (e: Exception) {
                _ui.update { it.copy(notice = "Mikrofon tidak bisa dipakai.") }
            } finally {
                runCatching { recorder?.stop() }
                recorder?.release()
                val id = sessionId
                if (id != null) withContext(NonCancellable) { runCatching { s.endVoice(id) } }
                _ui.update { it.copy(listening = false) }
            }
        }
    }

    fun stopVoice() {
        voiceJob?.cancel()
    }

    override fun onCleared() {
        disconnect()
    }

    private companion object {
        const val VOICE_SAMPLE_RATE = 8000
        const val VOICE_CHUNK = 8 * 1024
        const val VOICE_MAX_MS = 15_000L
    }
}
