package any.ui.home.collections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Checkbox
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import any.base.R
import any.base.prefs.FolderViewType
import any.base.prefs.forcedFolderViewTypeFlow
import any.base.prefs.preferencesStore

@Composable
internal fun FolderViewTypes(
    currentViewType: FolderViewType,
    onSelectViewType: (type: FolderViewType, applyToAllFolders: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val preferencesStore = LocalContext.current.preferencesStore()

    var applyToAllFolders by remember { mutableStateOf(false) }

    LaunchedEffect(preferencesStore) {
        preferencesStore.forcedFolderViewTypeFlow()
            .collect { applyToAllFolders = it != null }
    }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectViewType(currentViewType, !applyToAllFolders) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = applyToAllFolders,
                    onCheckedChange = { onSelectViewType(currentViewType, it) },
                )

                Text(stringResource(R.string.apply_to_all_folders))
            }
        }

        items(FolderViewType.values()) { viewType ->
            val isSelected = viewType == currentViewType
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!isSelected) {
                            onSelectViewType(viewType, applyToAllFolders)
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            onSelectViewType(viewType, applyToAllFolders)
                        }
                    },
                )

                Spacer(modifier = Modifier.width(8.dp))

                val title = when (viewType) {
                    FolderViewType.Grid -> {
                        stringResource(R.string.grid)
                    }
                    FolderViewType.Card -> {
                        stringResource(R.string.card)
                    }
                    FolderViewType.List -> {
                        stringResource(R.string.list)
                    }
                    FolderViewType.FullWidth -> {
                        stringResource(R.string.full_width)
                    }
                }
                Text(text = title)
            }
        }
    }
}