package any.ui.service

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.image.ImageRequest
import any.data.entity.ServiceManifest
import any.data.entity.ServiceResource
import any.domain.entity.UiServiceManifest
import any.richtext.RichContent
import any.ui.common.image.AsyncImage
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.richtext.RichText
import any.ui.common.theme.link
import any.ui.common.theme.secondaryText
import any.ui.common.widget.Avatar
import java.text.SimpleDateFormat
import kotlin.math.max
import any.base.R as BaseR
import any.ui.common.R as CommonUiR

@Composable
fun ServiceDetails(
    onOpenLink: (String) -> Unit,
    onShowChangelog: (UiServiceManifest.Changelog) -> Unit,
    service: UiServiceManifest,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val res = context.resources

    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .verticalScrollBar(listState),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            ServiceInfoHeader(service = service)
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                ItemWithHeading(
                    heading = stringResource(BaseR.string.service_source),
                    modifier = Modifier.weight(1f),
                ) {
                    val sourceRes = when (service.source) {
                        ServiceManifest.Source.Unspecified -> BaseR.string.unknown
                        ServiceManifest.Source.Builtin -> BaseR.string.builtin
                        ServiceManifest.Source.Remote -> BaseR.string.network
                        ServiceManifest.Source.Local -> BaseR.string.storage
                    }
                    Text(stringResource(sourceRes))
                }

                ItemWithHeading(
                    heading = stringResource(BaseR.string.developer),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val avatar = service.developerAvatar
                        if (!avatar.isNullOrEmpty()) {
                            Avatar(name = service.developer, url = avatar)
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        val url = service.developerUrl
                        if (!url.isNullOrEmpty()) {
                            LinkText(text = service.developer, onClick = { onOpenLink(url) })
                        } else {
                            Text(text = service.developer)
                        }
                    }
                }
            }
        }

        item {
            ItemWithHeading(heading = stringResource(BaseR.string.description)) {
                RichText(
                    content = service.description ?: RichContent.Empty,
                    onLinkClick = onOpenLink,
                )
            }
        }

        val languages = service.languages
        if (!languages.isNullOrEmpty()) {
            item {
                ItemWithHeading(heading = stringResource(BaseR.string.languages)) {
                    Text(text = languages.joinToString("\n") { it.displayName })
                }
            }
        }

        item {
            ItemWithHeading(heading = stringResource(BaseR.string.dates)) {
                Dates(
                    buildTime = service.buildTime,
                    addedAt = service.addedAt,
                    updatedAt = service.updatedAt,
                )
            }
        }

        val supportedPostUrls = service.supportedPostUrls
        if (!supportedPostUrls.isNullOrEmpty()) {
            item {
                ItemWithHeading(heading = stringResource(BaseR.string.supported_post_urls)) {
                    Text(text = supportedPostUrls.joinToString("\n"))
                }
            }
        }

        val supportedUserUrls = service.supportedUserUrls
        if (!supportedUserUrls.isNullOrEmpty()) {
            item {
                ItemWithHeading(heading = res.getString(BaseR.string.supported_user_urls)) {
                    Text(text = supportedUserUrls.joinToString("\n"))
                }
            }
        }

        val homepage = service.homepage
        val changelog = service.changelog
        if (changelog != null || !homepage.isNullOrEmpty()) {
            item {
                ItemWithHeading(heading = stringResource(BaseR.string.more_info)) {
                    if (!homepage.isNullOrEmpty()) {
                        LinkText(
                            text = stringResource(BaseR.string.homepage),
                            onClick = { onOpenLink(homepage) },
                        )
                    }

                    if (changelog != null) {
                        if (!homepage.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        LinkText(
                            text = stringResource(BaseR.string.changelog),
                            onClick = { onShowChangelog(changelog) },
                            showTrailingIcon = changelog is UiServiceManifest.Changelog.Url,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemWithHeading(
    heading: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(8.dp))

        Header(name = heading)

        Spacer(modifier = Modifier.height(4.dp))

        content()

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun Header(
    name: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = name,
        modifier = modifier,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun LinkText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTrailingIcon: Boolean = true,
) {
    val displayText = remember(showTrailingIcon) {
        buildAnnotatedString {
            append(text)
            if (showTrailingIcon) {
                appendInlineContent("trailingIcon")
            }
        }
    }
    Text(
        text = displayText,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
        color = MaterialTheme.colors.link,
        textDecoration = TextDecoration.Underline,
        inlineContent = mapOf(
            "trailingIcon" to InlineTextContent(
                placeholder = Placeholder(
                    width = 20.sp,
                    height = 16.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                ),
                children = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        val icon = CommonUiR.drawable.ic_baseline_open_in_new_24
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = MaterialTheme.colors.link,
                        )
                    }
                }
            )
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServiceInfoHeader(
    service: UiServiceManifest,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val icon = service.localFirstResourcePath(
            type = ServiceResource.Type.Icon,
            fallback = { service.icon }
        )
        if (!icon.isNullOrEmpty()) {
            AsyncImage(
                request = ImageRequest.Url(icon),
                contentDescription = null,
                modifier = Modifier.size(96.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = service.name,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Badge(
                label = { Text(stringResource(BaseR.string.id)) },
                text = { Text(service.id) },
            )

            if (service.id != service.originalId) {
                Badge(
                    label = { Text(stringResource(BaseR.string.original_id)) },
                    text = { Text(service.originalId) },
                )
            }

            Badge(
                label = { Text(stringResource(BaseR.string.version)) },
                text = { Text(service.version) },
            )

            RequiredApiVersionsBadge(
                minApiVersion = service.minApiVersion,
                maxApiVersion = service.maxApiVersion,
            )
        }
    }
}

@Composable
private fun Dates(
    buildTime: Long,
    addedAt: Long,
    updatedAt: Long,
    modifier: Modifier = Modifier,
    cellsHorizontalSpacing: Dp = 8.dp,
    cellsVerticalSpacing: Dp = 4.dp,
) {
    // Table like layout, it looks like:
    // (label)   (content)
    // ------    *******
    // --------- ***************
    //           ****
    // ---       ***********
    // -------   **********
    Layout(
        content = {
            Text(stringResource(BaseR.string.build_time))
            DateText(date = buildTime)

            if (addedAt >= 0) {
                Text(stringResource(BaseR.string.added_at))
                DateText(date = addedAt)
            }

            if (updatedAt >= 0) {
                Text(stringResource(BaseR.string.updated_at))
                DateText(date = updatedAt)
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val labels = mutableListOf<Measurable>()
        val contents = mutableListOf<Measurable>()
        for (i in measurables.indices) {
            if (i % 2 == 0) {
                labels.add(measurables[i])
            } else {
                contents.add(measurables[i])
            }
        }

        val hSpacing = cellsHorizontalSpacing.roundToPx()
        val vSpacing = cellsVerticalSpacing.roundToPx()

        // Measure labels first to get the max width of labels
        val labelPlaceables = labels.map { it.measure(constraints) }

        val maxLabelWidth = labelPlaceables.maxOf { it.width }
        val contentConstraints = constraints.copy(
            maxWidth = constraints.maxWidth - maxLabelWidth - hSpacing
        )
        // Measure contents
        val contentPlaceables = contents.map { it.measure(contentConstraints) }

        val maxContentWidth = contentPlaceables.maxOf { it.width }
        // Calculate the layout width
        val width = maxLabelWidth + hSpacing + maxContentWidth

        val labelsHeight = labelPlaceables.sumOf { it.height + vSpacing } - vSpacing
        val contentsHeight = contentPlaceables.sumOf { it.height + vSpacing } - vSpacing
        // Calculate the layout height
        val height = max(labelsHeight, contentsHeight).coerceAtLeast(0)

        layout(width, height) {
            var y = 0
            for (i in labelPlaceables.indices) {
                val l = labelPlaceables[i]
                // Place the label
                l.placeRelative(0, y)

                val contentHeight = if (i <= contentPlaceables.lastIndex) {
                    val c = contentPlaceables[i]
                    // Place the content
                    c.placeRelative(maxLabelWidth + hSpacing, y)
                    c.height
                } else {
                    0
                }

                y += max(l.height, contentHeight) + vSpacing
            }
        }
    }
}

@Composable
private fun DateText(
    date: Long,
    modifier: Modifier = Modifier,
) {
    val dateString = if (date >= 0) {
        remember(date) { SimpleDateFormat.getDateTimeInstance().format(date) }
    } else {
        stringResource(BaseR.string.unknown)
    }
    Text(
        text = dateString,
        modifier = modifier,
        color = MaterialTheme.colors.secondaryText,
    )
}
