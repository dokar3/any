package any.base.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

fun PaddingValues.copy(horizontal: Dp): PaddingValues {
    return PaddingValues(
        start = horizontal,
        top = calculateTopPadding(),
        end = horizontal,
        bottom = calculateBottomPadding(),
    )
}

fun PaddingValues.copy(
    layoutDirection: LayoutDirection,
    start: Dp = calculateStartPadding(layoutDirection),
    top: Dp = calculateTopPadding(),
    end: Dp = calculateEndPadding(layoutDirection),
    bottom: Dp = calculateBottomPadding(),
): PaddingValues {
    return PaddingValues(start, top, end, bottom)
}
