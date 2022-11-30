package any.data.js.plugin

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import java.text.SimpleDateFormat

object DefaultLogPlugin : LogPlugin {
    private const val TAG = "JsLogger"

    private const val MAX_LOG_ITEMS = 10_000

    @SuppressLint("SimpleDateFormat")
    val logDateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS")

    private val messages = object : ArrayList<LogItem>() {
        override fun add(element: LogItem): Boolean {
            if (size == MAX_LOG_ITEMS) {
                removeRange(0, MAX_LOG_ITEMS / 4)
            }
            return super.add(element)
        }

        override fun addAll(elements: Collection<LogItem>): Boolean {
            val s = size + elements.size
            if (s >= MAX_LOG_ITEMS) {
                removeRange(0, s - MAX_LOG_ITEMS + MAX_LOG_ITEMS / 4)
            }
            return super.addAll(elements)
        }

        override fun add(index: Int, element: LogItem) {
            throw UnsupportedOperationException("Call add(T) instead")
        }

        override fun addAll(index: Int, elements: Collection<LogItem>): Boolean {
            throw UnsupportedOperationException("Call add(T) instead")
        }
    }

    private val messagesFlow = MutableSharedFlow<List<LogItem>>(extraBufferCapacity = 1)

    override fun log(message: String) {
        Log.d(TAG, message)
        messages.add(LogItem(message = message, level = LogLevel.Log))
        messagesFlow.tryEmit(messages.toList())
    }

    override fun logWithTag(tag: String, message: String) {
        Log.d(tag, message)
        messages.add(LogItem(message = message, level = LogLevel.Log, tag = tag))
        messagesFlow.tryEmit(messages.toList())
    }

    override fun info(message: String) {
        Log.i(TAG, message)
        messages.add(LogItem(message = message, level = LogLevel.Info))
        messagesFlow.tryEmit(messages.toList())
    }

    override fun infoWithTag(tag: String, message: String) {
        Log.i(tag, message)
        messages.add(LogItem(message = message, level = LogLevel.Info, tag = tag))
        messagesFlow.tryEmit(messages.toList())
    }

    override fun warn(message: String) {
        Log.w(TAG, message)
        messages.add(LogItem(message = message, level = LogLevel.Warn))
        messagesFlow.tryEmit(messages.toList())
    }

    override fun warnWithTag(tag: String, message: String) {
        Log.w(tag, message)
        messages.add(LogItem(message = message, level = LogLevel.Warn, tag = tag))
        messagesFlow.tryEmit(messages.toList())
    }

    override fun error(message: String) {
        Log.e(TAG, message)
        messages.add(LogItem(message = message, level = LogLevel.Error))
        messagesFlow.tryEmit(messages.toList())
    }

    override fun errorWithTag(tag: String, message: String) {
        Log.e(TAG, message)
        messages.add(LogItem(message = message, level = LogLevel.Error, tag = tag))
        messagesFlow.tryEmit(messages.toList())
    }

    fun clear() {
        messages.clear()
        messagesFlow.tryEmit(emptyList())
    }

    fun messages(): List<LogItem> = messages.toList()

    fun messageFlow(): Flow<List<LogItem>> = messagesFlow
        .onStart { emit(messages.toList()) }
        .distinctUntilChanged()

    @Immutable
    data class LogItem(
        val message: String,
        val level: LogLevel,
        val tag: String? = null,
        val time: Long = System.currentTimeMillis(),
    ) {
        override fun toString(): String {
            return "${logDateFormat.format(time)}: $message"
        }
    }

    enum class LogLevel {
        Log,
        Info,
        Warn,
        Error,
    }
}