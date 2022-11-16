package any.ui.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset

fun Offset.toDpOffset(density: Density): DpOffset {
    return with(density) { DpOffset(x.toDp(), y.toDp()) }
}

fun IntOffset.toDpOffset(density: Density): DpOffset {
    return with(density) { DpOffset(x.toDp(), y.toDp()) }
}