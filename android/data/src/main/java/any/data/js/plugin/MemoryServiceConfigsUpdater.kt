package any.data.js.plugin

import any.data.entity.ServiceManifest
import any.data.json.Json

class MemoryServiceConfigsUpdater(
    private val latest: () -> ServiceManifest,
    private val update: (ServiceManifest) -> Unit,
    private val json: Json = Json,
) : ServiceConfigsUpdater {
    override fun update(configsJson: String) {
        try {
            val updatedConfigs = ServiceConfigsUpdater.updateConfigsFromJson(
                json = json,
                current = latest().configs,
                updated = configsJson,
            )
            update(latest().copy(configs = updatedConfigs))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}