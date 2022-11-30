package any.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

object HomeScreenDefaults {
    val HorizontalPadding = 16.dp

    val ListPadding = PaddingValues(
        horizontal = HorizontalPadding,
        vertical = HorizontalPadding / 2
    )

    val ListItemPadding = PaddingValues(
        horizontal = HorizontalPadding,
        vertical = HorizontalPadding / 2
    )

    val GridItemSpacing = HorizontalPadding
}