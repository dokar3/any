package any.base

import androidx.compose.runtime.Immutable

@Immutable
sealed class UiMessage(val message: String) {
    @Immutable
    class Normal(message: String) : UiMessage(message) {
        override fun toString(): String {
            return "UiMessage.Normal('$message')"
        }
    }

    @Immutable
    class Warn(message: String) : UiMessage(message) {
        override fun toString(): String {
            return "UiMessage.Warn('$message')"
        }
    }

    @Immutable
    class Error(message: String) : UiMessage(message) {
        override fun toString(): String {
            return "UiMessage.Error('$message')"
        }
    }
}