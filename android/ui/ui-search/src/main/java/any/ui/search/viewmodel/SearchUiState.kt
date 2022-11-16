package any.ui.search.viewmodel

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue
import any.base.UiMessage
import any.data.entity.JsPageKey
import any.domain.entity.UiPost
import any.domain.entity.UiServiceManifest

@Stable
data class SearchUiState(
    val services: List<UiServiceManifest> = emptyList(),
    val currentService: UiServiceManifest? = null,
    val query: TextFieldValue = TextFieldValue(),
    val hasSetQuery: Boolean = false,
    val pageKey: JsPageKey? = null,
    val posts: List<UiPost> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSuccess: Boolean = true,
    val message: UiMessage? = null,
    val searchedCount: Int = 0,
    val hasMore: Boolean = true,
    val isSearchable: Boolean = true,
)