package any.ui.readingbubble

import any.ui.readingbubble.entity.ReadingPost

data class ReadingBubbleUiState(
    val posts: List<ReadingPost> = emptyList(),
)