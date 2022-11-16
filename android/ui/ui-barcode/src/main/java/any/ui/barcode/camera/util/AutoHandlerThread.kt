package any.ui.barcode.camera.util

import android.os.Handler
import android.os.HandlerThread

internal class AutoHandlerThread(
    threadName: String = "BackgroundHandler"
) : HandlerThread(threadName) {
    val handler: Handler

    init {
        start()
        handler = Handler(looper)
    }
}