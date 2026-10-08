package com.azhari.tclremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.view.HapticFeedbackConstants
import com.azhari.tclremote.RemoteViewModel
import com.azhari.tclremote.Volume
import com.azhari.tclremote.data.TvApp
import com.azhari.tclremote.protocol.TvKeys
import kotlin.math.abs

// ---------------------------------------------------------------- Remote tab

@Composable
fun MainPad(onKey: (Int) -> Unit, voice: VoiceControl, volume: Volume?) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Labeled("Power") {
                IconKey(Icons.Default.PowerSettingsNew, "Power", { onKey(TvKeys.POWER) }, color = PowerRed, contentColor = Color.White)
            }
            Labeled("Input") { IconKey(Icons.AutoMirrored.Filled.Input, "Sumber input", { onKey(TvKeys.TV_INPUT) }) }
            Labeled("Setelan") { IconKey(Icons.Default.Settings, "Setelan TV", { onKey(TvKeys.SETTINGS) }) }
            if (voice.supported) {
                Labeled(if (voice.listening) "Bicara…" else "Tahan") {
                    PressSurface(
                        label = "Tahan untuk bicara",
                        onPress = voice.start,
                        onRelease = voice.stop,
                        modifier = Modifier.size(60.dp),
                        color = if (voice.listening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (voice.listening) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    ) { Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(27.dp)) }
                }
            }
        }

        DPad(onKey)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Labeled("Kembali") { IconKey(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", { onKey(TvKeys.BACK) }) }
            Labeled("Beranda") { IconKey(Icons.Default.Home, "Beranda", { onKey(TvKeys.HOME) }) }
            Labeled("Menu") { IconKey(Icons.Default.Menu, "Menu", { onKey(TvKeys.MENU) }) }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Rocker("VOL", up = { onKey(TvKeys.VOLUME_UP) }, down = { onKey(TvKeys.VOLUME_DOWN) }, upLabel = "Volume naik", downLabel = "Volume turun")
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Labeled(if (volume?.muted == true) "Bisu" else "Bisukan") {
                    IconKey(Icons.AutoMirrored.Filled.VolumeOff, "Bisukan", { onKey(TvKeys.VOLUME_MUTE) })
                }
                if (volume != null && volume.max > 0) {
                    Text(
                        "Volume ${volume.level}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            Rocker("CH", up = { onKey(TvKeys.CHANNEL_UP) }, down = { onKey(TvKeys.CHANNEL_DOWN) }, upLabel = "Saluran berikutnya", downLabel = "Saluran sebelumnya")
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconKey(Icons.Default.SkipPrevious, "Sebelumnya", { onKey(TvKeys.MEDIA_PREVIOUS) }, size = 52.dp)
            IconKey(Icons.Default.FastRewind, "Mundur", { onKey(TvKeys.MEDIA_REWIND) }, size = 52.dp)
            IconKey(Icons.Default.PlayArrow, "Putar atau jeda", { onKey(TvKeys.MEDIA_PLAY_PAUSE) }, size = 52.dp)
            IconKey(Icons.Default.Stop, "Berhenti", { onKey(TvKeys.MEDIA_STOP) }, size = 52.dp)
            IconKey(Icons.Default.FastForward, "Maju", { onKey(TvKeys.MEDIA_FAST_FORWARD) }, size = 52.dp)
            IconKey(Icons.Default.SkipNext, "Berikutnya", { onKey(TvKeys.MEDIA_NEXT) }, size = 52.dp)
        }
    }
}

@Composable
private fun DPad(onKey: (Int) -> Unit) {
    Box(
        Modifier.size(264.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        val arrow = Icons.Default.KeyboardArrowUp
        val clear = Color.Transparent
        val tint = MaterialTheme.colorScheme.onSurface
        IconKey(arrow, "Atas", { onKey(TvKeys.DPAD_UP) }, Modifier.align(Alignment.TopCenter).padding(top = 6.dp), size = 80.dp, repeat = true, color = clear, contentColor = tint)
        IconKey(arrow, "Bawah", { onKey(TvKeys.DPAD_DOWN) }, Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp), size = 80.dp, repeat = true, color = clear, contentColor = tint, iconModifier = Modifier.rotate(180f))
        IconKey(arrow, "Kiri", { onKey(TvKeys.DPAD_LEFT) }, Modifier.align(Alignment.CenterStart).padding(start = 6.dp), size = 80.dp, repeat = true, color = clear, contentColor = tint, iconModifier = Modifier.rotate(-90f))
        IconKey(arrow, "Kanan", { onKey(TvKeys.DPAD_RIGHT) }, Modifier.align(Alignment.CenterEnd).padding(end = 6.dp), size = 80.dp, repeat = true, color = clear, contentColor = tint, iconModifier = Modifier.rotate(90f))
        TextKey(
            "OK",
            { onKey(TvKeys.DPAD_CENTER) },
            Modifier.align(Alignment.Center).size(100.dp),
            label = "OK",
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun Rocker(title: String, up: () -> Unit, down: () -> Unit, upLabel: String, downLabel: String) {
    Column(
        Modifier.width(68.dp).clip(RoundedCornerShape(34.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconKey(Icons.Default.Add, upLabel, up, size = 68.dp, repeat = true)
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        IconKey(Icons.Default.Remove, downLabel, down, size = 68.dp, repeat = true)
    }
}

// ---------------------------------------------------------------- Touchpad tab

@Composable
fun TouchPad(onKey: (Int) -> Unit) {
    val key by rememberUpdatedState(onKey)
    val view = LocalView.current
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        key(TvKeys.DPAD_CENTER)
                    })
                }
                .pointerInput(Unit) {
                    val step = 56.dp.toPx()
                    var acc = Offset.Zero
                    detectDragGestures(onDragStart = { acc = Offset.Zero }) { change, drag ->
                        change.consume()
                        acc += drag
                        while (abs(acc.x) >= step || abs(acc.y) >= step) {
                            acc = if (abs(acc.x) > abs(acc.y)) {
                                key(if (acc.x > 0) TvKeys.DPAD_RIGHT else TvKeys.DPAD_LEFT)
                                Offset(acc.x - step * Math.signum(acc.x), 0f)
                            } else {
                                key(if (acc.y > 0) TvKeys.DPAD_DOWN else TvKeys.DPAD_UP)
                                Offset(0f, acc.y - step * Math.signum(acc.y))
                            }
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Geser untuk berpindah\nKetuk untuk OK",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Labeled("Kembali") { IconKey(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", { onKey(TvKeys.BACK) }) }
            Labeled("Beranda") { IconKey(Icons.Default.Home, "Beranda", { onKey(TvKeys.HOME) }) }
            Labeled("Putar/Jeda") { IconKey(Icons.Default.PlayArrow, "Putar atau jeda", { onKey(TvKeys.MEDIA_PLAY_PAUSE) }) }
            Labeled("Vol −") { IconKey(Icons.Default.Remove, "Volume turun", { onKey(TvKeys.VOLUME_DOWN) }, repeat = true) }
            Labeled("Vol +") { IconKey(Icons.Default.Add, "Volume naik", { onKey(TvKeys.VOLUME_UP) }, repeat = true) }
        }
    }
}

// ---------------------------------------------------------------- Numbers tab

@Composable
fun NumberPad(onKey: (Int) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { n -> DigitKey(n, onKey, Modifier.weight(1f)) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PressSurface("Saluran terakhir", { onKey(TvKeys.LAST_CHANNEL) }, Modifier.weight(1f).height(64.dp), shape = RoundedCornerShape(20.dp)) {
                Icon(Icons.Default.Replay, contentDescription = null)
            }
            DigitKey(0, onKey, Modifier.weight(1f))
            PressSurface("Info", { onKey(TvKeys.INFO) }, Modifier.weight(1f).height(64.dp), shape = RoundedCornerShape(20.dp)) {
                Icon(Icons.Default.Info, contentDescription = null)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ColorKey("Merah", Color(0xFFE53935), TvKeys.PROG_RED, onKey, Modifier.weight(1f))
            ColorKey("Hijau", Color(0xFF43A047), TvKeys.PROG_GREEN, onKey, Modifier.weight(1f))
            ColorKey("Kuning", Color(0xFFFDD835), TvKeys.PROG_YELLOW, onKey, Modifier.weight(1f))
            ColorKey("Biru", Color(0xFF1E88E5), TvKeys.PROG_BLUE, onKey, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Labeled("Panduan") { IconKey(Icons.Default.LiveTv, "Panduan acara", { onKey(TvKeys.GUIDE) }) }
            Labeled("Subtitle") { IconKey(Icons.Default.Subtitles, "Subtitle", { onKey(TvKeys.CAPTIONS) }) }
            Labeled("CH −") { IconKey(Icons.Default.Remove, "Saluran sebelumnya", { onKey(TvKeys.CHANNEL_DOWN) }) }
            Labeled("CH +") { IconKey(Icons.Default.Add, "Saluran berikutnya", { onKey(TvKeys.CHANNEL_UP) }) }
        }
    }
}

@Composable
private fun DigitKey(n: Int, onKey: (Int) -> Unit, modifier: Modifier) {
    TextKey("$n", { onKey(TvKeys.digit(n)) }, modifier.height(64.dp), shape = RoundedCornerShape(20.dp))
}

@Composable
private fun ColorKey(name: String, color: Color, key: Int, onKey: (Int) -> Unit, modifier: Modifier) {
    PressSurface(name, { onKey(key) }, modifier.height(40.dp), shape = RoundedCornerShape(20.dp), color = color) {}
}

// ---------------------------------------------------------------- Apps tab

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppsPad(vm: RemoteViewModel, currentApp: String) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(false) }
    var removing by rememberSaveable { mutableStateOf<String?>(null) }
    val view = LocalView.current

    LazyVerticalGrid(
        columns = GridCells.Adaptive(104.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (currentApp.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CurrentAppCard(
                    currentApp,
                    saved = apps.any { it.link == currentApp },
                    onSave = { adding = true },
                )
            }
        }
        items(apps, key = { it.link }) { app ->
            Card(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .combinedClickable(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            vm.launchApp(app)
                        },
                        onLongClick = { if (vm.isCustomApp(app)) removing = app.link },
                    ),
            ) {
                Column(
                    Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    AppBadge(app.name)
                    Spacer(Modifier.height(8.dp))
                    Text(app.name, style = MaterialTheme.typography.labelLarge, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        item {
            Card(modifier = Modifier.aspectRatio(1f).clip(MaterialTheme.shapes.medium).combinedClickable(onClick = { adding = true })) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Tambah", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    if (adding) {
        AddAppDialog(
            initialLink = currentApp.takeIf { cur -> apps.none { it.link == cur } } ?: "",
            onAdd = { vm.addApp(it); adding = false },
            onDismiss = { adding = false },
        )
    }
    removing?.let { link ->
        val app = apps.firstOrNull { it.link == link }
        if (app == null) {
            removing = null
        } else {
            ConfirmDialog(
                title = "Hapus ${app.name}?",
                text = "Pintasan ini akan dihapus dari daftar.",
                confirm = "Hapus",
                onConfirm = { vm.removeApp(app); removing = null },
                onDismiss = { removing = null },
            )
        }
    }
}

@Composable
private fun CurrentAppCard(packageName: String, saved: Boolean, onSave: () -> Unit) {
    Card {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Sedang dibuka di TV", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(packageName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!saved) OutlinedButton(onClick = onSave) { Text("Simpan") }
        }
    }
}

@Composable
private fun AppBadge(name: String) {
    val palette = listOf(Color(0xFFE53935), Color(0xFF8E24AA), Color(0xFF1E88E5), Color(0xFF00897B), Color(0xFFF4511E), Color(0xFF3949AB))
    val color = palette[Math.floorMod(name.hashCode(), palette.size)]
    Surface(shape = CircleShape, color = color, contentColor = Color.White, modifier = Modifier.size(44.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
