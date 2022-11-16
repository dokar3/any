package any.ui.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslCertificate
import android.os.Build
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun WebView(
    createWebView: Boolean,
    url: String,
    userAgent: String?,
    forceDark: Boolean,
    onTitleUpdated: (String?) -> Unit,
    onPageStarted: ((String?) -> Unit)?,
    onPageLoaded: ((String?) -> Unit)?,
    modifier: Modifier = Modifier,
    controller: WebViewController = WebViewController(),
) {
    val context = LocalContext.current

    var loadingProgress by remember { mutableStateOf(0f) }

    var webView by remember { mutableStateOf<WebView?>(null) }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    LaunchedEffect(forceDark) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            webView?.settings?.forceDark = if (forceDark) {
                WebSettings.FORCE_DARK_ON
            } else {
                WebSettings.FORCE_DARK_OFF
            }
        }
    }

    val wv = webView
    DisposableEffect(wv) {
        controller.webView = wv
        onDispose {
            wv?.destroy()
            controller.webView = null
        }
    }

    Box(modifier = modifier) {
        if (createWebView) {
            AndroidView(
                factory = {
                    createWebView(
                        context = context,
                        url = url,
                        userAgent = userAgent,
                        forceDark = forceDark,
                        onTitleUpdated = onTitleUpdated,
                        onProgressUpdate = { view, progress ->
                            loadingProgress = progress / 100f
                            if (progress == 0 && !controller.isLoading) {
                                controller.isLoading = true
                                onPageStarted?.invoke(view.url)
                            } else if (progress == 100 && controller.isLoading) {
                                controller.isLoading = false
                                onPageLoaded?.invoke(view.url)
                            }
                        }
                    ).also {
                        webView = it
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (loadingProgress != 1f) {
            LinearProgressIndicator(
                progress = loadingProgress,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(
    context: Context,
    url: String,
    userAgent: String?,
    forceDark: Boolean,
    onTitleUpdated: (String?) -> Unit,
    onProgressUpdate: (WebView, Int) -> Unit,
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        with(settings) {
            javaScriptEnabled = true

            if (forceDark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                this.forceDark = WebSettings.FORCE_DARK_ON
            }

            if (!userAgent.isNullOrEmpty()) {
                userAgentString = userAgent
            }
        }

        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                onProgressUpdate(view!!, 0)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                onProgressUpdate(view!!, 100)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                onProgressUpdate(view!!, 0)
                return false
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                onProgressUpdate(view!!, newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                onTitleUpdated(title)
            }
        }

        loadUrl(url)
    }
}

@Stable
class WebViewController {
    internal var webView: WebView? = null

    var isLoading: Boolean by mutableStateOf(false)
        internal set

    fun load(url: String) {
        webView?.loadUrl(url)
    }

    fun reload() {
        webView?.reload()
    }

    fun stopLoading() {
        webView?.stopLoading()
    }

    fun backward() {
        webView?.goBack()
    }

    fun forward() {
        webView?.goForward()
    }

    fun clearCache() {
        webView?.clearCache(true)
    }

    fun url(): String? {
        return webView?.url
    }

    fun certificate(): SslCertificate? {
        return webView?.certificate
    }
}

@Stable
internal sealed interface LoadState {
    object Loading : LoadState

    class Finished(
        val timeElapse: Long,
        val isSecure: Boolean,
    ) : LoadState
}
