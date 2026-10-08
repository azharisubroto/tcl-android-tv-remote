package com.azhari.tclremote.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azhari.tclremote.RemoteViewModel
import com.azhari.tclremote.data.Tv

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerScreen(vm: RemoteViewModel) {
    val saved by vm.saved.collectAsStateWithLifecycle()
    val discovered by vm.discovered.collectAsStateWithLifecycle()
    var manual by rememberSaveable { mutableStateOf(false) }
    val fresh = discovered.filter { d -> saved.none { it.host == d.host } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Pilih TV") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { manual = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Masukkan IP") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (saved.isNotEmpty()) {
                item { SectionTitle("TV tersimpan") }
                items(saved, key = { "saved-" + it.host }) { tv ->
                    TvRow(tv, onClick = { vm.open(tv) }, onDelete = { vm.forget(tv) })
                }
            }
            item { SectionTitle("TV di Wi‑Fi ini") }
            if (fresh.isEmpty()) {
                item { Searching() }
            } else {
                items(fresh, key = { "found-" + it.host }) { tv -> TvRow(tv, onClick = { vm.open(tv) }) }
            }
        }
    }

    if (manual) {
        ManualIpDialog(
            onDismiss = { manual = false },
            onConnect = { ip ->
                manual = false
                vm.open(Tv(ip, ip))
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun TvRow(tv: Tv, onClick: () -> Unit, onDelete: (() -> Unit)? = null) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Tv, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(tv.name, style = MaterialTheme.typography.titleMedium)
                if (tv.name != tv.host) {
                    Text(tv.host, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Hapus ${tv.name}") }
            }
        }
    }
}

@Composable
private fun Searching() {
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(Modifier.size(32.dp))
        Spacer(Modifier.size(12.dp))
        Text("Mencari TV…", style = MaterialTheme.typography.bodyLarge)
        Text(
            "Pastikan TV menyala dan HP terhubung ke Wi‑Fi yang sama dengan TV.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ManualIpDialog(onDismiss: () -> Unit, onConnect: (String) -> Unit) {
    var ip by rememberSaveable { mutableStateOf("") }
    val valid = ip.trim().isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alamat IP TV") },
        text = {
            Column {
                Text(
                    "Lihat di TV: Setelan › Jaringan & Internet › nama Wi‑Fi Anda.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.size(12.dp))
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it.filter { c -> c.isDigit() || c == '.' } },
                    placeholder = { Text("192.168.1.20") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                )
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onConnect(ip.trim()) }) { Text("Sambungkan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
