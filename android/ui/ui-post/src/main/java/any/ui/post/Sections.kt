package any.ui.post

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import any.base.ImmutableHolder
import any.domain.post.ContentSection
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.widget.EmojiEmptyContent

@Composable
fun Sections(
    sections: ImmutableHolder<List<ContentSection>>,
    onSectionClick: (index: Int, section: ContentSection) -> Unit,
    modifier: Modifier = Modifier,
    stackListFromBottom: Boolean = false,
) {
    val listHolder = remember(sections, stackListFromBottom) {
        if (stackListFromBottom) ImmutableHolder(sections.value.reversed()) else sections
    }
    if (listHolder.value.isNotEmpty()) {
        SectionList(
            sections = listHolder,
            onChapterClick = { index, section ->
                onSectionClick(index, section)
            },
            modifier = modifier,
            reverseLayout = stackListFromBottom,
        )
    } else {
        EmojiEmptyContent(modifier = Modifier.fillMaxSize()) {
            Text(stringResource(BaseR.string.no_sections))
        }
    }
}


@Composable
private fun SectionList(
    sections: ImmutableHolder<List<ContentSection>>,
    onChapterClick: (index: Int, chapter: ContentSection) -> Unit,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
) {
    val state = rememberLazyListState()
    LazyColumn(
        state = state,
        modifier = modifier
            .fillMaxWidth()
            .verticalScrollBar(state),
        reverseLayout = reverseLayout,
    ) {
        itemsIndexed(sections.value) { index, section ->
            SectionItem(
                name = section.name,
                depth = section.depth,
                isStart = section.isStart,
                isEnd = section.isEnd,
                onClick = { onChapterClick(index, section) },
            )
        }
    }
}

@Composable
private fun SectionItem(
    name: String,
    depth: Int,
    isStart: Boolean,
    isEnd: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gapWidth: Dp = 16.dp,
    gapDotRadius: Dp = 2.dp,
    gapDotColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawWithCache {
                val gapWidthPx = gapWidth.toPx()
                val gapDotRadiusPx = gapDotRadius.toPx()
                val offsetX = 16.dp.toPx()
                onDrawBehind {
                    for (i in 0 until depth) {
                        val cx = offsetX + gapWidthPx * i + gapDotRadiusPx * 2
                        drawCircle(
                            color = gapDotColor,
                            radius = gapDotRadiusPx,
                            center = center.copy(x = cx),
                        )
                    }
                }
            }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(
                start = 8.dp + gapWidth * depth,
                top = 8.dp,
                end = 8.dp,
                bottom = 8.dp
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val icon: Int
            val text: String
            if (isStart) {
                icon = CommonUiR.drawable.ic_baseline_vertical_align_top_24
                text = stringResource(BaseR.string.the_start)
            } else if (isEnd) {
                icon = CommonUiR.drawable.ic_baseline_vertical_align_bottom_24
                text = stringResource(BaseR.string.the_end)
            } else {
                icon = 0
                text = name
            }

            if (icon != 0) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                )

                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = text,
                color = MaterialTheme.colors.onSurface,
                fontWeight = if (depth == 0) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}