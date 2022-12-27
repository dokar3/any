package any.data.js.plugin

import any.data.entity.ServiceManifest
import any.data.json.Json
import any.data.repository.ServiceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class LocalServiceConfigsUpdater(
    service: ServiceManifest,
    private val serviceRepository: ServiceRepository,
    private val coroutineScope: CoroutineScope,
    private val json: Json = Json,
) : ServiceConfigsUpdater {
    private val serviceId = service.id

    override fun update(configsJson: String) {
        coroutineScope.launch {
            serviceRepository.updateWithLock(serviceId) {
                try {
                    val updatedConfigs = ServiceConfigsUpdater.updateConfigsFromJson(
                        json = json,
                        current = it.configs,
                        updated = configsJson,
                    )
                    it.copy(configs = updatedConfigs)
                } catch (e: Exception) {
                    DefaultLogPlugin.error(e.message ?: "Cannot update service configs: $e")
                    it
                }
            }
        }
    }
}