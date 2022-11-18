package any.ui.service.viewmodel

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import any.base.result.ValidationResult
import any.domain.entity.UiServiceManifest

@Stable
data class ServiceUiState(
    val isValidating: Boolean = false,
    val validations: Map<String, ValidationResult> = emptyMap(),
    val areAllValidationsPassed: Boolean = false,
    val updatedService: UiServiceManifest? = null,
    val serviceToSave: UiServiceManifest? = null,
    val upgradeInfo: UpgradeInfo? = null,
)

@Immutable
data class UpgradeInfo(
    val fromVersion: String,
    val toVersion: String,
)
