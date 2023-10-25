package any.base.prefs

import kotlinx.coroutines.flow.MutableStateFlow

object InMemorySettings {
    val isFrameRateMonitoringEnabled = MutableStateFlow(false)
}