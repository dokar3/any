package any.ui.service

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.R

@Composable
internal fun RequiredApiVersionsBadge(
    minApiVersion: String,
    maxApiVersion: String?,
    modifier: Modifier = Modifier,
) {
    Badge(
        label = {
            Text(stringResource(R.string.required_api_version))
        },
        text = {
            val text = if (!maxApiVersion.isNullOrEmpty()) {
                ">= $minApiVersion, <= $maxApiVersion"
            } else {
                ">= $minApiVersion"
            }
            Text(text)
        },
        textBackgroundColor = MaterialTheme.colors.primary,
        modifier = modifier,
    )
}

@Composable
internal fun Badge(
    label: @Composable () -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    textBackgroundColor: Color = MaterialTheme.colors.primary,
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(Color.DarkGray),
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(
                fontSize = 12.sp,
                color = Color.White,
            )
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
            ) {
                label()
            }

            Spacer(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.White),
            )

            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(
                    color = contentColorFor(textBackgroundColor),
                )
            ) {
                Box(
                    modifier = Modifier
                        .background(textBackgroundColor)
                        .padding(start = 4.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)
                ) {
                    text()
                }
            }
        }
    }
}
