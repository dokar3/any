package any.base.compose

import androidx.compose.runtime.Immutable

@Immutable
data class ImmutableHolder<T>(val value: T)