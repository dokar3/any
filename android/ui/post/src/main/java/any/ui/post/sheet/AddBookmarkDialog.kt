package any.ui.post.sheet

import any.base.R as BaseR
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import any.data.entity.Bookmark
import any.domain.entity.UiPost
import any.ui.common.widget.BasicDialog
import any.ui.common.widget.WarningMessage

@Composable
internal fun AddBookmarkDialog(
    onDismissRequest: () -> Unit,
    onAskToCollect: () -> Unit,
    post: UiPost,
    elementIndex: Int,
    currentBookmarks: List<Bookmark>,
    onLoadRequest: () -> Unit,
    onAddBookmark: (Bookmark) -> Unit,
    onRemoveBookmark: (Bookmark) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (elementIndex == -1) {
        onDismissRequest()
        return
    }

    val postUrl = post.url
    val serviceId = post.serviceId

    var bookmark: Bookmark? by remember(postUrl, elementIndex) {
        mutableStateOf(null)
    }

    val res = LocalContext.current.resources
    var bookmarkTitle by remember(elementIndex, bookmark) {
        mutableStateOf(
            bookmark?.title
                ?: (res.getString(BaseR.string.bookmark) + " ${currentBookmarks.size + 1}")
        )
    }

    LaunchedEffect(currentBookmarks, postUrl, elementIndex) {
        bookmark = currentBookmarks.find {
            it.serviceId == serviceId &&
                    it.postUrl == postUrl &&
                    it.elementIndex == elementIndex
        }
    }

    BasicDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = {
            Text(
                if (bookmark == null) {
                    stringResource(BaseR.string.add_bookmark)
                } else {
                    stringResource(BaseR.string.edit_bookmark)
                }
            )
        },
        confirmText = {
            Text(
                if (bookmark == null) {
                    stringResource(BaseR.string.add)
                } else {
                    stringResource(BaseR.string.update)
                }
            )
        },
        onConfirmClick = {
            val newBookmark = Bookmark(
                serviceId = serviceId,
                postUrl = postUrl,
                elementIndex = elementIndex,
                title = bookmarkTitle,
                createdAt = bookmark?.createdAt ?: System.currentTimeMillis()
            )
            onAddBookmark(newBookmark)
        },
        neutralText = if (bookmark != null) {
            {
                Text(
                    text = stringResource(BaseR.string.remove),
                    color = MaterialTheme.colors.error,
                )
            }
        } else {
            null
        },
        onNeutralClick = {
            onRemoveBookmark(bookmark!!)
        }
    ) {
        LaunchedEffect(serviceId, postUrl) {
            onLoadRequest()
        }

        Column {
            if (!post.isCollected()) {
                UncollectedWarning(onAskToCollect = onAskToCollect)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = bookmarkTitle,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = { bookmarkTitle = it },
                label = { Text(stringResource(BaseR.string.description)) },
                singleLine = true
            )
        }
    }
}

@Composable
private fun UncollectedWarning(
    onAskToCollect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WarningMessage(modifier = modifier) {
        Text(stringResource(BaseR.string.bookmark_uncollected_post_alert))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            TextButton(onClick = onAskToCollect) {
                Text(text = stringResource(BaseR.string.collect))
            }
        }
    }
}