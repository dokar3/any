package any.base

import android.os.SystemClock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.Executors

class AutoCleaner<T>(
    dispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher(),
    private val currTimeProvider: () -> Long = { SystemClock.uptimeMillis() },
    private val aliveTime: Long = 1000L * 60,
    private val onClean: (T) -> Unit,
) {
    private val coroutineScope = CoroutineScope(dispatcher)

    private val mutex = Mutex()

    private val queue = mutableMapOf<T, Long>()

    private var cleanJob: Job? = null

    private var isRunning = false

    init {
        require(aliveTime >= 0) { "aliveTime cannot be negative" }
    }

    fun enqueue(item: T) {
        coroutineScope.launch {
            if (aliveTime == 0L) {
                onClean(item)
                return@launch
            }
            val now = currTimeProvider()
            mutex.withLock {
                queue[item] = now + aliveTime
                if (!isRunning) {
                    run()
                }
            }
        }
    }

    fun remove(item: T) {
        coroutineScope.launch {
            mutex.withLock {
                queue.remove(item)
            }
        }
    }

    fun contains(item: T): Boolean {
        return queue.containsKey(item)
    }

    private fun run() {
        if (cleanJob?.isActive == true) {
            return
        }
        isRunning = true
        cleanJob = coroutineScope.launch {
            while (queue.isNotEmpty()) {
                val closestEntityToClean = queue.minByOrNull { it.value } ?: break
                val item = closestEntityToClean.key
                val cleanTime = closestEntityToClean.value
                val now = currTimeProvider()
                if (cleanTime <= now) {
                    mutex.withLock {
                        if (queue.remove(item) != null) {
                            onClean(item)
                        }
                    }
                } else {
                    delay(cleanTime - now)
                }
            }
        }
        isRunning = true
    }
}