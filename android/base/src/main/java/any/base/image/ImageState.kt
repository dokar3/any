package any.base.image

import androidx.compose.ui.unit.IntSize

sealed class ImageState {
    class Loading(val size: IntSize? = null) : ImageState()

    class Success(val size: IntSize) : ImageState()

    class Failure(val error: Throwable?) : ImageState()
}
