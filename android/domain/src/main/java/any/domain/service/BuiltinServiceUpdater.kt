package any.domain.service

import any.data.ServiceInstaller
import any.data.entity.ServiceConfig
import any.data.entity.ServiceManifest
import any.data.repository.ServiceRepository
import any.data.source.service.BuiltinServicesLoader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class BuiltinServiceUpdater(
    private val builtinServicesLoader: BuiltinServicesLoader,
    private val serviceRepository: ServiceRepository,
    private val serviceInstaller: ServiceInstaller,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun updateBuiltinServices(): List<ServiceManifest> = withContext(dispatcher) {
        val services = serviceRepository.getDbServices()
        val builtinServices = builtinServicesLoader.loadAll()
        val updatableServices = getUpdatableBuiltinServices(services, builtinServices)
        if (updatableServices.isEmpty()) {
            return@withContext emptyList()
        }
        val builtinMap = builtinServices.associateBy { it.id }
        val updatedServices = updatableServices.map {
            updateServiceFromBuiltin(it, builtinMap[it.originalId]!!)
        }
        serviceRepository.updateDbService(updatedServices)
        updatedServices
    }

    suspend fun getUpdatableBuiltinServices() = withContext(dispatcher) {
        val services = serviceRepository.getDbServices()
        val builtinServices = builtinServicesLoader.loadAll()
        getUpdatableBuiltinServices(services, builtinServices)
    }

    private fun getUpdatableBuiltinServices(
        services: List<ServiceManifest>,
        builtinServices: List<ServiceManifest>,
    ): List<ServiceManifest> {
        if (services.isEmpty() || builtinServices.isEmpty()) {
            return emptyList()
        }
        val builtinMap = builtinServices.associateBy { it.id }
        val updatableDbServices = mutableListOf<ServiceManifest>()
        for (service in services) {
            val builtin = builtinMap[service.originalId]
            if (builtin != null) {
                val serviceConfigMap = service.configs?.associateBy { it.key } ?: emptyMap()
                val partialBuiltin = builtin.copy(
                    id = service.id,
                    name = service.name,
                    configs = builtin.configs?.map {
                        it.copy(value = serviceConfigMap[it.key]?.value ?: it.value)
                    },
                    isEnabled = service.isEnabled,
                    pageKeyOfPage2 = service.pageKeyOfPage2,
                    localResources = service.localResources,
                )
                if (service != partialBuiltin && partialBuiltin.buildTime > service.buildTime) {
                    // Nothing to update
                    updatableDbServices.add(service)
                }
            }
        }
        return updatableDbServices
    }

    private fun updateServiceFromBuiltin(
        service: ServiceManifest,
        builtin: ServiceManifest,
    ): ServiceManifest {
        val localResources = serviceInstaller.installFromAssets(service)
            ?: throw IOException("Cannot extract resources from builtin service: ${service.id}")
        val updated = service.copy(
            originalId = builtin.originalId,
            description = builtin.description,
            developer = builtin.developer,
            developerUrl = builtin.developerUrl,
            developerAvatar = builtin.developerAvatar,
            homepage = builtin.homepage,
            changelog = builtin.changelog,
            version = builtin.version,
            minApiVersion = builtin.minApiVersion,
            maxApiVersion = builtin.maxApiVersion,
            isPageable = builtin.isPageable,
            postsViewType = builtin.postsViewType,
            mediaAspectRatio = builtin.mediaAspectRatio,
            icon = builtin.icon,
            headerImage = builtin.headerImage,
            themeColor = builtin.themeColor,
            darkThemeColor = builtin.darkThemeColor,
            main = builtin.main,
            mainChecksums = builtin.mainChecksums,
            languages = builtin.languages,
            configs = updateServiceConfigs(old = service.configs, new = builtin.configs),
            supportedPostUrls = builtin.supportedPostUrls,
            supportedUserUrls = builtin.supportedUserUrls,
            forceConfigsValidation = builtin.forceConfigsValidation,
            buildTime = builtin.buildTime,
            source = ServiceManifest.Source.Builtin,
            localResources = localResources,
        )
        return updated.toStored()
    }

    private fun updateServiceConfigs(
        old: List<ServiceConfig>?,
        new: List<ServiceConfig>?,
    ): List<ServiceConfig>? {
        if (old.isNullOrEmpty() || new.isNullOrEmpty()) {
            return new
        }
        val oldConfigMap = old.associateBy { it.key }
        return new.map { it.copy(value = oldConfigMap[it.key]?.value ?: it.value) }
    }
}