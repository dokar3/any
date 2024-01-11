package any.base.model

import androidx.compose.runtime.Immutable

@Immutable
sealed class UiMessage(
    val message: String,
    val id: Long = System.nanoTime(),
) {
    @Immutable
    class Normal(
        message: String,
        id: Long = System.nanoTime(),
    ) : UiMessage(message, id) {
        override fun toString(): String {
            return "UiMessage.Normal('$message')"
        }
    }

    @Immutable
    class Warn(
        message: String,
        id: Long = System.nanoTime(),
    ) : UiMessage(message, id) {
        override fun toString(): String {
            return "UiMessage.Warn('$message')"
        }
    }

    @Immutable
    class Error(
        message: String,
        id: Long = System.nanoTime(),
    ) : UiMessage(message, id) {
        override fun toString(): String {
            return "UiMessage.Error('$message')"
        }
    }
}