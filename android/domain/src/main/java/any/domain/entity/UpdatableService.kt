package any.domain.entity

import androidx.compose.runtime.Immutable
import any.data.entity.ServiceManifest

@Immutable
data class UpdatableService(
    val value: ServiceManifest,
    val upgradeInfo: ServiceUpdateInfo,
    val isUpdating: Boolean = false,
    val isUpdated: Boolean = false,
)