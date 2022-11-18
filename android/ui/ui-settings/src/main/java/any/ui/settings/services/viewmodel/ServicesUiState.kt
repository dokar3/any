package any.ui.settings.services.viewmodel

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue
import any.base.UiMessage
import any.domain.entity.UiServiceManifest

@Stable
data class ServicesUiState(
    val services: List<UiServiceManifest> = emptyList(),
    val selectedServices: Set<UiServiceManifest> = emptySet(),
    val servicesToConfigure: List<AppendableService> = emptyList(),
    val isLoadingServiceToConfig: Boolean = false,
    val isUpdatingBuiltinServices: Boolean = false,
    val updatableBuiltinServiceCount: Int = 0,
    val updatedBuiltinServiceCount: Int = -1,
    val message: UiMessage? = null,
) {
    val selectedServiceCount = selectedServices.size

    val isInSelection: Boolean = selectedServices.isNotEmpty()
}

@Stable
data class AddServiceUiState(
    val appendableServices: List<AppendableService> = emptyList(),
    val searchQuery: TextFieldValue = TextFieldValue(),
    val isLoadingAppendableServices: Boolean = false,
)

@Stable
data class AppendableService(
    val service: UiServiceManifest,
    val isAdded: Boolean,
    private val saveService: (toAdd: UiServiceManifest) -> Unit,
) {
    fun saveService(toAdd: UiServiceManifest) {
        saveService.invoke(toAdd)
    }
}
