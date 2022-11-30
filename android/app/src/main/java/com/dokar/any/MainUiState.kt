package com.dokar.any

import androidx.compose.runtime.Immutable
import any.ui.readingbubble.entity.ReadingPost

@Immutable
data class MainUiState(
    val serviceManifestUrlToAdd: String? = null,
    val readingPostToNavigate: ReadingPost? = null,
    val shortcutsDestination: ShortcutsDestination? = null,
    val serviceIdToConfigure: String? = null,
)