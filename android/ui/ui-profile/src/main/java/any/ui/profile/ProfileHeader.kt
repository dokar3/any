package any.ui.profile

import any.base.R as BaseR
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import any.base.ImmutableHolder
import any.base.image.ImageRequest
import any.domain.entity.UiUser
import any.richtext.RichContent
import any.richtext.renderTexts
import any.ui.common.R
import any.ui.common.image.AsyncImage
import any.ui.common.richtext.RichTextStyle
import any.ui.common.theme.placeholder
import any.ui.common.theme.secondaryText
import any.ui.common.widget.Avatar
import io.dokar.expandabletext.ExpandableText
import kotlinx.coroutines.launch

@Composable
internal fun ProfileHeaderBanner(
    listScrollYProvider: () -> Int?,
    contentOffsetYProvider: () -> Float,
    heightUpdater: (Int) -> Unit,
    url: String?,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 5f / 2f,
    backgroundColor: Color = Color(0xffacacac),
) {
    var bannerHeight by remember { mutableStateOf(0) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .onSizeChanged {
                bannerHeight = it.height
                heightUpdater(it.height)
            }
            .graphicsLayer {
                val scrollY = listScrollYProvider()
                if (scrollY != null) {
                    translationY = scrollY.toFloat()
                }

                val offsetY = contentOffsetYProvider()
                if (offsetY > 0) {
                    val scale = (bannerHeight + offsetY) / bannerHeight
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    scaleX = scale
                    scaleY = scale
                }
            }
            .drawWithCache {
                onDrawWithContent {
                    if (listScrollYProvider() != null) {
                        drawRect(color = backgroundColor)
                        drawContent()
                    }
                }
            },
    ) {
        if (url != null) {
            AsyncImage(
                request = ImageRequest.Url(url),
                contentDescription = stringResource(BaseR.string.user_banner_image),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
internal fun ProfileHeader(
    onFollowClick: () -> Unit,
    user: UiUser?,
    bannerHeight: Int,
    modifier: Modifier = Modifier,
    themeColor: Color = MaterialTheme.colors.primary,
) {
    val density = LocalDensity.current

    val res = LocalContext.current.resources

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            val avatarSize = 88.dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(with(density) { bannerHeight.toDp() }))

                Spacer(modifier = Modifier.height(avatarSize / 2 + 12.dp))

                UserNames(
                    username = user?.name,
                    alternativeName = user?.alternativeName,
                )

                Spacer(modifier = Modifier.height(16.dp))

                ActionsBar(
                    onFollowClick = onFollowClick,
                    isFollowed = user?.isFollowed() == true,
                    showPlaceholder = user == null,
                    themeColor = themeColor,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Description(
                    text = user?.description,
                    showPlaceholder = user == null,
                )

                val counts = remember(
                    user?.postCount,
                    user?.followerCount,
                    user?.followingCount,
                ) {
                    val list = mutableListOf<Pair<String, Int>>()
                    val postCount = user?.postCount
                    if (postCount != null) {
                        list.add(res.getString(BaseR.string.posts) to postCount)
                    }
                    val followerCount = user?.followerCount
                    if (followerCount != null) {
                        list.add(res.getString(BaseR.string.followers) to followerCount)
                    }
                    val followingCount = user?.followingCount
                    if (followingCount != null) {
                        list.add(res.getString(BaseR.string.following) to followingCount)
                    }
                    list
                }
                if (counts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CountsBar(counts = ImmutableHolder(counts))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { bannerHeight.toDp() })
                    .padding(16.dp),
            ) {
                if (user != null) {
                    ServiceName(
                        serviceName = user.serviceName,
                        modifier = Modifier.align(Alignment.BottomEnd),
                        backgroundColor = themeColor,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .offset { IntOffset(0, bannerHeight - avatarSize.roundToPx() / 2) }
                    .background(
                        color = MaterialTheme.colors.background,
                        shape = CircleShape,
                    )
                    .padding(8.dp)
                    .align(Alignment.TopCenter),
            ) {
                Avatar(
                    name = user?.name ?: "",
                    url = user?.avatar,
                    size = Dp.Unspecified,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (user != null) {
                Divider(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun ActionsBar(
    onFollowClick: () -> Unit,
    isFollowed: Boolean,
    showPlaceholder: Boolean,
    modifier: Modifier = Modifier,
    themeColor: Color = MaterialTheme.colors.primary,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Box {
            val overlayButtonScale = remember { Animatable(if (isFollowed) 0f else 1f) }
            var skipInitialAnim by remember { mutableStateOf(true) }
            var prevState by remember { mutableStateOf(isFollowed) }

            LaunchedEffect(isFollowed) {
                if (prevState == isFollowed) {
                    return@LaunchedEffect
                }
                if (skipInitialAnim) {
                    prevState = isFollowed
                    skipInitialAnim = false
                    return@LaunchedEffect
                }
                prevState = false
                launch {
                    overlayButtonScale.snapTo(if (isFollowed) 0f else 1f)
                    overlayButtonScale.animateTo(
                        targetValue = if (isFollowed) 1f else 0f,
                        animationSpec = tween(durationMillis = 375),
                    )
                    prevState = isFollowed
                }
            }

            FollowButton(
                onClick = {
                    skipInitialAnim = false
                    onFollowClick()
                },
                isFollowed = prevState,
                showPlaceholder = showPlaceholder,
                themeColor = themeColor,
            )

            if (overlayButtonScale.isRunning) {
                FollowButton(
                    onClick = {},
                    isFollowed = true,
                    showPlaceholder = false,
                    modifier = Modifier.graphicsLayer {
                        scaleX = overlayButtonScale.value
                        scaleY = overlayButtonScale.value
                    },
                    themeColor = themeColor,
                )
            }
        }
    }
}

@Composable
private fun FollowButton(
    onClick: () -> Unit,
    isFollowed: Boolean,
    modifier: Modifier = Modifier,
    showPlaceholder: Boolean = false,
    themeColor: Color = MaterialTheme.colors.primary,
) {
    val icon: ImageVector
    val iconColor: Color
    val backgroundColor: Color
    val buttonBorder: BorderStroke?
    if (isFollowed) {
        icon = Icons.Default.Check
        iconColor = themeColor
        backgroundColor = MaterialTheme.colors.background
        buttonBorder = BorderStroke(width = 1.dp, color = iconColor)
    } else {
        icon = Icons.Default.Add
        iconColor = Color.White
        backgroundColor = themeColor
        buttonBorder = null
    }
    Button(
        onClick = onClick,
        modifier = modifier
            .sizeIn(minWidth = 72.dp)
            .alpha(if (showPlaceholder) 0.5f else 1f),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = iconColor,
        ),
        border = buttonBorder,
        contentPadding = PaddingValues(0.dp),
    ) {
        if (!showPlaceholder) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(BaseR.string.follow),
            )
        }
    }
}

@Composable
private fun ServiceName(
    serviceName: String?,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.primary,
) {
    if (!serviceName.isNullOrEmpty()) {
        Row(
            modifier = modifier
                .background(
                    color = backgroundColor,
                    shape = MaterialTheme.shapes.small,
                )
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_app),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.White,
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = serviceName,
                fontSize = 14.sp,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun UserNames(
    username: String?,
    alternativeName: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = username ?: "",
            modifier = Modifier
                .width(if (username == null) 120.dp else Dp.Unspecified)
                .background(
                    color = if (username == null) {
                        MaterialTheme.colors.placeholder
                    } else {
                        Color.Transparent
                    },
                ),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        if (!alternativeName.isNullOrEmpty()) {
            Text(
                text = alternativeName,
                color = MaterialTheme.colors.secondaryText,
            )
        }
    }
}

@Composable
private fun Description(
    text: RichContent?,
    showPlaceholder: Boolean,
    modifier: Modifier = Modifier,
    richTextStyle: RichTextStyle = RichTextStyle.Default,
) {
    val annotatedString = remember(text, richTextStyle) {
        text?.elements?.renderTexts(
            linkColor = richTextStyle.linkColor,
            inlineCodeBackgroundColor = richTextStyle.linkColor,
        )
    }
    if (annotatedString != null) {
        var expanded by remember { mutableStateOf(false) }
        ExpandableText(
            expanded = expanded,
            text = annotatedString.text,
            collapsedMaxLines = 3,
            modifier = modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = !expanded },
                ),
            toggle = {
                if (!expanded) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(BaseR.string.show_all),
                        modifier = Modifier
                            .size(width = 24.dp, height = 20.dp)
                            .rotate(90f),
                        tint = MaterialTheme.colors.primary,
                    )
                }
            },
            lineHeight = 1.5.em,
        )
    } else if (showPlaceholder) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val lineHeight = with(LocalDensity.current) {
                LocalTextStyle.current.fontSize.toDp() * 1.5f
            }
            val widthFrictions = arrayOf(0.7f, 0.5f)
            for ((idx, friction) in widthFrictions.withIndex()) {
                Spacer(
                    modifier = Modifier
                        .padding(
                            bottom = if (idx != widthFrictions.lastIndex) 8.dp else 0.dp,
                        )
                        .fillMaxWidth(friction)
                        .height(lineHeight)
                        .background(MaterialTheme.colors.placeholder.copy(alpha = 0.7f))
                )
            }
        }
    }
}

@Composable
private fun CountsBar(
    counts: ImmutableHolder<List<Pair<String, Int>>>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        for ((name, count) in counts.value) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = name,
                    fontSize = 14.sp,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (count >= 0) count.toString() else "?",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}