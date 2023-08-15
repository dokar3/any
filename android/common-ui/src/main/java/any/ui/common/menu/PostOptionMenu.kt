package any.ui.common.menu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import any.base.util.ClipboardUtil
import any.base.util.Intents
import any.domain.entity.UiPost
import com.dokar.sheets.BottomSheet
import com.dokar.sheets.BottomSheetValue
import com.dokar.sheets.rememberBottomSheetState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import any.base.R as BaseR
import any.ui.common.R as CommonUiR

@Composable
fun PostOptionMenu(
    post: UiPost,
    showMultiSelectionItem: Boolean,
    onDiscardRequest: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onAddToCollectionsClick: () -> Unit,
    onAddToFolderClick: (() -> Unit)? = null,
    onMultiSelectionClick: (() -> Unit)? = null,
) {
    val isCollected = post.isCollected()

    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val state = rememberBottomSheetState()

    LaunchedEffect(post) {
        state.expand()
    }

    LaunchedEffect(state) {
        snapshotFlow { state.value }
            .distinctUntilChanged()
            .filter { it == BottomSheetValue.Collapsed }
            .drop(1)
            .collect { onDismiss() }
    }

    BottomSheet(
        state = state,
        modifier = modifier,
        skipPeeked = true,
    ) {
        val res = LocalContext.current.resources
        val normalColor = MaterialTheme.colors.onSurface
        val errorColor = MaterialTheme.colors.error
        val headerTitle = stringResource(BaseR.string.post)
        LazyColumn {
            headerItem(title = headerTitle, subTitle = post.title)

            item {
                val collectText: String
                val collectIcon: Int
                val tint: Color
                if (isCollected) {
                    collectText = stringResource(BaseR.string.remove_from_collections)
                    collectIcon = CommonUiR.drawable.ic_baseline_delete_outline_24
                    tint = errorColor
                } else {
                    collectText = stringResource(BaseR.string.add_to_collections)
                    collectIcon = CommonUiR.drawable.ic_baseline_add_24
                    tint = normalColor
                }
                var preparingToDiscard = false

                var title by remember(collectText) { mutableStateOf(collectText) }

                val collectIconRotation = remember { Animatable(0f) }

                MenuItem(
                    icon = {
                        Icon(
                            painter = painterResource(collectIcon),
                            contentDescription = null,
                            modifier = Modifier.rotate(collectIconRotation.value),
                            tint = tint,
                        )
                    },
                    onClick = {
                        scope.launch {
                            if (isCollected) {
                                if (preparingToDiscard) {
                                    onDiscardRequest()
                                    state.collapse()
                                } else {
                                    preparingToDiscard = true
                                    val rotationAnim = scope.launch {
                                        collectIconRotation.animateTo(
                                            targetValue = 0f,
                                            animationSpec = shakingTween(),
                                        )
                                    }
                                    title = res.getString(BaseR.string.sure_to_remove)
                                    delay(4500L)
                                    title = collectText
                                    rotationAnim.cancel()
                                    collectIconRotation.animateTo(0f)
                                    preparingToDiscard = false
                                }
                            } else {
                                onAddToCollectionsClick()
                                state.collapse()
                            }
                        }
                    },
                ) {
                    Text(title)
                }
            }

            if (showMultiSelectionItem) {
                item {
                    MenuItem(
                        icon = {
                            Icon(
                                painter = painterResource(
                                    CommonUiR.drawable.ic_baseline_checklist_24
                                ),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            scope.launch {
                                state.collapse()
                                onMultiSelectionClick?.invoke()
                            }
                        }
                    ) {
                        Text("Select")
                    }
                }
            }

            if (post.isCollected()) {
                item {
                    MenuItem(
                        icon = {
                            Icon(
                                painter = painterResource(
                                    CommonUiR.drawable.ic_outline_create_new_folder_24
                                ),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            scope.launch {
                                state.collapse()
                                onAddToFolderClick?.invoke()
                            }
                        }
                    ) {
                        Text(stringResource(BaseR.string.add_to_folder))
                    }
                }
            }

            item {
                MenuItem(
                    icon = {
                        Icon(
                            painter = painterResource(
                                CommonUiR.drawable.ic_baseline_open_in_browser_24
                            ),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        Intents.openInBrowser(context, post.url)
                        scope.launch { state.collapse() }
                    }
                ) {
                    Text(stringResource(BaseR.string.open_in_browser))
                }
            }

            item {
                MenuItem(
                    icon = {
                        Icon(
                            painter = painterResource(
                                CommonUiR.drawable.ic_baseline_link_24
                            ),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        ClipboardUtil.copyText(context, post.url)
                        scope.launch { state.collapse() }
                    }
                ) {
                    Text(stringResource(BaseR.string.copy_url))
                }
            }

            cancelItem(
                text = res.getString(android.R.string.cancel),
                onClick = {
                    scope.launch { state.collapse() }
                }
            )
        }
    }
}

private fun shakingTween(
    duration: Int = 1500,
): AnimationSpec<Float> {
    return infiniteRepeatable(
        animation = keyframes {
            durationMillis = duration
            -30f at 70
            0f at 140
            35f at 200
            0f at 280
            -25f at 360
            0f at 460
            15f at 540
            0f at 640
            -5f at 760
            0f at 860
        }
    )
}
