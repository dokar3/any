package any.ui.settings

import androidx.compose.runtime.Stable

@Stable
data class SettingsUiState(
    val title: String = "",
    val showBackButton: Boolean = false
)