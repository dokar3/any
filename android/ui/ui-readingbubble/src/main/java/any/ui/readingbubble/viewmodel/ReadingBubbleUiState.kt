package any.ui.readingbubble.viewmodel

import any.ui.readingbubble.entity.ReadingPost

data class ReadingBubbleUiState(
    val posts: List<ReadingPost> = emptyList(),
)