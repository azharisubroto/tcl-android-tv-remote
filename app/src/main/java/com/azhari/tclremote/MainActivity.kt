package com.azhari.tclremote

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.azhari.tclremote.protocol.TvKeys
import com.azhari.tclremote.ui.App
import com.azhari.tclremote.ui.TclRemoteTheme

class MainActivity : ComponentActivity() {
    private val vm: RemoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TclRemoteTheme {
                App(vm)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        vm.onAppVisible()
    }

    override fun onStop() {
        super.onStop()
        vm.onAppHidden()
    }

    /** The phone's volume buttons control the TV while connected. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val tvKey = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> TvKeys.VOLUME_UP
            KeyEvent.KEYCODE_VOLUME_DOWN -> TvKeys.VOLUME_DOWN
            else -> null
        }
        if (tvKey != null && vm.isConnected) {
            if (event.action == KeyEvent.ACTION_DOWN) vm.key(tvKey)
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
