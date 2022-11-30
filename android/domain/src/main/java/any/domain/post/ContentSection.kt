package any.domain.post

import androidx.compose.runtime.Immutable

@Immutable
data class ContentSection(
    val name: String,
    val targetElementIndex: Int,
    val depth: Int = 0,
    val isStart: Boolean = false,
    val isEnd: Boolean = false,
) {
    companion object {
        fun start() = ContentSection(name = "", targetElementIndex = 0, isStart = true)

        fun end(index: Int) = ContentSection(name = "", targetElementIndex = index, isEnd = true)
    }
}