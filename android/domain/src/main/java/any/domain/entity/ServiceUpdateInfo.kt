package any.domain.entity

import androidx.compose.runtime.Immutable

@Immutable
data class ServiceUpdateInfo(
    val fromVersion: String,
    val toVersion: String,
)