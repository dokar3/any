package any.ui.common.gesture

import android.util.Log
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

suspend fun PointerInputScope.detectEventActions(
    onDown: (() -> Unit)? = null,
    onUp: (() -> Unit)? = null
) {
    coroutineScope {
        forEachGesture {
            awaitPointerEventScope {
                while (this@coroutineScope.isActive) {
                    val ev = awaitPointerEvent().changes.firstOrNull()
                    if (ev == null) {
                        Log.d("GestureImage", "empty events, skip")
                        return@awaitPointerEventScope
                    }
                    if (ev.changedToDownIgnoreConsumed()) {
                        onDown?.invoke()
                    }
                    if (ev.changedToUpIgnoreConsumed()) {
                        if (ev.pressed != ev.previousPressed) ev.consume()
                        onUp?.invoke()
                    }
                }
            }
        }
    }
}