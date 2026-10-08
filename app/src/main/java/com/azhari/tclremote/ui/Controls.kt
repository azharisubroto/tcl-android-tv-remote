package com.azhari.tclremote.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A remote button. Fires on touch-down, like a physical remote, with a short vibration.
 * With [repeat], holding it keeps sending (for arrows and volume).
 */
@Composable
fun PressSurface(
    label: String,
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
    repeat: Boolean = false,
    onRelease: (() -> Unit)? = null,
    shape: Shape = CircleShape,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val currentPress by rememberUpdatedState(onPress)
    val currentRelease by rememberUpdatedState(onRelease)
    var pressed by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .semantics {
                contentDescription = label
                role = Role.Button
                onClick { currentPress(); true }
            }
            .pointerInput(repeat) {
                detectTapGestures(onPress = {
                    pressed = true
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    currentPress()
                    val repeater = if (repeat) {
                        scope.launch {
                            delay(400)
                            while (true) {
                                currentPress()
                                delay(110)
                            }
                        }
                    } else {
                        null
                    }
                    tryAwaitRelease()
                    repeater?.cancel()
                    pressed = false
                    currentRelease?.invoke()
                })
            },
        shape = shape,
        color = if (pressed) MaterialTheme.colorScheme.primaryContainer else color,
        contentColor = if (pressed) MaterialTheme.colorScheme.onPrimaryContainer else contentColor,
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
fun IconKey(
    icon: ImageVector,
    label: String,
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    repeat: Boolean = false,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconModifier: Modifier = Modifier,
) {
    PressSurface(
        label = label,
        onPress = onPress,
        modifier = modifier.size(size),
        repeat = repeat,
        color = color,
        contentColor = contentColor,
    ) {
        Icon(icon, contentDescription = null, modifier = iconModifier.size(size * 0.45f))
    }
}

@Composable
fun TextKey(
    text: String,
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = text,
    shape: Shape = CircleShape,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    PressSurface(label = label, onPress = onPress, modifier = modifier, shape = shape, color = color, contentColor = contentColor) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}

/** A small caption under a button. */
@Composable
fun Labeled(caption: String, content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        content()
        Text(
            caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
