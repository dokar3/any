package any.ui.post.menu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import any.domain.entity.UiPost
import any.download.PostImageDownloader
import any.download.PostImageDownloader.DownloadStatus
import any.ui.common.theme.pass
import any.ui.common.widget.SimpleDialog
import any.base.R as BaseR
import any.ui.common.R as CommonUiR

@Composable
internal fun DownloadItem(
    post: UiPost,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val context = LocalContext.current

    val downloader = remember { PostImageDownloader.get(context) }

    val startDownload: () -> Unit = remember {
        {
            downloader.downloadPostImages(post.raw)
        }
    }

    var maxProgressWidth by remember { mutableIntStateOf(0) }
    var progressTextWidth by remember { mutableIntStateOf(0) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    val status by remember(post) {
        downloader.getDownloadStatus(post.raw)
    }.collectAsState(DownloadStatus.FetchingStatus)

    val progress = if (status.downloaded >= 0 && status.total >= 0) {
        (status.downloaded.toFloat() / status.total).coerceIn(0f, 1f)
    } else {
        0f
    }

    val isDownloading = status is DownloadStatus.Downloading

    val isWaiting = status is DownloadStatus.Waiting

    val isComplete = status.let { it is DownloadStatus.Finished && it.isComplete() }

    val isFailure = status is DownloadStatus.Failure

    val progressTextColor = if (maxProgressWidth > 0 &&
        progressTextWidth > 0 &&
        !isComplete &&
        maxProgressWidth * progress >= progressTextWidth
    ) {
        MaterialTheme.colors.onPrimary
    } else {
        MaterialTheme.colors.onBackground
    }

    val deleteActionIcon: @Composable () -> Unit = remember {
        {
            Icon(
                painter = painterResource(CommonUiR.drawable.ic_baseline_delete_outline_24),
                contentDescription = stringResource(BaseR.string.delete_downloaded_images),
                tint = MaterialTheme.colors.error,
            )
        }
    }

    val showDelete = status.downloaded > 1 && !isDownloading && !isWaiting

    Box {
        val progressColor = if (isComplete || progress == 0f) {
            Color.Transparent
        } else if (isDownloading) {
            MaterialTheme.colors.primary
        } else if (isFailure) {
            MaterialTheme.colors.error
        } else {
            MaterialTheme.colors.onBackground.copy(alpha = 0.3f)
        }
        DownloadButton(
            downloaded = status.downloaded,
            total = status.total,
            progressColor = progressColor,
            modifier = modifier.fillMaxWidth(),
            progressText = {
                Row(
                    modifier = Modifier
                        .onSizeChanged { progressTextWidth = it.width }
                        .padding(contentPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isDownloading || isWaiting || isComplete) {
                        Icon(
                            painter = painterResource(CommonUiR.drawable.ic_download),
                            contentDescription = stringResource(BaseR.string.download),
                            modifier = Modifier
                                .size(24.dp)
                                .pulseAlphaAnimation(enabled = isWaiting),
                            tint = progressTextColor,
                        )

                        val spacing = contentPadding.calculateStartPadding(
                            LocalLayoutDirection.current
                        )
                        Spacer(modifier = Modifier.width(spacing))
                    } else {
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Text(
                        text = "${status.downloaded} / ${status.total}",
                        modifier = Modifier.heightIn(min = 20.dp),
                        color = progressTextColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            onMaxProgressWidthChange = { maxProgressWidth = it },
            onSecondaryAction = { showDeleteDialog = true },
            secondaryAction = if (showDelete) deleteActionIcon else null,
            actionEnabled = !isComplete,
            onAction = {
                if (isDownloading || isWaiting) {
                    downloader.cancel(post.url)
                } else if (!isComplete) {
                    startDownload()
                }
            },
            action = {
                if (isDownloading || isWaiting) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(BaseR.string.cancel_download),
                    )
                } else if (isComplete) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(BaseR.string.downloaded),
                        tint = MaterialTheme.colors.pass,
                    )
                } else {
                    val actionIconColor = if (status is DownloadStatus.Failure) {
                        MaterialTheme.colors.error
                    } else {
                        MaterialTheme.colors.onBackground
                    }
                    Icon(
                        painter = painterResource(CommonUiR.drawable.ic_download),
                        contentDescription = stringResource(BaseR.string.download),
                        tint = actionIconColor,
                    )
                }
            },
        )
    }

    if (showDeleteDialog) {
        SimpleDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(BaseR.string.delete)) },
            text = { Text(stringResource(BaseR.string.delete_downloaded_images_alert)) },
            cancelText = { Text(stringResource(android.R.string.cancel)) },
            confirmText = {
                Text(
                    text = stringResource(BaseR.string.delete),
                    color = MaterialTheme.colors.error,
                )
            },
            onConfirmClick = { downloader.delete(post.raw) },
        )
    }
}

@Composable
private fun DownloadButton(
    downloaded: Int,
    total: Int,
    progressColor: Color,
    modifier: Modifier = Modifier,
    progressText: @Composable (() -> Unit)? = null,
    onMaxProgressWidthChange: (Int) -> Unit = {},
    secondaryActionEnabled: Boolean = true,
    onSecondaryAction: () -> Unit = {},
    secondaryAction: @Composable (() -> Unit)? = null,
    actionEnabled: Boolean = true,
    onAction: () -> Unit = {},
    action: @Composable () -> Unit,
) {
    val progress = (downloaded.toFloat() / total).coerceIn(0f, 1f)

    Row(
        modifier = modifier.height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { onMaxProgressWidthChange(it.width) }
                    .drawBehind {
                        drawRect(
                            color = progressColor,
                            size = size.copy(width = size.width * progress),
                        )
                    },
            )

            if (progressText != null) {
                progressText()
            }
        }

        Row(modifier = Modifier) {
            if (secondaryAction != null) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(min = 42.dp)
                        .clickable(
                            enabled = secondaryActionEnabled,
                            onClick = onSecondaryAction
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    secondaryAction()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = 42.dp)
                    .clickable(
                        enabled = actionEnabled,
                        onClick = onAction,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                action()
            }
        }
    }
}

fun Modifier.pulseAlphaAnimation(
    enabled: Boolean = true,
    minAlpha: Float = 0f,
    maxAlpha: Float = 1f,
    animSpec: DurationBasedAnimationSpec<Float> = tween(durationMillis = 1000),
): Modifier = composed {
    val alpha = remember(minAlpha, maxAlpha) { Animatable(initialValue = maxAlpha) }

    LaunchedEffect(alpha, animSpec, enabled) {
        if (!enabled) {
            alpha.snapTo(maxAlpha)
            return@LaunchedEffect
        }
        alpha.animateTo(
            targetValue = minAlpha,
            animationSpec = infiniteRepeatable(
                animation = animSpec,
                repeatMode = RepeatMode.Reverse,
            )
        )
    }

    alpha(alpha.value)
}
