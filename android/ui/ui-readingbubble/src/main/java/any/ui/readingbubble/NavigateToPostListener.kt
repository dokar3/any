package any.ui.readingbubble

import any.ui.readingbubble.entity.ReadingPost

fun interface NavigateToPostListener {
    fun onNavigate(post: ReadingPost)
}