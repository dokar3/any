package any.base.prefs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

open class PreferenceValue<T>(
    private val store: PreferencesStore,
    private val key: String,
    private val defaultValue: T
) {
    @Suppress("unchecked_cast")
    var value: T
        get() = store.get(key, defaultValue) as T
        set(value) = store.put(key, value)

    fun asFlow(): Flow<T> {
        return store.valueChangedFlow()
            .filter { it == key }
            .map { value }
            .onStart { emit(value) }
            .distinctUntilChanged()
    }

    fun asStateFlow(
        scope: CoroutineScope,
    ): StateFlow<T> {
        return asFlow().stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = value
        )
    }
}


fun intValue(
    store: PreferencesStore,
    key: String,
    defaultValue: Int = 0
) = PreferenceValue(store, key, defaultValue)

fun floatValue(
    store: PreferencesStore,
    key: String,
    defaultValue: Float = 0f
) = PreferenceValue(store, key, defaultValue)

fun longValue(
    store: PreferencesStore,
    key: String,
    defaultValue: Long = 0L
) = PreferenceValue(store, key, defaultValue)

fun boolValue(
    store: PreferencesStore,
    key: String,
    defaultValue: Boolean = false
) = PreferenceValue(store, key, defaultValue)

fun stringValue(
    store: PreferencesStore,
    key: String,
    defaultValue: String
) = PreferenceValue(store, key, defaultValue)

fun nullableStringValue(
    store: PreferencesStore,
    key: String,
    defaultValue: String? = null
) = PreferenceValue(store, key, defaultValue)