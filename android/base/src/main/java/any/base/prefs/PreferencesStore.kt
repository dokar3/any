package any.base.prefs

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.Flow

@Stable
interface PreferencesStore {
    val values: MutableMap<String, PreferenceValue<*>>

    fun get(key: String, defaultValue: Any?): Any?

    fun put(key: String, value: Any?)

    fun remove(key: String)

    fun valueChangedFlow(): Flow<String>
}
