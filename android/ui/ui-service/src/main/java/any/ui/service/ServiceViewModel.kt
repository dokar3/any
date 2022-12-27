package any.ui.service

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import any.base.AndroidStrings
import any.base.Strings
import any.base.result.ValidationResult
import any.data.entity.ServiceConfig
import any.data.entity.ServiceManifest
import any.data.entity.updateValuesFrom
import any.data.entity.value
import any.data.js.ServiceRunner
import any.data.js.validator.BasicServiceConfigsValidator
import any.data.js.validator.JsServiceConfigsValidator
import any.data.repository.ServiceRepository
import any.domain.entity.ServiceUpdateInfo
import any.domain.entity.UiServiceManifest
import com.vdurmont.semver4j.Semver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ServiceViewModel(
    private val serviceRepository: ServiceRepository,
    private val appRunner: ServiceRunner,
    private val strings: Strings,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private var checkUpgradeJob: Job? = null

    private val _serviceUiState = MutableStateFlow(ServiceUiState())
    val serviceUiState = _serviceUiState

    private val basicConfigsValidator by lazy {
        BasicServiceConfigsValidator(strings = strings)
    }

    fun checkUpgradeInfo(service: UiServiceManifest) {
        checkUpgradeJob?.cancel()
        checkUpgradeJob = viewModelScope.launch(workerDispatcher) {
            val local = serviceRepository.findDbService(service.id)
            if (local == null) {
                _serviceUiState.update {
                    it.copy(updateInfo = null)
                }
                return@launch
            }
            val isUpgrading = Semver(local.version) != Semver(service.version) ||
                    local.main != service.main ||
                    local.mainChecksums != service.mainChecksums ||
                    local.configs != service.configs?.updateValuesFrom(local.configs) ||
                    local.source != service.source
            val updateInfo = if (isUpgrading) {
                ServiceUpdateInfo(
                    fromVersion = local.version,
                    toVersion = service.version,
                )
            } else {
                null
            }
            _serviceUiState.update {
                it.copy(updateInfo = updateInfo)
            }
        }
    }

    fun tryValidateConfigsAndSave(
        service: UiServiceManifest,
        values: Map<String, Any?>,
        runJsValidator: Boolean,
    ) = viewModelScope.launch(workerDispatcher) {
        val serviceConfigs = service.configs
        if (serviceConfigs.isNullOrEmpty()) {
            // Nothing to validate
            _serviceUiState.update {
                it.copy(
                    areAllValidationsPassed = true,
                    serviceToSave = service.raw,
                )
            }
            return@launch
        }

        _serviceUiState.update { it.copy(isValidating = true) }

        val configs = serviceConfigs.map { it.copy(value = values[it.key] ?: it.value) }

        // Basic validations
        val basicResults = basicConfigsValidator.validate(configs)

        val jsValidator =
            JsServiceConfigsValidator(serviceRunner = appRunner, service = service.raw)

        val jsValidatorResults = if (runJsValidator &&
            basicResults.all { it is ValidationResult.Pass }
        ) {
            // Run js service config validations
            jsValidator.validate(configs)
        } else {
            MutableList(basicResults.size) { ValidationResult.Pass }
        }

        // Merge validations
        val results = MutableList(basicResults.size) {
            when {
                basicResults[it] is ValidationResult.Fail -> {
                    basicResults[it]
                }

                jsValidatorResults[it] is ValidationResult.Fail -> {
                    jsValidatorResults[it]
                }

                else -> {
                    ValidationResult.Pass
                }
            }
        }

        val validations = mutableMapOf<String, ValidationResult>()
        for (i in configs.indices) {
            validations[configs[i].key] = results[i]
        }

        val areAllValidationsPassed = results.all { it is ValidationResult.Pass }
        val updatedService = if (areAllValidationsPassed) {
            val updatedService = jsValidator.getUpdatedService()
            val updatedConfigs = updatedService.configs?.map {
                it.copy(value = values[it.key] ?: it.value)
            }
            updatedService.copy(configs = updatedConfigs)
        } else {
            service.raw
        }

        val toSave = if (areAllValidationsPassed) {
            updatedService.toStored()
                .copy(updatedAt = System.currentTimeMillis())
                .updateFromDb()
        } else {
            null
        }

        _serviceUiState.update {
            it.copy(
                isValidating = false,
                areAllValidationsPassed = areAllValidationsPassed,
                validations = validations,
                serviceToSave = toSave,
            )
        }
    }

    fun saveService(service: ServiceManifest) = viewModelScope.launch(workerDispatcher) {
        serviceRepository.upsertDbService(service)
        _serviceUiState.update {
            it.copy(
                serviceToSave = null,
                savedService = service,
            )
        }
    }

    fun clearValidationResult(config: ServiceConfig) = viewModelScope.launch(workerDispatcher) {
        val validations = _serviceUiState.value.validations
        if (!validations.containsKey(config.key)) {
            return@launch
        }
        val updated = validations.toMutableMap()
        updated.remove(config.key)
        _serviceUiState.update {
            it.copy(validations = updated.toMap())
        }
    }

    fun resetUiState() {
        _serviceUiState.update { ServiceUiState() }
    }

    private suspend fun ServiceManifest.updateFromDb(): ServiceManifest {
        val dbService = serviceRepository.findDbService(id) ?: return this
        return copy(
            isEnabled = dbService.isEnabled,
            pageKeyOfPage2 = dbService.pageKeyOfPage2,
        )
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ServiceViewModel(
                serviceRepository = ServiceRepository.getDefault(context),
                appRunner = ServiceRunner.getDefault(context),
                strings = AndroidStrings(context),
            ) as T
        }
    }
}
