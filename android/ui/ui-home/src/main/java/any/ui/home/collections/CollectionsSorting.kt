package any.ui.home.collections

import any.base.R as BaseR
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import any.base.model.PostSorting

@Composable
internal fun CollectionsSorting(
    currentSorting: PostSorting,
    onSelectSorting: (PostSorting) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(PostSorting.values()) { sorting ->
            val isSelected = sorting == currentSorting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!isSelected) {
                            onSelectSorting(sorting)
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            onSelectSorting(sorting)
                        }
                    },
                )

                Spacer(modifier = Modifier.width(8.dp))

                val title = when (sorting) {
                    PostSorting.ByAddTime -> {
                        stringResource(BaseR.string.by_add_time)
                    }
                    PostSorting.ByRecentBrowsing -> {
                        stringResource(BaseR.string.by_recent_browsing)
                    }
                    PostSorting.ByTitle -> {
                        stringResource(BaseR.string.by_title)
                    }
                }
                Text(text = title)
            }
        }
    }
}