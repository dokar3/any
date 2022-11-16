package any.ui.jslogger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.data.js.plugin.DefaultLogPlugin
import any.ui.common.R
import any.ui.common.isLastItemVisible
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.theme.warn
import any.ui.common.widget.EmojiEmptyContent
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun LoggerScreen(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        SelectionContainer {
            val messages by DefaultLogPlugin.messageFlow()
                .collectAsState(initial = DefaultLogPlugin.messages())

            val listState = rememberLazyListState()

            LaunchedEffect(listState) {
                launch {
                    snapshotFlow { listState.layoutInfo.totalItemsCount }
                        .filter { it > 0 }
                        .collect {
                            listState.scrollToItem(it - 1)
                            cancel()
                        }
                }
            }

            LaunchedEffect(listState) {
                DefaultLogPlugin.messageFlow()
                    .map { it.size }
                    .filter { it > 0 }
                    .collect {
                        if (listState.isLastItemVisible()) {
                            listState.animateScrollToItem(it - 1)
                        }
                    }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScrollBar(state = listState),
                state = listState,
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            EmojiEmptyContent {
                                Text("Nothing here")
                            }
                        }
                    }
                }

                items(items = messages) {
                    LogItemView(item = it)
                }
            }
        }

        FloatingActionButton(
            onClick = { DefaultLogPlugin.clear() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset((-32).dp, (-32).dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_clear_all_24),
                contentDescription = "Clear all",
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun LogItemView(
    item: DefaultLogPlugin.LogItem,
    modifier: Modifier = Modifier,
) {
    val timeColor = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
    val normalColor = MaterialTheme.colors.onBackground.copy(alpha = 0.5f)
    val errorColor = MaterialTheme.colors.error.copy(alpha = 0.9f)
    val warnColor = MaterialTheme.colors.warn.copy(alpha = 0.9f)

    val text = remember(item, timeColor, normalColor, errorColor, warnColor) {
        buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    color = timeColor,
                    fontWeight = FontWeight.SemiBold,
                )
            ) {
                append(DefaultLogPlugin.logDateFormat.format(item.time))

                val tag = item.tag
                if (!tag.isNullOrEmpty()) {
                    append(" [")
                    append(tag)
                    append("]")
                }

                append(": ")
            }

            val messageColor = when (item.level) {
                DefaultLogPlugin.LogLevel.Error -> errorColor
                DefaultLogPlugin.LogLevel.Warn -> warnColor
                else -> normalColor
            }
            withStyle(style = SpanStyle(color = messageColor)) {
                append(item.message)
            }
        }
    }

    Text(
        text = text,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        fontSize = 14.sp,
    )
}