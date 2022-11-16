package any.ui.settings.services

import any.base.R as BaseR
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.data.entity.ServiceManifest
import any.data.entity.ServiceResource
import any.domain.entity.UiServiceManifest
import any.ui.common.widget.Avatar

@Composable
internal fun ServiceItem(
    service: UiServiceManifest,
    modifier: Modifier = Modifier,
    showServiceSource: Boolean = false,
    actionButton: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .alpha(if (service.areApiVersionsCompatible) 1f else ContentAlpha.disabled)
            .fillMaxWidth(),
    ) {
        Avatar(
            name = service.name,
            url = service.localFirstResourcePath(
                type = ServiceResource.Type.Icon,
                fallback = { service.icon }
            ),
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = service.name.trim('\n'),
                    modifier = Modifier.weight(weight = 1f, fill = false),
                    fontWeight = FontWeight.Medium,
                )

                Row {
                    Spacer(modifier = Modifier.width(6.dp))
                    if (!service.areApiVersionsCompatible) {
                        TextTag(
                            backgroundColor = MaterialTheme.colors.onBackground,
                            text = stringResource(BaseR.string.incompatible),
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (showServiceSource) {
                        val tag = when (service.source) {
                            ServiceManifest.Source.Unspecified -> BaseR.string.unknown
                            ServiceManifest.Source.Builtin -> BaseR.string.builtin
                            ServiceManifest.Source.Remote -> BaseR.string.network
                            ServiceManifest.Source.Local -> BaseR.string.storage
                        }
                        TextTag(
                            backgroundColor = MaterialTheme.colors.primary,
                            text = stringResource(tag),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = service.description?.toString() ?: "",
                color = MaterialTheme.colors.onBackground.copy(
                    alpha = ContentAlpha.medium,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (actionButton != null) {
            actionButton()
        }
    }
}

@Composable
private fun TextTag(
    backgroundColor: Color,
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colors.onBackground,
) {
    Text(
        text = text,
        modifier = modifier
            .tagStyled(color = backgroundColor)
            .padding(horizontal = 4.dp),
        color = textColor,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
    )
}

private fun Modifier.tagStyled(
    color: Color,
    height: Dp = 18.dp,
): Modifier {
    return height(height)
        .background(
            color = color.copy(alpha = 0.1f),
            shape = RoundedCornerShape(2.dp),
        )
}