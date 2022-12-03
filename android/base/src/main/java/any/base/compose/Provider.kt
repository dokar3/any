package any.base.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.autoSaver
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun <T> rememberProvider(initialValue: () -> T): Provider<T> {
    return remember { Provider(initialValue()) }
}

@Composable
@Suppress("UNCHECKED_CAST")
fun <T, Saveable : Any> rememberSaveableProvider(
    vararg inputs: Any?,
    saver: Saver<T, Saveable> = autoSaver<T>() as Saver<T, Saveable>,
    key: String? = null,
    init: () -> T
): Provider<T> {
    return rememberSaveable(
        inputs = inputs,
        saver = Saver(
            save = {
                with(saver) {
                    save(it.value)
                }
            },
            restore = {
                saver.restore(it)?.let { value ->
                    Provider(value)
                }
            },
        ),
        key = key,
    ) {
        Provider(init())
    }
}

@Stable
class Provider<T>(internal var value: T) {
    fun get(): T {
        return value ?: throw IllegalStateException(
            "No value is provided, call provide() to set a value first"
        )
    }

    fun provide(value: T) {
        this.value = value
    }
}