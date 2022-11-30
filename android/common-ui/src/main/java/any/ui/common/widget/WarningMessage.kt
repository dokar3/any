package any.ui.common.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import any.ui.common.theme.warn

@Composable
fun WarningMessage(
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.small,
    message: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colors.warn.copy(alpha = 0.08f),
                shape = shape,
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.warn.copy(alpha = 0.2f),
                shape = shape,
            )
            .padding(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colors.warn,
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (title != null) {
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                ) {
                    title()
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        message()
    }
}