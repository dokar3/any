package any.ui.imagepager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
interface ImageGestureHandler {
    /**
     * Left part of image was clicked
     *
     * @return handled
     */
    fun onLeftTap(): Boolean

    /**
     * Center part of image was clicked
     *
     * @return handled
     */
    fun onCenterTap(): Boolean

    /**
     * Right part of image was clicked
     *
     * @return handled
     */
    fun onRightTap(): Boolean
}

@Composable
fun ImagePager(
    onBack: () -> Unit,
    currentIndexUpdater: (Int) -> Unit,
    title: String?,
    images: List<String>,
    initialPage: Int = 0,
) {
        FragmentImagePager(
            onBack = onBack,
            currentIndexUpdater = currentIndexUpdater,
            title = title,
            images = images,
            initialPage = initialPage,
        )
}
