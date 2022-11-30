package any.ui.common.menu

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.ui.common.theme.secondaryText
import any.ui.common.widget.OptionsText
import any.ui.common.widget.TextOption

internal fun LazyListScope.headerItem(
    title: String,
    subTitle: String,
) {
    item {
        MenuItem(
            clickable = false,
            padding = PaddingValues(16.dp, 6.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OptionsText(
                    text = subTitle,
                    options = listOf(
                        TextOption.Copy(stringResource(BaseR.string.copy)),
                    ),
                    isSelectable = false,
                    color = MaterialTheme.colors.secondaryText,
                )
            }
        }
    }
}

internal fun LazyListScope.cancelItem(
    text: String,
    onClick: () -> Unit,
) {
    item {
        MenuItem(
            icon = {
                Icon(
                    painter = painterResource(CommonUiR.drawable.ic_baseline_close_24),
                    contentDescription = null,
                )
            },
            onClick = onClick,
        ) {
            Text(text)
        }
    }
}