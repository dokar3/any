package any.base

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import any.base.prefs.DarkModeEnabledFlowUpdater

abstract class DarkModeAwareActivity : ComponentActivity() {
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        DarkModeEnabledFlowUpdater.update()
    }
}