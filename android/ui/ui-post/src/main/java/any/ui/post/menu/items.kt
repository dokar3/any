package any.ui.post.menu

import any.base.R as BaseR
import any.ui.common.R

internal val CopyUrlItem = PostMenuItem(
    icon = R.drawable.ic_baseline_link_24,
    title = BaseR.string.copy_url,
)

internal val OpenInBrowserItem = PostMenuItem(
    icon = R.drawable.ic_baseline_open_in_browser_24,
    title = BaseR.string.open_in_browser,
)

internal val ReversePagesItem = PostMenuItem(
    icon = R.drawable.ic_ascending,
    title = BaseR.string.reverse,
)

internal val FloatingPostItem = PostMenuItem(
    icon = R.drawable.ic_baseline_layers_24,
    title = any.base.R.string.floating,
)

internal val CancelItem = PostMenuItem(
    icon = R.drawable.ic_baseline_close_24,
    title = android.R.string.cancel,
)

internal val BookmarksItem = PostMenuItem(
    icon = R.drawable.ic_baseline_bookmark_border_24,
    title = BaseR.string.bookmarks,
)

internal val SectionsItem = PostMenuItem(
    icon = R.drawable.ic_baseline_format_list_numbered_24,
    title = BaseR.string.sections,
)

internal val CommentsItem = PostMenuItem(
    icon = R.drawable.ic_outline_comment_24,
    title = BaseR.string.comments,
)

internal val ShareItem = PostMenuItem(
    icon = R.drawable.ic_baseline_share_24,
    title = BaseR.string.share,
)

internal val GoToTopItem = PostMenuItem(
    icon = R.drawable.ic_baseline_arrow_upward_24,
    title = BaseR.string.go_to_top,
)

internal val JumpToPageItem = PostMenuItem(
    icon = R.drawable.ic_baseline_jump_24,
    title = BaseR.string.jump_to_page,
)

internal val ThemeItem = PostMenuItem(
    icon = R.drawable.ic_outline_wb_sunny_24,
    title = BaseR.string.theme,
)

internal val TextStyleItem = PostMenuItem(
    icon = R.drawable.ic_baseline_text_format_24,
    title = BaseR.string.text_style,
)
