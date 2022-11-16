package any.ui.imagepager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

open class ImagePagerViewModel : ViewModel() {
    private val positionUpdaters = mutableSetOf<ImagePagerPositionController>()

    var images: List<String>? = null

    private val _scrollPosition = MutableStateFlow(-1)

    init {
        viewModelScope.launch {
            _scrollPosition.collect { position ->
                positionUpdaters.forEach { it.update(position) }
            }
        }
    }

    fun updateScrollPosition(pos: Int) {
        _scrollPosition.update { pos }
    }

    fun attachUpdater(updater: ImagePagerPositionController) {
        positionUpdaters.add(updater)
    }

    fun detachUpdater(updater: ImagePagerPositionController) {
        positionUpdaters.remove(updater)
    }

    override fun onCleared() {
        super.onCleared()
        positionUpdaters.clear()
    }
}