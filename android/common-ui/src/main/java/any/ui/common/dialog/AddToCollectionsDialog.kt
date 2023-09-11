package any.ui.common.dialog

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import any.data.entity.Folder
import any.domain.entity.UiPost
import any.ui.common.theme.secondaryText
import any.ui.common.widget.BasicDialog
import com.dokar.chiptextfield.Chip
import com.dokar.chiptextfield.ChipTextFieldDefaults
import com.dokar.chiptextfield.ChipTextFieldState
import com.dokar.chiptextfield.OutlinedChipTextField
import com.dokar.chiptextfield.rememberChipTextFieldState
import kotlinx.coroutines.launch
import any.base.R as BaseR
import any.ui.common.R as CommonUiR

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun AddToCollectionsDialog(
    onCollect: suspend (post: UiPost) -> Unit,
    onDismissRequest: () -> Unit,
    post: UiPost,
) {
    val scope = rememberCoroutineScope()
    val chips = remember(post.tags) { post.tags?.map(::Chip) ?: emptyList() }
    var chipTextFieldValue by remember { mutableStateOf("") }
    val chipState = rememberChipTextFieldState(chips = chips)

    var folderPath: String by remember(post.folder) {
        mutableStateOf(post.folder ?: Folder.ROOT.path)
    }

    var showAddToFolderDialog by remember { mutableStateOf(false) }

    BasicDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(stringResource(BaseR.string.add_to_collections))
        },
        content = {
            DialogContent(
                post = post,
                chipState = chipState,
                chipTextFieldValue = chipTextFieldValue,
                onChipTextFieldValueChange = { chipTextFieldValue = it },
                folderName = folderPath,
                onFolderClick = { showAddToFolderDialog = true },
            )
        },
        confirmText = {
            Text(stringResource(BaseR.string.confirm))
        },
        cancelText = {
            Text(stringResource(android.R.string.cancel))
        },
        onConfirmClick = {
            scope.launch {
                val tags = chipState.chips
                    .map(Chip::text)
                    .toMutableList()
                    .also {
                        if (chipTextFieldValue.isNotEmpty()) {
                            it += chipTextFieldValue
                        }
                    }
                val updatedPost = post.copy(
                    tags = tags,
                    folder = folderPath,
                )
                onCollect(updatedPost)
                onDismissRequest()
            }
        },
        onCancelClick = {
            onDismissRequest()
        }
    )

    if (showAddToFolderDialog) {
        PostFolderSelectionDialog(
            onDismissRequest = { showAddToFolderDialog = false },
            onFolderSelected = { folderPath = it.path },
            initiallySelectedFolder = post.folder,
        )
    }
}

@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@Composable
private fun DialogContent(
    post: UiPost,
    chipState: ChipTextFieldState<Chip>,
    chipTextFieldValue: String,
    onChipTextFieldValueChange: (String) -> Unit,
    folderName: String,
    onFolderClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(BaseR.string.post),
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(post.title)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(BaseR.string.folder),
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(4.dp))

        val underlineColor = MaterialTheme.colors.secondaryText
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onFolderClick)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        topLeft = Offset(0f, size.height - 1.dp.toPx()),
                        size = Size(size.width, 1.dp.toPx()),
                        color = underlineColor,
                    )
                }
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(CommonUiR.drawable.ic_baseline_folder_24),
                contentDescription = stringResource(BaseR.string.select_folder),
                tint = MaterialTheme.colors.primary
            )

            Spacer(modifier = Modifier.width(8.dp))

            val name = if (folderName == Folder.ROOT.path) {
                stringResource(BaseR.string.root_folder)
            } else {
                folderName
            }
            Text(name)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(BaseR.string.tags),
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedChipTextField(
            state = chipState,
            value = chipTextFieldValue,
            onValueChange = onChipTextFieldValueChange,
            onSubmit = ::Chip,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            innerModifier = Modifier
                .heightIn(max = 120.dp)
                .verticalScroll(state = rememberScrollState()),
            chipStyle = ChipTextFieldDefaults.chipStyle(
                shape = MaterialTheme.shapes.large,
                focusedTextColor = MaterialTheme.colors.onPrimary,
                focusedBorderColor = MaterialTheme.colors.primary,
                cursorColor = MaterialTheme.colors.secondary,
                focusedBackgroundColor = MaterialTheme.colors.primary,
            )
        )
    }
}