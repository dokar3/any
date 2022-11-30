package any.ui.home.downloads

import any.base.R as BaseR
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import any.base.ImmutableHolder
import any.ui.home.HomeScreenDefaults
import any.ui.home.SelectionChip
import any.ui.home.downloads.viewmodel.DownloadType

@Composable
internal fun TypeSelection(
    onSelect: (DownloadType) -> Unit,
    types: ImmutableHolder<List<DownloadType>>,
    selectedType: DownloadType,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = HomeScreenDefaults.ListItemPadding,
    ) {
        items(items = types.value) {
            DownloadTypeItem(
                onSelect = { onSelect(it) },
                type = it,
                isSelected = selectedType == it,
            )
        }
    }
}

@Composable
private fun DownloadTypeItem(
    onSelect: () -> Unit,
    type: DownloadType,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val textRes = when (type) {
        is DownloadType.All -> BaseR.string.all
        is DownloadType.Downloading -> BaseR.string.downloading
        is DownloadType.Downloaded -> BaseR.string.downloaded
    }
    SelectionChip(
        isSelected = isSelected,
        onClick = {
            if (!isSelected) {
                onSelect()
            }
        },
        modifier = modifier,
    ) {
        Text(
            text = "${stringResource(textRes)} (${type.count})",
            maxLines = 1,
        )
    }
}