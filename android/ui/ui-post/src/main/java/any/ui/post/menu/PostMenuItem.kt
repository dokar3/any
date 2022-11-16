package any.ui.post.menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
internal class PostMenuItem(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    isSelected: Boolean = false
) {
    var isSelected by mutableStateOf(isSelected)
}