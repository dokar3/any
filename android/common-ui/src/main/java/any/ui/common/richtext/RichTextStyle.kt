package any.ui.common.richtext

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import any.ui.common.theme.link

@Immutable
data class RichTextStyle(
    val linkColor: Color,
    val inlineCodeBackground: Color,
) {
    companion object {
        val Default: RichTextStyle
            @Composable
            get() {
                return RichTextStyle(
                    linkColor = MaterialTheme.colors.link,
                    inlineCodeBackground = MaterialTheme.colors.onBackground.copy(alpha = 0.07f),
                )
            }
    }
}