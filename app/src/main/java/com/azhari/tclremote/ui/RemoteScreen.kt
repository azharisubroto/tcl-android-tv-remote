package com.azhari.tclremote.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.azhari.tclremote.Connection
import com.azhari.tclremote.RemoteUi
import com.azhari.tclremote.RemoteViewModel

private val TABS = listOf("Remote", "Touchpad", "Angka", "Aplikasi")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(vm: RemoteViewModel, ui: RemoteUi) {
    val tv = ui.tv ?: return
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var keyboard by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(ui.keyboardRequests) { if (ui.keyboardRequests > 0) keyboard = true }
    LaunchedEffect(ui.notice) {
        ui.notice?.let {
            vm.clearNotice()
            snackbar.showSnackbar(it)
        }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val voice = VoiceControl(
        supported = ui.voiceSupported,
        listening = ui.listening,
        start = {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) vm.startVoice() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
        },
        stop = vm::stopVoice,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(tv.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        StatusLine(ui)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = vm::leave) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Ganti TV")
                    }
                },
                actions = {
                    IconButton(onClick = { keyboard = true }) {
                        Icon(Icons.Default.Keyboard, contentDescription = "Ketik di TV")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ConnectionBanner(ui, onRetry = vm::connect, onRepair = vm::repair)
            TabRow(selectedTabIndex = tab) {
                TABS.forEachIndexed { i, title ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title, maxLines = 1) })
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    0 -> MainPad(onKey = vm::key, voice = voice, volume = ui.volume)
                    1 -> TouchPad(onKey = vm::key)
                    2 -> NumberPad(onKey = vm::key)
                    else -> AppsPad(vm, ui.currentApp)
                }
            }
        }
    }

    if (keyboard) KeyboardSheet(onSend = vm::sendText, onKey = vm::key, onDismiss = { keyboard = false })
    ui.pairing?.let { PairingDialog(it, tvName = tv.name, onSubmit = vm::submitCode, onCancel = vm::cancelPairing) }
}

class VoiceControl(val supported: Boolean, val listening: Boolean, val start: () -> Unit, val stop: () -> Unit)

@Composable
private fun StatusLine(ui: RemoteUi) {
    val (text, color) = when (ui.connection) {
        Connection.Connected -> (if (ui.isOn == false) "Terhubung · TV siaga" else "Terhubung") to Color(0xFF34A853)
        Connection.Connecting -> "Menghubungkan…" to MaterialTheme.colorScheme.onSurfaceVariant
        Connection.NeedsPairing -> "Perlu dipasangkan" to MaterialTheme.colorScheme.tertiary
        is Connection.Failed -> "Tidak terhubung" to MaterialTheme.colorScheme.error
        Connection.Idle -> "Tidak terhubung" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.size(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ConnectionBanner(ui: RemoteUi, onRetry: () -> Unit, onRepair: () -> Unit) {
    when (val c = ui.connection) {
        Connection.Connecting -> LinearProgressIndicator(Modifier.fillMaxWidth())
        is Connection.Failed -> Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        ) {
            Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
                Text(c.message, style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onRepair) { Text("Pasangkan ulang") }
                    TextButton(onClick = onRetry) { Text("Coba lagi") }
                }
            }
        }
        else -> Unit
    }
}
