package any.ui.post.item

import androidx.compose.foundation.layout.padding
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import any.ui.common.LocalFontScale

@Composable
internal fun TextElementItem(
    text: String?,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text ?: "",
        modifier = modifier
            .padding(
                horizontal = ItemsDefaults.ItemHorizontalSpacing,
                vertical = ItemsDefaults.ItemVerticalSpacing
            ),
        fontSize = LocalTextStyle.current.fontSize * LocalFontScale.current,
    )
}