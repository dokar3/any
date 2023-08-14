package any.base.prefs

import android.content.SharedPreferences
import androidx.annotation.DoNotInline
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class SharedPreferencesStore(
    private val preferences: SharedPreferences
) : PreferencesStore {
    private val valueChangedFlow = MutableSharedFlow<String>(extraBufferCapacity = 2)

    private val onValueChanged = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != null) {
            valueChangedFlow.tryEmit(key)
        }
    }

    init {
        registerValueChangeListener()
    }

    // This will prevent the OnSharedPreferenceChangeListener member variable from
    // being converted to a local variable, as a result, the listener does not get
    // garbage collected after the function executes.
    // More details on StackOverflow: https://stackoverflow.com/a/3104265
    @DoNotInline
    fun registerValueChangeListener() {
        preferences.registerOnSharedPreferenceChangeListener(onValueChanged)
    }

    fun unregisterValueChangeListener() {
        preferences.unregisterOnSharedPreferenceChangeListener(onValueChanged)
    }

    override fun get(key: String, defaultValue: Any?): Any? {
        return try {
            when (defaultValue) {
                null -> getString(key, null)
                is Int -> getInt(key, defaultValue)
                is Float -> getFloat(key, defaultValue)
                is Long -> getLong(key, defaultValue)
                is Boolean -> getBoolean(key, defaultValue)
                is String -> getString(key, defaultValue)
                else -> throw IllegalArgumentException(
                    "Unsupported default value: " +
                            "$defaultValue, type: ${defaultValue::class.java}"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    override val values: MutableMap<String, PreferenceValue<*>> = mutableMapOf()

    override fun put(key: String, value: Any?) {
        when (value) {
            null -> remove(key)
            is Int -> putInt(key, value)
            is Float -> putFloat(key, value)
            is Long -> putLong(key, value)
            is Boolean -> putBoolean(key, value)
            is String -> putString(key, value)
            else -> throw IllegalArgumentException(
                "Unsupported value: $value, " +
                        "type: ${value::class.java}"
            )
        }
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    override fun valueChangedFlow(): Flow<String> {
        return valueChangedFlow
    }

    private fun getInt(key: String, defaultValue: Int): Int {
        return preferences.getInt(key, defaultValue)
    }

    private fun putInt(key: String, value: Int) {
        preferences.edit().putInt(key, value).apply()
    }

    private fun getFloat(key: String, defaultValue: Float): Float {
        return preferences.getFloat(key, defaultValue)
    }

    private fun putFloat(key: String, value: Float) {
        preferences.edit().putFloat(key, value).apply()
    }

    private fun getLong(key: String, defaultValue: Long): Long {
        return preferences.getLong(key, defaultValue)
    }

    private fun putLong(key: String, value: Long) {
        preferences.edit().putLong(key, value).apply()
    }

    private fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return preferences.getBoolean(key, defaultValue)
    }

    private fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    private fun getString(key: String, defaultValue: String?): String? {
        return preferences.getString(key, defaultValue)
    }

    private fun putString(key: String, value: String?) {
        preferences.edit().putString(key, value).apply()
    }
}