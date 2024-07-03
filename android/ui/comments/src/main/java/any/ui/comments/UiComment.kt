package any.ui.comments

import androidx.compose.runtime.Immutable
import any.data.entity.Comment
import any.data.entity.Post
import java.util.UUID

@Immutable
sealed class UiComment {
    @Immutable
    data class Comment(
        val id: UUID = UUID.randomUUID(),
        val username: String = "",
        val content: String = "",
        val avatar: String? = null,
        val media: List<Post.Media>? = null,
        val date: Long = 0,
        val upvotes: Int = 0,
        val downvote: Int = 0,
    ) : UiComment()

    @Immutable
    data class Reply(
        val id: UUID = UUID.randomUUID(),
        val depth: Int = 1,
        val username: String = "",
        val content: String = "",
        val avatar: String? = null,
        val media: List<Post.Media>? = null,
        val date: Long = 0,
        val upvotes: Int = 0,
        val downvote: Int = 0,
    ) : UiComment()

    @Immutable
    data class ExpandReplies(
        val commentId: UUID,
        val count: Int,
    ) : UiComment()

    @Immutable
    data class CollapseReplies(
        val commentId: UUID,
        val count: Int,
    ) : UiComment()
}

internal fun List<Comment>.toUiComments(
    idGenerator: () -> UUID = { UUID.randomUUID() },
): List<UiComment> {
    return toUiComments(level = 0, idGenerator = idGenerator)
}

private fun List<Comment>.toUiComments(
    level: Int,
    idGenerator: () -> UUID = { UUID.randomUUID() },
): List<UiComment> {
    val list = mutableListOf<UiComment>()
    for (comment in this) {
        val uiComment = with(comment) {
            if (level == 0) {
                UiComment.Comment(
                    id = idGenerator(),
                    username = username,
                    avatar = avatar,
                    content = content,
                    media = media,
                    date = date ?: 0L,
                    upvotes = upvotes ?: 0,
                    downvote = downvote ?: 0,
                )
            } else {
                UiComment.Reply(
                    id = idGenerator(),
                    depth = level - 1,
                    username = username,
                    avatar = avatar,
                    content = content,
                    media = media,
                    date = date ?: 0L,
                    upvotes = upvotes ?: 0,
                    downvote = downvote ?: 0,
                )
            }
        }
        list.add(uiComment)
        val replies = comment.replies
        if (!replies.isNullOrEmpty()) {
            list.addAll(
                replies.toUiComments(
                    level = level + 1,
                    idGenerator = idGenerator,
                )
            )
        }
    }
    return list
}

internal fun List<UiComment>.getReplies(
    target: UiComment.Comment,
): List<UiComment.Reply> {
    val replies = mutableListOf<UiComment.Reply>()
    var foundTarget = false
    for (comment in this) {
        if (foundTarget) {
            if (comment is UiComment.Reply) {
                replies.add(comment)
            } else {
                break
            }
        } else {
            if (comment == target) {
                foundTarget = true
            }
        }
    }
    return replies
}

/**
 * Collapse replies in a comment list
 */
internal fun List<UiComment>.collapseReplies(
    expandCount: Int,
): List<UiComment> {
    require(expandCount >= 0) { "expandedReplyCount cannot be negative" }

    val collapsed = mutableListOf<CollapsedInfo>()
    var prevComment: UiComment.Comment? = null
    var replyStartAt = -1
    var replyCount = 0
    for (i in this.indices) {
        val item = this[i]
        val isTheLast = i == this.lastIndex
        val isReply = item is UiComment.Reply
        if (isReply) {
            replyCount++
        }
        if (isTheLast || !isReply) {
            if (replyStartAt != -1 && replyCount > expandCount) {
                val first = replyStartAt + expandCount
                val last = if (isTheLast) i else i - 1
                val info = CollapsedInfo(
                    first = first,
                    last = last,
                    parentComment = prevComment,
                    count = last - first + 1,
                )
                collapsed.add(info)
            }
            replyStartAt = -1
        } else if (replyStartAt == -1) {
            replyStartAt = i
        }
        if (!isReply) {
            replyCount = 0
        }
        if (item is UiComment.Comment) {
            prevComment = item
        }
    }

    val collapsedComments = mutableListOf<UiComment>()
    var idx = 0
    for (info in collapsed) {
        collapsedComments.addAll(subList(idx, info.first))
        val parentComment = info.parentComment
        if (parentComment != null) {
            // Add expand item
            collapsedComments.add(
                UiComment.ExpandReplies(
                    commentId = parentComment.id,
                    count = info.count,
                )
            )
        }
        idx = info.last + 1
    }
    if (idx < this.lastIndex) {
        collapsedComments.addAll(subList(idx, size))
    }

    return collapsedComments
}

private data class CollapsedInfo(
    val first: Int,
    val last: Int,
    val parentComment: UiComment.Comment?,
    val count: Int,
)
