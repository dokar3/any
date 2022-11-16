package any.ui.post.menu

import any.base.R as BaseR
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.lifecycleScope
import any.base.StableHolder
import any.base.image.PostImageSaver
import any.base.util.Intents
import any.base.util.PackageUtil
import any.download.PostImageDownloader
import any.ui.common.R
import any.ui.common.widget.AnimatedPopup
import any.ui.common.widget.AnimatedPopupItem
import any.ui.common.widget.rememberAnimatedPopupDismissRequester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

internal val addBookmarkItem by lazy {
    PostMenuItem(
        icon = R.drawable.ic_baseline_bookmark_border_24,
        title = BaseR.string.add_bookmark,
    )
}

private val saveImageItem by lazy {
    PostMenuItem(
        icon = R.drawable.ic_baseline_save_alt_24,
        title = BaseR.string.save_image,
    )
}

private val copyImageUrlItem by lazy {
    PostMenuItem(
        icon = R.drawable.ic_baseline_link_24,
        title = BaseR.string.copy_image_url,
    )
}

private val glensItem by lazy {
    PostMenuItem(
        icon = R.drawable.ic_glens,
        title = BaseR.string.google_lens,
    )
}

private val shareImageItem by lazy {
    PostMenuItem(
        icon = R.drawable.ic_baseline_share_24,
        title = BaseR.string.share_image,
    )
}

@Composable
internal fun PostImageOptionsPopup(
    onDismissRequest: () -> Unit,
    onAddToBookmarkClick: () -> Unit,
    onPrepareToShare: () -> Unit,
    onReadyToShare: () -> Unit,
    postTitle: String?,
    selectedImage: String,
    contentImages: StableHolder<List<String>>,
    isReversed: Boolean,
    isShareEnabled: Boolean,
    offset: DpOffset,
    modifier: Modifier = Modifier,
) {
    val scope = LocalLifecycleOwner.current.lifecycleScope

    val images = contentImages.value
    val imageIndex = images.indexOf(selectedImage)
    val page = if (isReversed) {
        images.size - imageIndex - 1
    } else {
        imageIndex
    }

    val context = LocalContext.current

    val popupDismissRequester = rememberAnimatedPopupDismissRequester()

    val clipboardManager = LocalClipboardManager.current

    val isGoogleLensInstalled = remember {
        PackageUtil.isPackageInstalled(
            packageName = PackageUtil.PKG_GOOGLE_LENS,
            context = context,
        )
    }
    val items = remember(imageIndex) {
        buildList {
            if (imageIndex != -1) {
                add(addBookmarkItem)
            }
            add(saveImageItem)
            add(copyImageUrlItem)
            if (isGoogleLensInstalled) {
                add(glensItem)
            }
            add(shareImageItem)
        }
    }

    suspend fun onPopupItemClick(item: PostMenuItem) {
        popupDismissRequester.dismiss()
        when (item) {
            addBookmarkItem -> {
                onAddToBookmarkClick()
            }
            saveImageItem -> {
                val ret = withContext(Dispatchers.IO) {
                    PostImageSaver.saveToPicturesDir(
                        imageFetcher = PostImageDownloader.get(context),
                        postTitle = postTitle,
                        imageIndex = page,
                        url = selectedImage,
                    )
                }
                if (!coroutineContext.isActive) {
                    return
                }
                if (ret.isSuccess) {
                    Toast.makeText(
                        context,
                        BaseR.string.image_saved,
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        BaseR.string.save_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            copyImageUrlItem -> {
                clipboardManager.setText(AnnotatedString(selectedImage))
                Toast.makeText(context, BaseR.string.url_copied, Toast.LENGTH_SHORT).show()
            }
            glensItem,
            shareImageItem -> {
                val packageName = if (item == glensItem) {
                    PackageUtil.PKG_GOOGLE_LENS
                } else {
                    null
                }
                if (isShareEnabled) {
                    onPrepareToShare()
                    Intents.shareImage(
                        context = context,
                        imageFetcher = PostImageDownloader.get(context),
                        url = selectedImage,
                        packageName = packageName,
                    )
                    onReadyToShare()
                }
            }
            else -> {}
        }
    }

    AnimatedPopup(
        dismissRequester = popupDismissRequester,
        onDismissed = onDismissRequest,
        modifier = modifier,
        offset = offset,
        properties = PopupProperties(focusable = true),
        scaleAnimOrigin = TransformOrigin(0.5f, 0f),
    ) {
        AnimatedPopupItem(
            index = 0,
            onClick = null,
        ) {
            Text(
                text = stringResource(BaseR.string._image_with_index, page + 1),
                color = MaterialTheme.colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }

        for (i in items.indices) {
            val item = items[i]
            AnimatedPopupItem(
                index = i + 1,
                onClick = { scope.launch { onPopupItemClick(item) } },
                contentPadding = PaddingValues(
                    start = 0.dp,
                    top = 10.dp,
                    end = 16.dp,
                    bottom = 10.dp
                ),
            ) {
                PostPopupMenuItem(item = item)
            }
        }
    }
}
