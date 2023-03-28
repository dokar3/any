package any.ui.common.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
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
        awaitEachGesture {
            while (this@coroutineScope.isActive) {
                val ev = awaitPointerEvent().changes.firstOrNull() ?: return@awaitEachGesture
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