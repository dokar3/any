package any.ui.common.post

import androidx.compose.ui.unit.em
import any.domain.entity.UiPost

internal object PostItemDefaults {
    val PrimaryTextLineHeight = 1.6.em

    const val TextMaxLines = 8

    const val IconButtonsOpacity = 0.7f

    fun buildPostInfo(post: UiPost): String {
        return buildString {
            val category = post.category
            if (!category.isNullOrEmpty()) {
                if (isNotEmpty()) {
                    append(" | ")
                }
                append(category)
            }

            val date = post.date
            if (!date.isNullOrEmpty()) {
                if (isNotEmpty()) {
                    append(" | ")
                }
                append(date)
            }
        }
    }
}