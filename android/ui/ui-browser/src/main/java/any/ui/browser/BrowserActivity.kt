package any.ui.browser

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.webkit.CookieManager
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.get
import any.base.DarkModeAwareActivity
import any.base.prefs.darkModeEnabledFlow
import any.base.prefs.darkModePrimaryColor
import any.base.prefs.preferencesStore
import any.base.prefs.primaryColor
import any.base.util.applyLightStatusBar
import any.base.util.clearLightStatusBar
import any.ui.common.theme.AnyTheme

class BrowserActivity : DarkModeAwareActivity() {
    private var hasSetResult = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent?.getStringExtra(EXTRA_URL) ?: intent?.data?.toString()
        val title = intent?.getStringExtra(EXTRA_TITLE)
        val userAgent = intent?.getStringExtra(EXTRA_USER_AGENT)

        var shouldCreateWebView by mutableStateOf(false)

        setContent {
            val scope = rememberCoroutineScope()

            val window = (LocalContext.current as Activity).window

            val view = LocalView.current

            val preferencesStore = LocalContext.current.preferencesStore()

            val isDark by preferencesStore.darkModeEnabledFlow(LocalContext.current, scope)
                .collectAsState()
            val primaryColor by preferencesStore.primaryColor
                .asStateFlow(scope)
                .collectAsState()
            val darkModePrimaryColor by preferencesStore.darkModePrimaryColor
                .asStateFlow(scope)
                .collectAsState()

            AnyTheme(
                darkTheme = isDark,
                primaryColor = primaryColor,
                darkModePrimaryColor = darkModePrimaryColor,
            ) {
                Surface {
                    val backgroundColor = MaterialTheme.colors.background

                    LaunchedEffect(view, isDark) {
                        if (isDark) {
                            view.clearLightStatusBar(window)
                        } else {
                            view.applyLightStatusBar(window)
                        }
                        window.statusBarColor = backgroundColor.toArgb()
                    }

                    BrowserScreen(
                        createWebView = shouldCreateWebView,
                        url = url,
                        title = title,
                        userAgent = userAgent,
                        forceDark = isDark,
                        onBackClick = { finish() },
                    )
                }
            }
        }

        val composeView = findViewById<ViewGroup>(android.R.id.content)[0]
        composeView.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                composeView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                shouldCreateWebView = true
            }
        })
    }

    override fun finish() {
        val cookiesTargetUrl = intent?.getStringExtra(EXTRA_COOKIES_TARGET_URL)
        if (!cookiesTargetUrl.isNullOrEmpty()) {
            setCookiesAsResult(cookiesTargetUrl)
        }
        super.finish()
    }

    private fun setCookiesAsResult(url: String?) {
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie(url)
        val data = Intent().apply {
            putExtra(EXTRA_RESULT_COOKIES, cookies)
        }
        setResult(REQUEST_CODE_COOKIES, data)
        hasSetResult = true
    }

    companion object {
        const val EXTRA_URL = "extra.url"
        const val EXTRA_TITLE = "extra.title"
        const val EXTRA_USER_AGENT = "extra.ua"
        const val EXTRA_COOKIES_TARGET_URL = "extra.cookies_target_url"

        const val EXTRA_RESULT_COOKIES = "extra.result_cookies"

        const val REQUEST_CODE_COOKIES = 100
    }
}
