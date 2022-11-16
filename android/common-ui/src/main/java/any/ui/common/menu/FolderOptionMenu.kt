package any.ui.common.menu

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.dokar.sheets.BottomSheet
import com.dokar.sheets.BottomSheetValue
import com.dokar.sheets.PeekHeight
import com.dokar.sheets.rememberBottomSheetState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Composable
fun FolderOptionMenu(
    folderName: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onUnfoldClick: (() -> Unit)? = null,
    onRenameClick: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()

    val state = rememberBottomSheetState()

    LaunchedEffect(state) {
        state.expand()
    }

    LaunchedEffect(state) {
        snapshotFlow { state.value }
            .distinctUntilChanged()
            .filter { it == BottomSheetValue.Collapsed }
            .collect {
                onDismissRequest()
            }
    }

    BottomSheet(
        state = state,
        modifier = modifier,
        peekHeight = PeekHeight.fraction(1f),
    ) {
        val res = LocalContext.current.resources
        LazyColumn {
            headerItem(
                title = res.getString(BaseR.string.folder),
                subTitle = folderName,
            )


            item {
                MenuItem(
                    onClick = {
                        scope.launch {
                            state.collapse()
                            onUnfoldClick?.invoke()
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(CommonUiR.drawable.ic_baseline_undo_24),
                            contentDescription = null,
                        )
                    },
                ) {
                    Text(stringResource(BaseR.string.unfold))
                }
            }

            item {
                MenuItem(
                    onClick = {
                        scope.launch {
                            state.collapse()
                            onRenameClick?.invoke()
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(CommonUiR.drawable.ic_outline_space_bar_24),
                            contentDescription = null,
                        )
                    },
                ) {
                    Text(stringResource(BaseR.string.rename))
                }
            }

            cancelItem(
                text = res.getString(android.R.string.cancel),
                onClick = {
                    scope.launch {
                        state.collapse()
                    }
                }
            )
        }
    }
}