package any.ui.settings.menu

import androidx.annotation.DrawableRes

internal data class ActionMenuItem(
    val title: String,
    @DrawableRes
    val iconRes: Int,
    val onClick: (() -> Unit)? = null,
)
