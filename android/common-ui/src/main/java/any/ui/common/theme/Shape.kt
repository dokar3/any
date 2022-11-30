package any.ui.common.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

val Shapes.thumb: CornerBasedShape
    @Composable
    get() = Shapes.small