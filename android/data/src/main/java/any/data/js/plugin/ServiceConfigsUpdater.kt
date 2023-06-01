package any.data.js.plugin

import any.data.entity.ServiceConfig
import any.data.json.Json
import any.data.json.fromJson

interface ServiceConfigsUpdater {
    fun update(configsJson: String)

    companion object {
        /**
         * Update service configs from the configs json
         *
         * @throws IllegalArgumentException If configs json is invalid
         */
        fun updateConfigsFromJson(
            json: Json,
            current: List<ServiceConfig>?,
            updated: String,
        ): List<ServiceConfig> {
            if (current.isNullOrEmpty()) {
                throw IllegalArgumentException(
                    "Cannot update configs: No configs defined in manifest.json"
                )
            }

            val currConfigMap = current.associateBy { it.key }.toMutableMap()

            val newConfigMap = json.fromJson<Map<String, Any?>>(updated)
            if (newConfigMap.isNullOrEmpty()) {
                throw IllegalArgumentException(
                    "Cannot update configs: Failed to parse configs object"
                )
            }

            for ((key, value) in newConfigMap) {
                val currConfig = currConfigMap[key] ?: throw IllegalArgumentException(
                    "Failed to update config '$key': Field is not defined in manifest.json"
                )
                val newConfig = currConfig.copy(value = value)
                currConfigMap[key] = newConfig
            }

            return currConfigMap.values.toList()
        }
    }
}