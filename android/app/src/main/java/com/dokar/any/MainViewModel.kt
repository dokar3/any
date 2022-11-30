package com.dokar.any

import androidx.lifecycle.ViewModel
import any.navigation.NavEvent
import any.ui.readingbubble.entity.ReadingPost
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    private val _navEvent = MutableSharedFlow<NavEvent>(extraBufferCapacity = 1)
    val navEvent: Flow<NavEvent> = _navEvent

    fun setServiceManifestUrlToAdd(url: String?) {
        _uiState.update { it.copy(serviceManifestUrlToAdd = url) }
    }

    fun setReadingPostToNavigate(post: ReadingPost?) {
        _uiState.update { it.copy(readingPostToNavigate = post) }
    }

    fun setShortcutsDestination(destination: ShortcutsDestination?) {
        _uiState.update { it.copy(shortcutsDestination = destination) }
    }

    fun setServiceIdToConfigure(serviceId: String?) {
        _uiState.update { it.copy(serviceIdToConfigure = serviceId) }
    }

    fun sendNavEvent(navEvent: NavEvent) {
        _navEvent.tryEmit(navEvent)
    }
}