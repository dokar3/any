package any.ui.common.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EndOfList(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    text: @Composable () -> Unit,
) {
    val textColor = MaterialTheme.colors.onBackground.copy(alpha = 0.5f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onClick?.invoke() },
            )
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StrokeLine(color = textColor)
        Spacer(modifier = Modifier.width(8.dp))
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(
                color = textColor,
                fontSize = 14.sp,
            )
        ) {
            text()
        }
        Spacer(modifier = Modifier.width(8.dp))
        StrokeLine(color = textColor)
    }
}

@Composable
private fun StrokeLine(
    color: Color,
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier = modifier
            .height(1.dp)
            .width(36.dp)
            .background(color)
    )
}