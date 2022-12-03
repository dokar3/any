package any.base.compose

import androidx.compose.runtime.Stable

@Stable
data class StableHolder<T>(val value: T)