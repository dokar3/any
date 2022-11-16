package any.ui.settings.services.viewmodel

import any.base.R as BaseR
import android.content.Context
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import any.base.AndroidStrings
import any.base.AppVersionProvider
import any.base.DefaultAppVersionProvider
import any.base.Strings
import any.base.UiMessage
import any.base.file.AndroidFileReader
import any.base.file.FileReader
import any.base.prefs.PreferencesStore
import any.base.prefs.preferencesStore
import any.base.prefs.versionCodeIgnoresBuiltinServiceUpdates
import any.data.Comparators
import any.data.ServiceInstaller
import any.data.entity.ServiceManifest
import any.data.repository.ServiceRepository
import any.data.source.service.AssetsServicesLoader
import any.domain.entity.UiServiceManifest
import any.domain.service.BuiltinServiceUpdater
import any.domain.service.toUiManifest
import any.richtext.html.DefaultHtmlParser
import any.richtext.html.HtmlParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ServiceMgtViewModel(
    private val serviceRepository: ServiceRepository,
    private val serviceInstaller: ServiceInstaller,
    private val builtinServiceUpdater: BuiltinServiceUpdater,
    private val strings: Strings,
    private val fileReader: FileReader,
    private val preferencesStore: PreferencesStore,
    private val appVersionProvider: AppVersionProvider,
    private val htmlParser: HtmlParser = DefaultHtmlParser(),
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _servicesUiState = MutableStateFlow(ServicesUiState())
    val servicesUiState: StateFlow<ServicesUiState> = _servicesUiState

    private val _addServiceUiState = MutableStateFlow(AddServiceUiState())
    val addServiceUiState: StateFlow<AddServiceUiState> = _addServiceUiState

    private var allAppendableServices: List<AppendableService>? = null

    private var loadServiceFromManifestUrlJob: Job? = null

    init {
        viewModelScope.launch(workerDispatcher) {
            serviceRepository.changes.collect {
                loadDbServices()
                loadAppendableServices()
            }
        }
        viewModelScope.launch {
            val currVersionCode = appVersionProvider.versionCode
            val ignoredVersionCode = preferencesStore.versionCodeIgnoresBuiltinServiceUpdates.value
            if (currVersionCode == ignoredVersionCode) {
                return@launch
            }
            val services = builtinServiceUpdater.getUpdatableBuiltinServices()
            _servicesUiState.update { it.copy(updatableBuiltinServiceCount = services.size) }
        }
    }

    fun loadDbServices() {
        viewModelScope.launch(workerDispatcher) {
            val services = serviceRepository.loadDbServices()
                // Sorted by enabled state and name
                .sortedWith { o1, o2 ->
                    val t1 = (if (o1.isEnabled) 0 else 1).toString() + o1.name
                    val t2 = (if (o2.isEnabled) 0 else 1).toString() + o2.name
                    Comparators.stringComparator.compare(t1, t2)
                }
                .map { it.toUiManifest(fileReader, htmlParser) }
            _servicesUiState.update {
                it.copy(services = services, selectedServices = emptySet())
            }
        }
    }

    fun selectDbService(service: UiServiceManifest) {
        val selectedServices = _servicesUiState.value.selectedServices.toMutableSet()
        selectedServices.add(service)
        _servicesUiState.update { it.copy(selectedServices = selectedServices) }
    }

    fun unselectDbService(service: UiServiceManifest) {
        val selectedServices = _servicesUiState.value.selectedServices.toMutableSet()
        selectedServices.remove(service)
        _servicesUiState.update { it.copy(selectedServices = selectedServices) }
    }

    fun updateDbService(service: UiServiceManifest) {
        viewModelScope.launch(workerDispatcher) {
            serviceRepository.updateDbService(service.raw)
            loadDbServices()
        }
    }

    fun clearServicesToConfigure() {
        _servicesUiState.update {
            it.copy(servicesToConfigure = emptyList())
        }
    }

    fun setServicesToConfigure(services: List<UiServiceManifest>) {
        viewModelScope.launch {
            val servicesToConfigure = services.map {
                AppendableService(
                    service = it,
                    isAdded = serviceRepository.findDbService(it.toStored().id) != null,
                    onServiceAdded = { loadDbServices() },
                )
            }
            _servicesUiState.update {
                it.copy(servicesToConfigure = servicesToConfigure)
            }
        }
    }

    fun loadServiceToConfigById(serviceId: String) {
        viewModelScope.launch {
            val service = serviceRepository.findDbService(serviceId)
            if (service != null) {
                val configuringService = AppendableService(
                    service = service.toUiManifest(fileReader, htmlParser),
                    isAdded = true,
                    onServiceAdded = { loadDbServices() },
                )
                _servicesUiState.update {
                    it.copy(servicesToConfigure = listOf(configuringService))
                }
            } else {
                _servicesUiState.update {
                    it.copy(servicesToConfigure = emptyList())
                }
            }
        }
    }

    fun loadServiceToConfigByUrl(url: String) {
        loadServiceFromManifestUrlJob?.cancel()
        loadServiceFromManifestUrlJob = viewModelScope.launch {
            _servicesUiState.update {
                it.copy(isLoadingServiceToConfig = true)
            }
            val service = try {
                serviceRepository.loadServiceFromManifestUrl(url)
            } catch (e: Exception) {
                e.printStackTrace()
                _servicesUiState.update {
                    it.copy(message = UiMessage.Error(strings(BaseR.string.cannot_unpack_service)))
                }
                null
            }
            if (service != null) {
                val configuringService = AppendableService(
                    service = service.toUiManifest(fileReader, htmlParser),
                    isAdded = serviceRepository.findDbService(service.toStored().id) != null,
                    onServiceAdded = { loadDbServices() },
                )
                _servicesUiState.update {
                    it.copy(
                        servicesToConfigure = listOf(configuringService),
                        isLoadingServiceToConfig = false,
                    )
                }
            } else {
                _servicesUiState.update {
                    it.copy(
                        servicesToConfigure = emptyList(),
                        isLoadingServiceToConfig = false,
                    )
                }
            }
        }
    }

    fun loadServicesToConfigFromZip(zip: File, deleteZipAfterAdding: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _servicesUiState.update {
                it.copy(isLoadingServiceToConfig = true)
            }

            val manifests = serviceInstaller.readManifests(zip)
                .map { it.copy(service = it.service.markAsLocal()) }
            if (manifests.isEmpty()) {
                _servicesUiState.update {
                    it.copy(
                        message = UiMessage.Error(strings(BaseR.string.invalid_service_zip)),
                        isLoadingServiceToConfig = false,
                    )
                }
                return@launch
            }
            val services = manifests.map { manifest ->
                val service = manifest.service
                AppendableService(
                    service = service.toUiManifest(fileReader, htmlParser),
                    isAdded = serviceRepository.findDbService(service.toStored().id) != null,
                    onServiceAdded = {
                        viewModelScope.launch(workerDispatcher) {
                            installZipService(
                                service = it.raw,
                                zip = zip,
                                deleteZipAfterAdding = deleteZipAfterAdding,
                            )
                        }
                    }
                )
            }
            _servicesUiState.update {
                it.copy(
                    servicesToConfigure = services,
                    isLoadingServiceToConfig = false,
                )
            }
        }
    }

    private fun addToDbServices(service: ServiceManifest) {
        viewModelScope.launch(workerDispatcher) {
            val addedService = service.toStored()
            serviceRepository.upsertDbService(addedService)
            loadDbServices()
            val services = (allAppendableServices ?: emptyList()).toMutableList()
            for (i in services.indices) {
                val item = services[i]
                if (item.service.id == addedService.id) {
                    services[i] = item.copy(isAdded = true)
                }
            }
            updateAppendableServices(services)
        }
    }

    fun removeSelectedDbServices() {
        viewModelScope.launch(Dispatchers.IO) {
            val services = _servicesUiState.value.selectedServices
            if (services.isEmpty()) return@launch
            val rawServices = services.map { it.raw }
            serviceRepository.removeDbServices(rawServices)
            // Remove code and resources
            rawServices.forEach { serviceInstaller.removeServiceFiles(it) }
            loadDbServices()
        }
    }

    fun clearMessage() {
        _servicesUiState.update {
            it.copy(message = null)
        }
    }

    fun unselectAllDbServices() {
        _servicesUiState.update {
            it.copy(selectedServices = emptySet())
        }
    }

    fun updateSearchQuery(query: TextFieldValue) {
        _addServiceUiState.update {
            it.copy(searchQuery = query)
        }
        val allServices = allAppendableServices
        if (allServices != null) {
            viewModelScope.launch(workerDispatcher) {
                val appendableServices = filterServicesByQuery(
                    services = allServices,
                    query = query.text
                )
                _addServiceUiState.update {
                    it.copy(appendableServices = appendableServices)
                }
            }
        }
    }

    fun loadAppendableServicesIfEmpty() {
        if (_addServiceUiState.value.appendableServices.isNotEmpty() ||
            _addServiceUiState.value.isLoadingAppendableServices
        ) {
            return
        }
        loadAppendableServices()
    }

    private fun loadAppendableServices() {
        viewModelScope.launch(workerDispatcher) {
            _addServiceUiState.update {
                it.copy(isLoadingAppendableServices = true)
            }
            val localServiceIds = serviceRepository.loadDbServices()
                .map { it.id }
                .toHashSet()
            // Load builtin services
            val builtinServices = serviceRepository.loadBuiltinServices()
                .sortedWith(Comparators.serviceManifestNameComparator)
                .map {
                    AppendableService(
                        service = it.toUiManifest(fileReader, htmlParser),
                        isAdded = localServiceIds.contains(it.id),
                        onServiceAdded = { service ->
                            viewModelScope.launch(workerDispatcher) {
                                installBuiltinService(service.raw)
                                loadDbServices()
                            }
                        },
                    )
                }
            updateAppendableServices(builtinServices)
            _addServiceUiState.update {
                it.copy(isLoadingAppendableServices = false)
            }
        }
    }

    private fun updateAppendableServices(
        services: List<AppendableService>,
    ) {
        allAppendableServices = services
        val searchQuery = _addServiceUiState.value.searchQuery.text
        val appendableServices = filterServicesByQuery(
            services = allAppendableServices!!,
            query = searchQuery
        )
        _addServiceUiState.update {
            it.copy(appendableServices = appendableServices)
        }
    }

    private fun filterServicesByQuery(
        services: List<AppendableService>,
        query: String
    ): List<AppendableService> {
        if (query.isEmpty() || services.isEmpty()) {
            return services
        }
        return services.filter {
            val service = it.service
            if (service.name.contains(query, ignoreCase = true)) {
                return@filter true
            }
            if (service.id.contains(query, ignoreCase = true)) {
                return@filter true
            }
            false
        }
    }

    fun resetBuiltinServicesUpdateState() {
        _servicesUiState.update {
            it.copy(
                isUpdatingBuiltinServices = false,
                updatedBuiltinServiceCount = -1,
            )
        }
    }

    fun ignoreBuiltinServicesUpdates() {
        viewModelScope.launch(workerDispatcher) {
            val versionCode = appVersionProvider.versionCode
            preferencesStore.versionCodeIgnoresBuiltinServiceUpdates.value = versionCode
        }
    }

    fun updateBuiltinServices() {
        _servicesUiState.update {
            it.copy(
                isUpdatingBuiltinServices = true,
                updatedBuiltinServiceCount = -1,
            )
        }
        viewModelScope.launch(workerDispatcher) {
            val updatedDbServices = builtinServiceUpdater.updateBuiltinServices()
            _servicesUiState.update {
                it.copy(
                    isUpdatingBuiltinServices = false,
                    updatedBuiltinServiceCount = updatedDbServices.size,
                )
            }
        }
    }

    fun resetUpdatableBuiltinServiceCount() {
        _servicesUiState.update { it.copy(updatableBuiltinServiceCount = 0) }
    }

    private suspend fun installZipService(
        service: ServiceManifest,
        zip: File,
        deleteZipAfterAdding: Boolean,
    ) = withContext(workerDispatcher) {
        val extractedResources = serviceInstaller.installFromZip(
            zip = zip,
            manifestName = service.name,
        )
        if (extractedResources != null) {
            addToDbServices(service.copy(localResources = extractedResources))
            if (deleteZipAfterAdding) {
                zip.delete()
            }
        } else {
            _servicesUiState.update {
                val msg = strings(BaseR.string.cannot_unpack_service)
                it.copy(message = UiMessage.Error(msg))
            }
        }
    }

    private suspend fun installBuiltinService(
        service: ServiceManifest
    ) = withContext(workerDispatcher) {
        val extractedResources = serviceInstaller.installFromAssets(service)
        if (extractedResources != null) {
            addToDbServices(service.copy(localResources = extractedResources))
        } else {
            _servicesUiState.update { uiState ->
                val msg = strings(BaseR.string.cannot_install_service)
                uiState.copy(message = UiMessage.Error(msg))
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ServiceMgtViewModel(
                serviceRepository = ServiceRepository.getDefault(context),
                serviceInstaller = ServiceInstaller.getDefault(context),
                builtinServiceUpdater = BuiltinServiceUpdater(
                    serviceInstaller = ServiceInstaller.getDefault(context),
                    builtinServicesLoader = AssetsServicesLoader(context),
                    serviceRepository = ServiceRepository.getDefault(context),
                ),
                strings = AndroidStrings(context),
                fileReader = AndroidFileReader(context),
                preferencesStore = context.preferencesStore(),
                appVersionProvider = DefaultAppVersionProvider(context),
            ) as T
        }
    }
}