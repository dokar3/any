package any.base.testing.preference

import any.base.prefs.PreferenceValue
import any.base.prefs.PreferencesStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class TestPreferencesStore : PreferencesStore {
    private val prefs = mutableMapOf<String, Any?>()

    private val valueChangedFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)

    override val values: MutableMap<String, PreferenceValue<*>> = mutableMapOf()

    override fun get(key: String, defaultValue: Any?): Any? {
        return prefs[key] ?: defaultValue
    }

    override fun put(key: String, value: Any?) {
        prefs[key] = value
        valueChangedFlow.tryEmit(key)
    }

    override fun remove(key: String) {
        prefs.remove(key)
        valueChangedFlow.tryEmit(key)
    }

    override fun valueChangedFlow(): Flow<String> = valueChangedFlow
}