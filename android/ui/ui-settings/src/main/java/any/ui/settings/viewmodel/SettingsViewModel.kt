package any.ui.settings.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel : ViewModel() {
    private val _settingsUiState = MutableStateFlow(SettingsUiState())
    val settingsUiState: StateFlow<SettingsUiState> = _settingsUiState

    fun updateTitle(title: String) {
        _settingsUiState.update {
            it.copy(title = title)
        }
    }

    fun setShowBackArrow(show: Boolean) {
        _settingsUiState.update {
            it.copy(showBackButton = show)
        }
    }
}