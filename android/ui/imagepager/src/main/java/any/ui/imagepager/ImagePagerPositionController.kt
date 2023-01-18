package any.ui.imagepager

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@Stable
class ImagePagerPositionController {
    private val _position = MutableStateFlow(-1)
    val position: StateFlow<Int> = _position

    fun update(position: Int) {
        _position.update { position }
    }
}
