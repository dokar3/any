package any.ui.common.post

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import android.content.res.Resources
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import any.data.entity.Post
import any.domain.entity.UiPost

internal object PostItemDefaults {
    val PrimaryTextLineHeight = 1.6.em

    const val TextMaxLines = 8

    const val IconButtonsOpacity = 0.7f

    val postInfoLineContent by lazy {
        val iconPlaceholder = Placeholder(
            width = 20.sp,
            height = 17.sp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
        )
        mapOf(
            Post.Reference.Type.Repost.name to InlineTextContent(
                placeholder = iconPlaceholder,
                children = { PostInfoIcon(type = Post.Reference.Type.Repost) },
            ),
            Post.Reference.Type.Quote.name to InlineTextContent(
                placeholder = iconPlaceholder,
                children = { PostInfoIcon(type = Post.Reference.Type.Quote) },
            ),
            Post.Reference.Type.Reply.name to InlineTextContent(
                placeholder = iconPlaceholder,
                children = { PostInfoIcon(type = Post.Reference.Type.Reply) },
            ),
        )
    }

    fun buildPostInfo(post: UiPost, resources: Resources): AnnotatedString {
        return buildAnnotatedString {
            val ref = post.reference
            if (ref != null) {
                appendInlineContent(ref.type.name)
                val text = when (ref.type) {
                    Post.Reference.Type.Repost -> resources.getString(BaseR.string.repost)
                    Post.Reference.Type.Quote -> resources.getString(BaseR.string.quote)
                    Post.Reference.Type.Reply -> resources.getString(BaseR.string.reply)
                }
                append(text)
            }

            val category = post.category
            if (!category.isNullOrEmpty()) {
                if (length != 0) {
                    append(" | ")
                }
                append(category)
            }

            val date = post.date
            if (!date.isNullOrEmpty()) {
                if (length != 0) {
                    append(" | ")
                }
                append(date)
            }
        }
    }

    @Composable
    fun PostInfoIcon(
        type: Post.Reference.Type,
        modifier: Modifier = Modifier,
        alpha: Float = 0.6f,
    ) {
        val iconRes = when (type) {
            Post.Reference.Type.Repost -> CommonUiR.drawable.ic_post_repost
            Post.Reference.Type.Quote -> CommonUiR.drawable.ic_post_quote
            Post.Reference.Type.Reply -> CommonUiR.drawable.ic_post_reply
        }
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = modifier.alpha(alpha),
        )
    }
}