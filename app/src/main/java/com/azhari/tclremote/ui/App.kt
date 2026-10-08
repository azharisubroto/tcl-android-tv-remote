package com.azhari.tclremote.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azhari.tclremote.RemoteViewModel

@Composable
fun App(vm: RemoteViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    if (ui.tv == null) {
        PickerScreen(vm)
    } else {
        BackHandler { vm.leave() }
        RemoteScreen(vm, ui)
    }
}
