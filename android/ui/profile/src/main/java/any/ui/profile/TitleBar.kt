package any.ui.profile

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.util.applyLightStatusBar
import any.base.util.clearLightStatusBar
import any.ui.common.theme.sizes
import any.ui.common.theme.topBarBackground
import any.ui.common.widget.AnimatedPopup
import any.ui.common.widget.StatusBarSpacer
import any.ui.common.widget.TitleActionButton
import any.ui.common.widget.rememberAnimatedPopupDismissRequester
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import any.base.R as BaseR

@Composable
internal fun TitleBar(
    scrollProvider: () -> Int,
    onBack: () -> Unit,
    title: String?,
    fullyVisibleScrollY: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.topBarBackground,
    isLightTheme: Boolean = MaterialTheme.colors.isLight,
    contentPadding: PaddingValues = PaddingValues(8.dp),
) {
    val window = (LocalActivity.current as Activity).window

    val view = LocalView.current

    var scrollProgress by remember { mutableFloatStateOf(0f) }

    var contentColor by remember { mutableStateOf(Color.White) }

    LaunchedEffect(fullyVisibleScrollY) {
        snapshotFlow { scrollProvider() }
            .map {
                if (fullyVisibleScrollY != 0) {
                    (it.toFloat() / fullyVisibleScrollY).coerceIn(0f, 1f)
                } else {
                    0f
                }
            }
            .distinctUntilChanged()
            .collect {
                scrollProgress = it
                if (isLightTheme) {
                    contentColor = Color.Black.copy(alpha = it).compositeOver(Color.White)
                }
            }
    }

    LaunchedEffect(view, window, isLightTheme) {
        if (!isLightTheme) {
            return@LaunchedEffect
        }
        snapshotFlow { scrollProgress }
            .map { it > 0.5f }
            .distinctUntilChanged()
            .collect { darkIcons ->
                if (darkIcons) {
                    view.applyLightStatusBar(window)
                } else {
                    view.clearLightStatusBar(window)
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                drawRect(
                    color = backgroundColor,
                    alpha = scrollProgress,
                )
                drawContent()
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        StatusBarSpacer()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MaterialTheme.sizes.titleBarHeight)
                .padding(contentPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TitleActionButton(
                    label = stringResource(BaseR.string.back),
                    onClick = onBack,
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(BaseR.string.back),
                        tint = contentColor,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                TitleText(
                    text = title,
                    alpha = { scrollProgress },
                    color = { contentColor },
                )
            }

            Spacer(modifier = Modifier)
        }

        Divider(
            modifier = Modifier.graphicsLayer {
                alpha = scrollProgress
            },
        )
    }
}

@Composable
private fun TitleText(
    text: String?,
    alpha: () -> Float,
    color: () -> Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text ?: "",
        modifier = modifier.graphicsLayer {
            this.alpha = alpha()
        },
        color = color(),
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun MoreButton(
    iconTint: () -> Color,
    modifier: Modifier = Modifier,
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    TitleActionButton(
        label = stringResource(BaseR.string.more_options),
        onClick = { showMoreMenu = true },
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = stringResource(BaseR.string.more_options),
            tint = iconTint(),
        )

        if (showMoreMenu) {
            val popupDismissRequester = rememberAnimatedPopupDismissRequester()
            AnimatedPopup(
                dismissRequester = popupDismissRequester,
                onDismissed = { showMoreMenu = false },
                contentAlignmentToAnchor = Alignment.TopEnd,
                scaleAnimOrigin = TransformOrigin(1f, 0f),
            ) {}
        }
    }
}