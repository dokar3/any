package any.ui.service

import any.base.R as BaseR
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.R
import any.base.util.Intents
import any.domain.entity.UiServiceManifest
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.richtext.Markdown
import com.dokar.sheets.BottomSheet
import com.dokar.sheets.BottomSheetState
import kotlinx.coroutines.launch

@Composable
fun ServiceDetailsSheet(
    state: BottomSheetState,
    service: UiServiceManifest,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    fun openLink(url: String) {
        Intents.openInBrowser(context, url)
    }

    BottomSheet(state = state, modifier = modifier, skipPeeked = true) {
        Column(modifier = Modifier.fillMaxWidth()) {
            var changelogToShow by remember { mutableStateOf<String?>(null) }

            // A simple Box like layout but always restrict its size to the same as
            // the first child's
            Layout(
                content = {
                    val detailsAlpha by animateFloatAsState(
                        targetValue = if (changelogToShow == null) 1f else 0f
                    )
                    ServiceDetails(
                        onOpenLink = ::openLink,
                        onShowChangelog = {
                            when (it) {
                                is UiServiceManifest.Changelog.Text -> changelogToShow = it.text
                                is UiServiceManifest.Changelog.Url -> Intents.openInBrowser(context, it.url)
                            }
                        },
                        service = service,
                        modifier = Modifier.graphicsLayer {
                            alpha = detailsAlpha
                            translationY = (-120).dp.toPx() * (1f - detailsAlpha)
                        },
                    )

                    AnimatedVisibility(
                        visible = changelogToShow != null,
                        enter = slideInVertically(
                            animationSpec = spring()
                        ) { it / 2 } + fadeIn(
                            animationSpec = spring()
                        ),
                        exit = slideOutVertically(
                            animationSpec = spring()
                        ) { it / 2 } + fadeOut(
                            animationSpec = spring()
                        ),
                    ) {
                        var changelog by remember { mutableStateOf("") }

                        LaunchedEffect(changelogToShow) {
                            if (changelogToShow != null) {
                                changelog = changelogToShow!!
                            }
                        }

                        Changelog(
                            onLinkClick = ::openLink,
                            text = changelog,
                            serviceName = service.name,
                            modifier = Modifier
                                .background(MaterialTheme.colors.surface)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {},
                                )
                        )
                    }
                },
                modifier = Modifier.weight(1f, fill = false),
            ) { measureables, constraints ->
                val firstPlaceable = measureables[0].measure(constraints)

                val firstChildConstraints = constraints.copy(maxHeight = firstPlaceable.height)
                val resetPlaceables = List((measureables.size - 1).coerceAtLeast(0)) {
                    measureables[it + 1].measure(firstChildConstraints)
                }

                layout(firstPlaceable.width, firstPlaceable.height) {
                    firstPlaceable.placeRelative(0, 0)
                    for (placeable in resetPlaceables) {
                        placeable.placeRelative(0, 0)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Button(
                    onClick = {
                        if (changelogToShow == null) {
                            scope.launch { state.collapse() }
                        } else {
                            changelogToShow = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    elevation = null,
                ) {
                    Text(
                        text = stringResource(
                            if (changelogToShow == null) {
                                android.R.string.ok
                            } else {
                                BaseR.string.back
                            }
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun Changelog(
    onLinkClick: (String) -> Unit,
    text: String,
    serviceName: String,
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 8.dp,
            )
    ) {
        Text(
            text = stringResource(R.string._changelog_of, serviceName),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val scrollState = rememberScrollState()
            Markdown(
                text = text,
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .verticalScrollBar(scrollState),
                onLinkClick = onLinkClick,
            )
        }
    }
}
