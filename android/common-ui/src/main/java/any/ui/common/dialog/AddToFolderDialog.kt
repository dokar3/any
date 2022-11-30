package any.ui.common.dialog

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import any.data.entity.Folder
import any.data.entity.HierarchicalFolder
import any.domain.entity.UiPost
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.theme.secondaryText
import any.ui.common.widget.BasicDialog

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AddToFolderDialog(
    onDismissRequest: () -> Unit,
    onFolderConfirm: (Folder) -> Unit,
    post: UiPost?,
    modifier: Modifier = Modifier,
    viewModel: AddToFolderViewModel = viewModel(
        factory = AddToFolderViewModel.Factory(LocalContext.current)
    ),
) {
    val uiState by viewModel.addToFolderUiState.collectAsState()

    var showNewFolder by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.loadAllFolder()
    }

    LaunchedEffect(post) {
        if (post != null) {
            viewModel.selectFolderByPath(post.folder)
        }
    }

    DisposableEffect(viewModel) {
        onDispose { viewModel.reset() }
    }

    BasicDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.heightIn(max = 560.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(BaseR.string.add_to_folder),
                    modifier = Modifier.weight(1f),
                )
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp)
            ) {
                if (showNewFolder) {
                    val keyboardController = LocalSoftwareKeyboardController.current
                    val focusRequester = remember { FocusRequester() }
                    LaunchedEffect(keyboardController) {
                        keyboardController?.show()
                        focusRequester.requestFocus()
                    }
                    NewFolderTextField(
                        parentFolder = uiState.selectedFolder,
                        onSubmit = {
                            viewModel.newFolder(it)
                            showNewFolder = false
                        },
                        onDismissRequest = {
                            showNewFolder = false
                            keyboardController?.hide()
                            focusRequester.freeFocus()
                        },
                        modifier = Modifier
                            .animateContentSize()
                            .focusRequester(focusRequester),
                    )

                    Spacer(modifier = Modifier.heightIn(8.dp))
                }

                FolderList(
                    selectedFolder = uiState.selectedFolder,
                    folders = uiState.flattedFolders,
                    onSelect = {
                        val selected = uiState.selectedFolder == it
                        viewModel.selectFolder(it)
                        if (selected || !it.expanded) {
                            viewModel.toggleFolder(it)
                        }
                    },
                )
            }
        },
        neutralText = { Text(stringResource(BaseR.string.new_folder)) },
        confirmText = { Text(stringResource(android.R.string.ok)) },
        cancelText = { Text(stringResource(android.R.string.cancel)) },
        onNeutralClick = { showNewFolder = true },
        onConfirmClick = { onFolderConfirm(uiState.selectedFolder.toFolder()) },
        dismissOnNeutral = false,
    )
}

@Composable
private fun NewFolderTextField(
    parentFolder: HierarchicalFolder,
    onSubmit: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var value by remember(parentFolder) {
        val name = if (parentFolder != HierarchicalFolder.ROOT) {
            "${parentFolder.path}/"
        } else {
            ""
        }
        mutableStateOf(
            TextFieldValue(text = name, selection = TextRange(name.length))
        )
    }
    OutlinedTextField(
        value = value,
        onValueChange = {
            value = it
        },
        modifier = modifier.fillMaxWidth(),
        label = {
            Text(stringResource(BaseR.string.new_folder))
        },
        leadingIcon = {
            IconButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(BaseR.string.hide_new_folder_input),
                )
            }
        },
        trailingIcon = {
            IconButton(
                onClick = {
                    if (value.text.isNotEmpty()) {
                        onSubmit(value.text)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(BaseR.string.new_folder),
                )
            }
        },
        singleLine = true,
        maxLines = 1,
    )
}

@Composable
private fun FolderList(
    selectedFolder: HierarchicalFolder,
    folders: List<HierarchicalFolder>,
    onSelect: (HierarchicalFolder) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .verticalScrollBar(listState),
        state = listState,
    ) {
        items(
            items = folders,
            key = { it.path },
        ) {
            val isSelected = it == selectedFolder
            FolderItem(
                folder = it,
                isSelected = isSelected,
                onClick = {
                    onSelect(it)
                },
            )
        }
    }
}

@Composable
private fun FolderItem(
    folder: HierarchicalFolder,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (isSelected) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.secondaryText
    }
    val depthPadding = 16.dp * folder.depth.coerceIn(0, 10)
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(),
                onClick = onClick,
            )
            .padding(
                start = 8.dp + depthPadding,
                top = 8.dp,
                end = 8.dp,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(CommonUiR.drawable.ic_baseline_folder_24),
            contentDescription = stringResource(BaseR.string.folder_icon),
            tint = tint,
        )

        Spacer(modifier = Modifier.width(8.dp))

        val name = if (folder == HierarchicalFolder.ROOT) {
            stringResource(BaseR.string.root_folder)
        } else {
            folder.name
        }
        Text(
            text = name,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(8.dp))

        RadioButton(
            selected = isSelected,
            onClick = null,
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
            interactionSource = interactionSource,
        )
    }
}
