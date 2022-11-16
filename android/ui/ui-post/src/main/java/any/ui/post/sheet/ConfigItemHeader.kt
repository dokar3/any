package any.ui.post.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ConfigItemHeader(
    text: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
        )

        if (value != null) {
            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = value,
                modifier = modifier
                    .background(
                        color = MaterialTheme.colors.primary,
                        shape = CircleShape,
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                fontSize = 13.sp,
                color = MaterialTheme.colors.onPrimary,
            )
        }
    }
}