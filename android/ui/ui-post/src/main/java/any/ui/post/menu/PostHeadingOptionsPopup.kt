package any.ui.post.menu

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import any.ui.common.widget.AnimatedPopup
import any.ui.common.widget.AnimatedPopupItem
import any.ui.common.widget.rememberAnimatedPopupDismissRequester

@Composable
fun PostHeadingOptionsPopup(
    onDismissed: () -> Unit,
    onAddToBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissRequester = rememberAnimatedPopupDismissRequester()
    AnimatedPopup(
        dismissRequester = dismissRequester,
        onDismissed = onDismissed,
        modifier = modifier,
        scaleAnimOrigin = TransformOrigin(1f, 0f),
        properties = PopupProperties(focusable = true),
    ) {
        AnimatedPopupItem(
            index = 0,
            onClick = {
                dismissRequester.dismiss()
                onAddToBookmarkClick()
            },
            contentPadding = PaddingValues(
                start = 0.dp,
                top = 10.dp,
                end = 16.dp,
                bottom = 10.dp
            ),
        ) {
            PostPopupMenuItem(item = addBookmarkItem)
        }
    }
}