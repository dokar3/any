package any.ui.common.modifier

import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.fabOffset(
    spacingToEdge: Dp = 24.dp,
): Modifier {
    return offset(-spacingToEdge, -spacingToEdge)
}