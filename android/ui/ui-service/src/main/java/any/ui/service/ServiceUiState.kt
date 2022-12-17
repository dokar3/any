package any.ui.service

import androidx.compose.runtime.Immutable
import any.base.result.ValidationResult
import any.data.entity.ServiceManifest
import any.domain.entity.ServiceUpdateInfo

@Immutable
data class ServiceUiState(
    val isValidating: Boolean = false,
    val validations: Map<String, ValidationResult> = emptyMap(),
    val areAllValidationsPassed: Boolean = false,
    val serviceToSave: ServiceManifest? = null,
    val savedService: ServiceManifest? = null,
    val updateInfo: ServiceUpdateInfo? = null,
)
