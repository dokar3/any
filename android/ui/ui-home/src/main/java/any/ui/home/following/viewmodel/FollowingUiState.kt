package any.ui.home.following.viewmodel

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue
import any.base.model.UiMessage
import any.domain.entity.UiUser

@Immutable
data class FollowingUiState(
    val services: List<ServiceOfFollowingUsers> = emptyList(),
    val users: List<UiUser> = emptyList(),
    val selection: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val showEmpty: Boolean = false,
    val message: UiMessage? = null,
    val filterText: TextFieldValue = TextFieldValue(),
) {
    val isSelectionEnabled = selection.isNotEmpty()
}