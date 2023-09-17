package any.ui.post

import android.R
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.compose.ImmutableHolder
import any.data.entity.Bookmark
import any.ui.common.dialog.SimpleDialog
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.widget.EmojiEmptyContent
import any.base.R as BaseR

@Composable
internal fun Bookmarks(
    bookmarks: ImmutableHolder<List<Bookmark>>,
    onBookmarkClick: (Bookmark) -> Unit,
    onRemoveBookmark: (Bookmark) -> Unit,
    modifier: Modifier = Modifier,
    stackListFromBottom: Boolean = false,
) {
    var selectedBookmark: Bookmark? by remember { mutableStateOf(null) }

    val listHolder = remember(bookmarks, stackListFromBottom) {
        if (stackListFromBottom) ImmutableHolder(bookmarks.value.reversed()) else bookmarks
    }

    Box(modifier = modifier) {
        if (listHolder.value.isNotEmpty()) {
            BookmarkList(
                bookmarks = listHolder,
                onBookmarkClick = onBookmarkClick,
                onBookmarkLongClick = { selectedBookmark = it },
                reverseLayout = stackListFromBottom,
            )
        } else {
            EmojiEmptyContent(modifier = Modifier.fillMaxSize()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(BaseR.string.no_bookmarks),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(BaseR.string.add_bookmark_hint),
                        fontSize = 16.sp,
                        color = MaterialTheme.colors.onBackground.copy(
                            alpha = ContentAlpha.medium,
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    if (selectedBookmark != null) {
        val bookmark = selectedBookmark!!
        SimpleDialog(
            onDismissRequest = {
                selectedBookmark = null
            },
            title = {
                Text(stringResource(BaseR.string.remove_bookmark))
            },
            text = {
                Text(
                    stringResource(BaseR.string.remove_bookmark_alert)
                )
            },
            confirmText = {
                Text(
                    text = stringResource(BaseR.string.remove),
                    color = MaterialTheme.colors.error,
                )
            },
            cancelText = {
                Text(stringResource(R.string.cancel))
            },
            onConfirmClick = {
                onRemoveBookmark(bookmark)
            }
        )
    }
}

@Composable
private fun BookmarkList(
    bookmarks: ImmutableHolder<List<Bookmark>>,
    onBookmarkClick: (Bookmark) -> Unit,
    onBookmarkLongClick: (Bookmark) -> Unit,
    reverseLayout: Boolean = false,
) {
    val state = rememberLazyListState()
    LazyColumn(
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScrollBar(state),
        reverseLayout = reverseLayout,
    ) {
        items(bookmarks.value) { bookmark ->
            BookmarkItem(
                title = bookmark.title,
                index = bookmark.elementIndex,
                onClick = {
                    onBookmarkClick(bookmark)
                },
                onLongClick = {
                    onBookmarkLongClick(bookmark)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkItem(
    title: String,
    index: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onClick,
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .padding(start = 12.dp, end = 12.dp, top = 12.dp)
            .padding(bottom = 12.dp)
    ) {
        Text(title)

        Text(
            stringResource(BaseR.string._position, index + 1),
            fontSize = 14.sp,
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
        )
    }
}