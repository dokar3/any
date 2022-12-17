package any.ui.comments.test

import any.data.entity.Comment
import any.ui.comments.UiComment
import any.ui.comments.collapseReplies
import any.ui.comments.getReplies
import any.ui.comments.toUiComments
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class UiCommentsTest {
    @Test
    fun testConvertCommentsToUiComments() {
        val comments = listOf(
            Comment(
                replies = listOf(
                    Comment(),
                    Comment(
                        replies = listOf(
                            Comment(),
                        )
                    ),
                )
            ),
            Comment(),
            Comment(
                replies = listOf(
                    Comment(),
                )
            ),
        )

        val id = UUID.randomUUID()
        val uiComments = comments.toUiComments(idGenerator = { id })
        assertEquals(
            listOf(
                UiComment.Comment(id),
                UiComment.Reply(id = id, depth = 0),
                UiComment.Reply(id = id, depth = 0),
                UiComment.Reply(id = id, depth = 1),
                UiComment.Comment(id),
                UiComment.Comment(id),
                UiComment.Reply(id = id, depth = 0),
            ),
            uiComments,
        )
    }

    @Test
    fun testGetReplies() {
        val uiComments = listOf(
            UiComment.Comment(),
            UiComment.Reply(),
            UiComment.Reply(),
            UiComment.Reply(),
            UiComment.Reply(),
            UiComment.Comment(),
            UiComment.Comment(),
            UiComment.Reply(),
            UiComment.Reply(),
        )

        assertEquals(
            uiComments.subList(1, 5),
            uiComments.getReplies(uiComments[0] as UiComment.Comment)
        )

        assertEquals(
            emptyList<UiComment>(),
            uiComments.getReplies(uiComments[5] as UiComment.Comment),
        )

        assertEquals(
            uiComments.subList(7, uiComments.size),
            uiComments.getReplies(uiComments[6] as UiComment.Comment)
        )
    }

    @Test
    fun testCollapseReplies() {
        val id = UUID.randomUUID()

        val uiComments = listOf(
            UiComment.Comment(id),
            UiComment.Reply(id),
            UiComment.Reply(id),
            UiComment.Reply(id),
            UiComment.Reply(id),
            UiComment.Comment(id),
            UiComment.Comment(id),
            UiComment.Reply(id), // 0
            UiComment.Reply(id), // 1
            UiComment.Reply(id), // 2
            UiComment.Comment(id), // 3
            UiComment.Reply(id), // 4
            UiComment.Reply(id), // 5
            UiComment.Reply(id), // 6
            UiComment.Reply(id), // 7
        )

        val allCollapsed = uiComments.collapseReplies(expandCount = 0)
        assertEquals(
            listOf(
                UiComment.Comment(id),
                UiComment.ExpandReplies(commentId = id, count = 4),
                UiComment.Comment(id),
                UiComment.Comment(id),
                UiComment.ExpandReplies(commentId = id, count = 3),
                UiComment.Comment(id),
                UiComment.ExpandReplies(commentId = id, count = 4),
            ),
            allCollapsed,
        )

        val partialCollapsed = uiComments.collapseReplies(expandCount = 3)
        assertEquals(
            listOf(
                UiComment.Comment(id),
                UiComment.Reply(id),
                UiComment.Reply(id),
                UiComment.Reply(id),
                UiComment.ExpandReplies(commentId = id, count = 1),
                UiComment.Comment(id),
                UiComment.Comment(id),
                UiComment.Reply(id),
                UiComment.Reply(id),
                UiComment.Reply(id),
                UiComment.Comment(id),
                UiComment.Reply(id),
                UiComment.Reply(id),
                UiComment.Reply(id),
                UiComment.ExpandReplies(commentId = id, count = 1),
            ),
            partialCollapsed,
        )

        val noCollapsed = uiComments.collapseReplies(expandCount = Int.MAX_VALUE)
        assertEquals(uiComments, noCollapsed)
    }
}