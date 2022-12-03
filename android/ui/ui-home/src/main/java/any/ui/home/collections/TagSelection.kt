package any.ui.home.collections

import any.base.R as BaseR
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import any.base.compose.StableHolder
import any.ui.common.modifier.fadingEdge
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.widget.SimpleDialog
import any.ui.home.HomeScreenDefaults
import any.ui.home.SelectionChip
import any.ui.home.collections.viewmodel.SelectableTag

@Composable
internal fun TagSelection(
    tags: StableHolder<List<SelectableTag>>,
    onShowAllTagsClick: () -> Unit,
    onUpdateTags: (List<SelectableTag>) -> Unit,
    onRemoveTagFromPosts: (SelectableTag) -> Unit,
    modifier: Modifier = Modifier,
    tagsLayout: TagsLayout = TagsLayout.Row,
) {
    var selectedTag: SelectableTag? by remember { mutableStateOf(null) }

    val mutableTags = remember(tags) {
        tags.value.toMutableList()
    }

    val onTagClick = remember(mutableTags) {
        click@{ index: Int, tag: SelectableTag ->
            if (tag.isSelected) {
                val selectedTagCount = mutableTags.count { it.isSelected }
                if (selectedTagCount == 1) {
                    return@click
                }
                // unselect 'All'
                mutableTags[0] = mutableTags[0].copy(isSelected = false)
                mutableTags[index] = tag.copy(isSelected = false)
            } else {
                if (index == 0) {
                    // select tag 'all'
                    mutableTags.forEachIndexed { idx, t ->
                        mutableTags[idx] = t.copy(isSelected = false)
                    }
                    mutableTags[0] = mutableTags[0].copy(isSelected = true)
                } else {
                    // select a normal tag
                    mutableTags[0] = mutableTags[0].copy(isSelected = false)
                    mutableTags[index] = tag.copy(isSelected = true)
                }
            }
            onUpdateTags(mutableTags)
        }
    }

    val onTagLongClick: (SelectableTag) -> Unit = remember {
        { selectedTag = it }
    }

    when (tagsLayout) {
        TagsLayout.Row -> {
            RowTags(
                tags = mutableTags,
                modifier = modifier,
                onTagClick = onTagClick,
                onTagLongClick = onTagLongClick,
                onShowAllTagsClick = onShowAllTagsClick
            )
        }

        TagsLayout.Flow -> {
            FlowTags(
                tags = mutableTags,
                modifier = modifier,
                onTagClick = onTagClick,
                onTagLongClick = onTagLongClick,
            )
        }
    }

    if (selectedTag != null) {
        val tag = selectedTag!!
        SimpleDialog(
            onDismissRequest = { selectedTag = null },
            title = {
                Text(stringResource(BaseR.string._remove_tag, tag.name))
            },
            text = {
                val message = stringResource(BaseR.string.remove_tag_alert)
                Text(message)
            },
            confirmText = {
                Text(stringResource(android.R.string.ok), color = MaterialTheme.colors.error)
            },
            onConfirmClick = {
                onRemoveTagFromPosts(tag)
            },
            cancelText = {
                Text(stringResource(android.R.string.cancel))
            }
        )
    }
}

@Composable
private fun FlowTags(
    tags: List<SelectableTag>,
    modifier: Modifier = Modifier,
    onTagClick: (index: Int, tag: SelectableTag) -> Unit,
    onTagLongClick: ((SelectableTag) -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .verticalScrollBar(listState),
        state = listState,
    ) {
        itemsIndexed(tags) { index, tag ->
            TagItem(
                tag = tag,
                onClick = { onTagClick(index, tag) },
                onLongClick = { onTagLongClick?.invoke(tag) },
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun RowTags(
    tags: List<SelectableTag>,
    modifier: Modifier = Modifier,
    onTagClick: (index: Int, tag: SelectableTag) -> Unit,
    onTagLongClick: ((SelectableTag) -> Unit)? = null,
    onShowAllTagsClick: () -> Unit
) {
    Box(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .fadingEdge(endEdgeLength = 96.dp),
            contentPadding = HomeScreenDefaults.ListItemPadding,
        ) {
            itemsIndexed(items = tags) { index, tag ->
                TagItem(
                    tag = tag,
                    onClick = { onTagClick(index, tag) },
                    onLongClick = { onTagLongClick?.invoke(tag) },
                )
            }
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.BottomEnd)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(radius = 24.dp),
                    onClick = onShowAllTagsClick
                ),
            contentAlignment = Alignment.Center
        ) {
            val icon = rememberVectorPainter(Icons.Filled.MoreVert)
            Icon(painter = icon, contentDescription = stringResource(BaseR.string.more))
        }
    }
}

@Composable
internal fun TagItem(
    tag: SelectableTag,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    SelectionChip(
        isSelected = tag.isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
    ) {
        Text("${tag.name} (${tag.count})", maxLines = 1)
    }
}

internal enum class TagsLayout {
    Row,
    Flow
}
