package any.ui.runsql

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import any.ui.common.R as CommonUiR

@Composable
internal fun InputBox(
    text: String,
    onValueChange: (String) -> Unit,
    sendEnabled: Boolean,
    modifier: Modifier = Modifier,
    onSubmit: (text: String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.8.dp)
                .background(MaterialTheme.colors.onBackground.copy(alpha = 0.1f))
                .shadow(2.dp)
        )

        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.surface)
                .padding(12.dp, 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f),
                placeholder = {
                    Text("Sql command here")
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                painter = painterResource(CommonUiR.drawable.ic_baseline_send_24),
                contentDescription = null,
                modifier = Modifier
                    .clickable(
                        enabled = sendEnabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        onClick = { onSubmit(text) },
                    )
                    .padding(16.dp, 12.dp),
                tint = MaterialTheme.colors.primary
            )
        }
    }
}
