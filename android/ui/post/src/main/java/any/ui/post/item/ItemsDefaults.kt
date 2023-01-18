package any.ui.post.item

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

internal object ItemsDefaults {
    val ItemVerticalSpacing = 8.dp
    val ItemHorizontalSpacing = 16.dp

    val ContentPadding = PaddingValues(
        horizontal = ItemHorizontalSpacing,
        vertical = ItemVerticalSpacing,
    )
}