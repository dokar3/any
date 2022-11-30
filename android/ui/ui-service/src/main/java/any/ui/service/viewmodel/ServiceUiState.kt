package any.ui.service.viewmodel

import androidx.compose.runtime.Immutable
import any.base.result.ValidationResult
import any.data.entity.ServiceManifest

@Immutable
data class ServiceUiState(
    val isValidating: Boolean = false,
    val validations: Map<String, ValidationResult> = emptyMap(),
    val areAllValidationsPassed: Boolean = false,
    val serviceToSave: ServiceManifest? = null,
    val savedService: ServiceManifest? = null,
    val upgradeInfo: UpgradeInfo? = null,
)

@Immutable
data class UpgradeInfo(
    val fromVersion: String,
    val toVersion: String,
)
