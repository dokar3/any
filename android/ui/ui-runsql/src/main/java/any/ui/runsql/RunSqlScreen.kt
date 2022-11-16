package any.ui.runsql

import android.database.Cursor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.room.RoomDatabase
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import any.base.ImmutableHolder
import any.base.util.ClipboardUtil
import any.base.util.performLongPress
import any.data.db.AppDatabase
import any.data.db.PostContentDatabase
import any.ui.common.widget.AnimatedPopup
import any.ui.common.widget.AnimatedPopupItem
import any.ui.common.widget.rememberAnimatedPopupDismissRequester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private suspend fun submitText(
    db: Db,
    currentMessages: List<Message>,
    text: String
): List<Message> = withContext(Dispatchers.IO) {
    if (text == "clear") {
        return@withContext emptyList()
    }

    val newMessages = currentMessages.toMutableList()
    // new input
    newMessages.add(Message(text = AnnotatedString(text), isSelf = true))

    // run sql
    val sql = SimpleSQLiteQuery(text)
    val result = queryDb(db.create(), sql)
    newMessages.add(Message(text = result, isSelf = false))

    return@withContext newMessages
}

private fun queryDb(
    database: RoomDatabase,
    sql: SupportSQLiteQuery
): AnnotatedString {
    return try {
        val cursor = database.query(sql)
        val out = parseCursor(cursor)
        cursor.close()
        out
    } catch (e: Exception) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = Color(0xFFEB4F4F))) {
                append(e.toString())
            }
        }
    }
}

private fun parseCursor(cursor: Cursor): AnnotatedString {
    val annotatedString = buildAnnotatedString {
        while (cursor.moveToNext()) {
            val count = cursor.columnCount
            val names = cursor.columnNames
            for (i in 0 until count) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(names[i])
                    append(": ")
                }

                val value = cursor.getString(i)
                if (value == null) {
                    append("NULL")
                } else {
                    append(value)
                }

                if (i != count - 1) {
                    append(", ")
                }
            }
            append(System.lineSeparator())
        }
    }
    return if (annotatedString.isNotEmpty()) {
        annotatedString.subSequence(0, annotatedString.length - 1)
    } else {
        AnnotatedString("[NO OUTPUT]")
    }
}

@Composable
fun RunSqlScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    val dbs = remember {
        listOf(
            Db(
                name = "App",
                creator = { AppDatabase.get(context) },
            ),
            Db(
                name = "Post Content",
                creator = { PostContentDatabase.get(context) },
            ),
        )
    }

    var selectedDb: Db by remember { mutableStateOf(dbs.first()) }

    var canSend by remember { mutableStateOf(true) }

    var inputText by remember { mutableStateOf("") }

    var messages: List<Message> by remember { mutableStateOf(emptyList()) }

    val listState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                DbSelector(
                    dbs = ImmutableHolder(dbs),
                    selectedDb = selectedDb,
                    onSelect = { selectedDb = it },
                )
            }

            MessageList(
                listState = listState,
                messages = ImmutableHolder(messages),
                onCopyMessage = { ClipboardUtil.copyText(context, it.text.toString()) },
                onEditMessage = { inputText = it.text.toString() },
                modifier = Modifier.weight(weight = 1f, fill = true),
            )

            InputBox(
                text = inputText,
                onValueChange = { inputText = it },
                canSend = canSend,
                modifier = Modifier.height(82.dp),
                onSubmit = { text ->
                    scope.launch {
                        canSend = false
                        messages = submitText(selectedDb, messages, text)
                        canSend = true
                        listState.animateScrollToItem(messages.size - 1)
                        inputText = ""
                    }
                }
            )
        }
    }
}

@Composable
private fun MessageList(
    listState: LazyListState,
    messages: ImmutableHolder<List<Message>>,
    onCopyMessage: (Message) -> Unit,
    onEditMessage: (Message) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth()
    ) {
        items(messages.value) { message ->
            MessageItem(
                message,
                onCopyMessage = { onCopyMessage(message) },
                onEditMessage = { onEditMessage(message) },
            )
        }
    }
}

@Composable
private fun MessageItem(
    message: Message,
    onCopyMessage: () -> Unit,
    onEditMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bubbleColor = if (message.isSelf) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.surface
    }

    val textColor = if (message.isSelf) {
        MaterialTheme.colors.onPrimary
    } else {
        MaterialTheme.colors.onSurface
    }

    val bubbleShape = if (message.isSelf) {
        MaterialTheme.shapes.medium.copy(topEnd = CornerSize(4.dp))
    } else {
        MaterialTheme.shapes.medium.copy(topStart = CornerSize(4.dp))
    }

    var allLines by remember { mutableStateOf(0) }

    val maxDisplayLines = 10

    val text = remember(message) {
        val str = message.text
        val chars = str.toString().toCharArray()
        var lineBreaks = 0
        var displayToIndex = 0
        for (i in chars.indices) {
            if (chars[i] == '\n') {
                lineBreaks++
            }
            if (lineBreaks == maxDisplayLines) {
                displayToIndex = i
            }
        }

        allLines = lineBreaks

        if (lineBreaks > maxDisplayLines) {
            str.subSequence(0, displayToIndex + 1)
        } else {
            str
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp, 8.dp)
    ) {
        val hapticFeedback = LocalHapticFeedback.current

        var showMessageMenu by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            hapticFeedback.performLongPress()
                            showMessageMenu = true
                        }
                    )
                }
                .background(
                    color = bubbleColor,
                    shape = bubbleShape
                )
                .let {
                    if (!message.isSelf) {
                        it.border(
                            width = 1.dp,
                            color = MaterialTheme.colors.onBackground.copy(0.15f),
                            shape = bubbleShape
                        )
                    } else {
                        it.align(Alignment.CenterEnd)
                    }
                }
                .padding(12.dp),
        ) {
            val selectionColors = TextSelectionColors(
                handleColor = MaterialTheme.colors.secondary,
                backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.4f)
            )

            val textToolbar = if (showMessageMenu) NoopTextToolBar else LocalTextToolbar.current

            CompositionLocalProvider(
                LocalTextSelectionColors provides selectionColors,
                LocalTextToolbar provides textToolbar,
            ) {
                SelectionContainer {
                    Text(text, color = textColor)
                }
            }

            if (allLines > maxDisplayLines) {
                MoreLines(
                    allLines = allLines,
                    maxDisplayLines = maxDisplayLines,
                    bubbleColor = bubbleColor
                )
            }

            if (showMessageMenu) {
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    MessageMenu(
                        onCopyClick = onCopyMessage,
                        onEditClick = onEditMessage,
                        onDismissRequest = { showMessageMenu = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.MoreLines(
    allLines: Int,
    maxDisplayLines: Int,
    bubbleColor: Color
) {
    var moreLinesHeight by remember {
        mutableStateOf(0)
    }
    val brush = remember(bubbleColor, moreLinesHeight) {
        val colors = listOf(
            Color.Transparent,
            bubbleColor
        )
        ShaderBrush(
            LinearGradientShader(
                Offset.Zero,
                Offset(0f, moreLinesHeight.toFloat()),
                colors
            )
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged {
                moreLinesHeight = it.height
            }
            .align(Alignment.BottomCenter)
            .background(brush = brush)
            .padding(top = 24.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text("+ More ${allLines - maxDisplayLines} lines")
    }
}

@Composable
private fun MessageMenu(
    onDismissRequest: () -> Unit,
    onCopyClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val popupDismissRequester = rememberAnimatedPopupDismissRequester()

    AnimatedPopup(
        dismissRequester = popupDismissRequester,
        onDismissed = onDismissRequest,
        modifier = modifier,
        elevation = 4.dp,
        minWidth = 120.dp,
        contentAlignmentToAnchor = Alignment.TopCenter,
        scaleAnimOrigin = TransformOrigin(0.5f, 0f),
    ) {
        AnimatedPopupItem(
            index = 0,
            onClick = {
                onCopyClick()
                popupDismissRequester.dismiss()
            },
        ) {
            Text("Copy")
        }

        AnimatedPopupItem(
            index = 0,
            onClick = {
                onEditClick()
                popupDismissRequester.dismiss()
            },
        ) {
            Text("Edit")
        }
    }
}

internal class Message(
    val text: AnnotatedString,
    val isSelf: Boolean
)