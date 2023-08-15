package any.ui.common.widget

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CheckableItem(
    isChecked: Boolean,
    showCheckmark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (showCheckmark) {
            if (isChecked) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colors.primary,
                )
            } else {
                Spacer(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(2.dp)
                        .border(
                            width = 2.dp,
                            color = LocalContentColor.current,
                            shape = CircleShape,
                        ),
                )
            }
        }

        val itemOffset by animateDpAsState(
            if (showCheckmark) 24.dp + 16.dp else 0.dp,
            label = "itemOffset",
        )
        Box(
            modifier = Modifier.offset(x = itemOffset),
            content = content,
        )
    }
}