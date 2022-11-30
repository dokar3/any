package any.ui.home.following

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.ImmutableHolder
import any.base.util.compose.performLongPress
import any.domain.entity.UiUser
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.theme.secondaryText
import any.ui.common.theme.themeColorOrPrimary
import any.ui.common.widget.Avatar
import any.ui.common.widget.CheckableItem
import any.ui.home.HomeScreenDefaults

@Composable
internal fun FollowingList(
    onItemClick: (UiUser) -> Unit,
    onItemLongClick: (UiUser) -> Unit,
    users: ImmutableHolder<List<UiUser>>,
    selection: ImmutableHolder<Set<String>>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .verticalScrollBar(
                state = listState,
                padding = contentPadding,
            ),
        state = listState,
        contentPadding = contentPadding,
    ) {
        items(
            items = users.value,
            key = { it.serviceId + it.id },
            contentType = { "user_item" },
        ) {
            UserItem(
                onClick = { onItemClick(it) },
                onLongClick = { onItemLongClick(it) },
                user = it,
                isSelected = selection.value.contains(it.id),
                showSelection = selection.value.isNotEmpty(),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserItem(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    user: UiUser,
    isSelected: Boolean,
    showSelection: Boolean,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current

    CheckableItem(
        isChecked = isSelected,
        showCheckmark = showSelection,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    hapticFeedback.performLongPress()
                    onLongClick()
                },
            )
            .padding(HomeScreenDefaults.ListItemPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(name = user.name, url = user.avatar, size = 52.dp)

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                val secondaryTextColor = MaterialTheme.colors.secondaryText
                val name = remember(user.name, user.alternativeName, secondaryTextColor) {
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(user.name)
                        }
                        if (!user.alternativeName.isNullOrEmpty()) {
                            withStyle(SpanStyle(color = secondaryTextColor)) {
                                append(" (${user.alternativeName})")
                            }
                        }
                    }
                }
                Text(text = name)

                Spacer(modifier = Modifier.height(8.dp))

                val tagColor = themeColorOrPrimary(
                    themeColor = Color(user.serviceThemeColor),
                    darkThemeColor = Color(user.serviceDarkThemeColor),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = tagColor.copy(alpha = 0.2f),
                            shape = CircleShape,
                        )
                        .padding(
                            start = 4.dp,
                            top = 4.dp,
                            end = 8.dp,
                            bottom = 4.dp
                        ),
                ) {
                    Avatar(
                        name = user.serviceName ?: "?",
                        url = user.serviceIcon,
                        size = 18.dp,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = user.serviceName ?: "?",
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}