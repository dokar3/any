package com.dokar.any

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import any.base.DarkModeAwareActivity
import any.base.image.ImageLoader
import any.base.log.Logger
import any.base.prefs.darkModeEnabledFlow
import any.base.prefs.darkModePrimaryColor
import any.base.prefs.isSecureScreenEnabled
import any.base.prefs.preferencesStore
import any.base.prefs.primaryColor
import any.base.util.Dirs
import any.base.util.FileUtil
import any.data.cache.ExoVideoCache
import any.navigation.Routes
import any.navigation.navPushEvent
import any.navigation.search
import any.navigation.userProfile
import any.ui.common.theme.AnyTheme
import any.ui.jslogger.FloatingLoggerService
import any.ui.readingbubble.ReadingBubbleService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MainActivity : DarkModeAwareActivity() {
    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        observeSecureScreen()

        handleIntent(intent)

        setContent {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            val preferencesStore = context.preferencesStore()
            val darkMode by preferencesStore.darkModeEnabledFlow(context, scope)
                .collectAsState()
            val primaryColor by preferencesStore.primaryColor
                .asStateFlow(scope)
                .collectAsState()
            val darkModePrimaryColor by preferencesStore.darkModePrimaryColor
                .asStateFlow(scope)
                .collectAsState()
            AnyTheme(
                darkTheme = darkMode,
                dynamicColors = true,
                primaryColor = primaryColor,
                darkModePrimaryColor = darkModePrimaryColor,
            ) {
                Surface(color = MaterialTheme.colors.background) {
                    MainScreen(
                        darkMode = darkMode,
                        mainViewModel = mainViewModel,
                        modifier = Modifier.semantics { contentDescription = "MainScreen" },
                    )
                }
            }
        }

        ReadingBubbleService.addNavigateListener(this) {
            mainViewModel.setReadingPostToNavigate(it)
            if (lifecycle.currentState < Lifecycle.State.STARTED) {
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        ImageLoader.trimMemory(level)
    }

    override fun onDestroy() {
        super.onDestroy()
        FloatingLoggerService.dismiss()
        ReadingBubbleService.dismiss()
        ReadingBubbleService.removeNavListener(this)
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            FileUtil.clearDirectory(Dirs.backupTempDir(this@MainActivity))
            FileUtil.clearDirectory(Dirs.servicesTempDir(this@MainActivity))
            ExoVideoCache.release()
        }
    }

    private fun observeSecureScreen() = lifecycleScope.launch {
        preferencesStore().isSecureScreenEnabled
            .asFlow()
            .distinctUntilChanged()
            .collect { enabled ->
                if (enabled) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.handleAppUrl()) return
        if (intent.handleShortcutsNavigation()) return
        if (intent.handleAddService()) return
    }

    private fun Intent.handleAppUrl(): Boolean {
        if (action != Intent.ACTION_VIEW) return false
        val uri = data ?: return false
        when (val host = uri.host) {
            APP_URL_HOST_USER -> {
                val serviceId = uri.getQueryParameter("serviceId") ?: return false
                val userId = uri.getQueryParameter("id") ?: return false
                val route = Routes.userProfile(serviceId = serviceId, userId = userId)
                mainViewModel.sendNavEvent(navPushEvent(route))
                return true
            }

            APP_URL_HOST_SEARCH -> {
                val serviceId = uri.getQueryParameter("serviceId") ?: return false
                val query = uri.getQueryParameter("query") ?: return false
                val route = Routes.search(serviceId = serviceId, query = query)
                mainViewModel.sendNavEvent(navPushEvent(route))
                return true
            }

            APP_URL_HOST_ACTIONS -> {
                when (val path = uri.lastPathSegment) {
                    APP_URL_ACTION_CONFIGURE_SERVICE -> {
                        val serviceId = uri.getQueryParameter("id") ?: return false
                        mainViewModel.setServiceIdToConfigure(serviceId)
                        return true
                    }

                    else -> {
                        Logger.e(
                            TAG,
                            "Unsupported app url path: $path, full url: $uri"
                        )
                    }
                }
            }

            else -> {
                Logger.e(TAG, "Unsupported app url host: $host")
            }
        }

        return false
    }

    private fun Intent.handleShortcutsNavigation(): Boolean {
        if (action != Intent.ACTION_VIEW) return false
        val targetDest = getStringExtra(EXTRA_TARGET_DEST) ?: return false
        mainViewModel.setShortcutsDestination(ShortcutsDestination.fromValue(targetDest))
        return true
    }

    private fun Intent.handleAddService(): Boolean {
        if (action != ACTION_ADD_SERVICE) return false
        val manifestUrl = intent.getStringExtra(EXTRA_SERVICE_MANIFEST_URL) ?: return false
        mainViewModel.setServiceManifestUrlToAdd(manifestUrl)
        return true
    }

    companion object {
        private const val TAG = "MainActivity"

        const val ACTION_ADD_SERVICE = "any.action.add_service"
        const val EXTRA_SERVICE_MANIFEST_URL = "extra.service_manifest_url"
        const val EXTRA_TARGET_DEST = "extra.target_destination"

        // anyapp://app.actions
        private const val APP_URL_HOST_ACTIONS = "app.actions"
        private const val APP_URL_ACTION_CONFIGURE_SERVICE = "configure_service"

        // anyapp://user
        private const val APP_URL_HOST_USER = "user"

        // anyapp://search
        private const val APP_URL_HOST_SEARCH = "search"
    }
}
