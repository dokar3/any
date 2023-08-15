package any.ui.browser

import android.webkit.CookieManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import any.base.util.BrowserType
import any.base.util.ClipboardUtil
import any.base.util.Intents
import any.ui.common.widget.EmojiEmptyContent
import any.base.R as BaseR

@Composable
internal fun BrowserScreen(
    createWebView: Boolean,
    url: String?,
    forceDark: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPageLoaded: ((String?) -> Unit)? = null,
    title: String? = null,
    userAgent: String? = null,
    titleBarBackgroundColor: Color = MaterialTheme.colors.background,
) {
    val context = LocalContext.current

    var webTitle by remember { mutableStateOf(title ?: url ?: "") }
    var webSubtitle by remember { mutableStateOf<String?>(null) }

    var loadState by remember { mutableStateOf<LoadState>(LoadState.Loading) }

    var loadStartTime by remember { mutableLongStateOf(0L) }

    val webViewController = remember { WebViewController() }

    Column(modifier = modifier.fillMaxSize()) {
        if (url != null) {
            WebView(
                createWebView = createWebView,
                url = url,
                userAgent = userAgent,
                forceDark = forceDark,
                controller = webViewController,
                onTitleUpdated = {
                    if (it != null) {
                        webTitle = it
                        webSubtitle = webViewController.url()
                    }
                },
                onPageStarted = {
                    loadState = LoadState.Loading
                    loadStartTime = System.currentTimeMillis()
                },
                onPageLoaded = {
                    onPageLoaded?.invoke(it)
                    loadState = LoadState.Finished(
                        timeElapse = System.currentTimeMillis() - loadStartTime,
                        isSecure = webViewController.certificate() != null,
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .clipToBounds(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                EmojiEmptyContent {
                    Text(stringResource(BaseR.string.nothing_to_loading))
                }
            }
        }

        TitleBar(
            title = webTitle,
            subtitle = webSubtitle,
            loadState = loadState,
            onBackClick = onBackClick,
            onStopLoadingClick = { webViewController.stopLoading() },
            onReloadClick = { webViewController.reload() },
            onCopyLinkClick = {
                webViewController.url()?.let {
                    ClipboardUtil.copyText(context, it)
                    Toast.makeText(context, BaseR.string.url_copied, Toast.LENGTH_SHORT).show()
                }
            },
            onShareClick = {
                webViewController.url()?.let {
                    Intents.shareText(context, it)
                }
            },
            onOpenInBrowserClick = {
                webViewController.url()?.let {
                    Intents.openInBrowser(context, it, BrowserType.External)
                }
            },
            onRequestClearBrowsingData = { clearCache, clearCookies ->
                if (clearCache) {
                    webViewController.clearCache()
                }
                if (clearCookies) {
                    CookieManager.getInstance().removeAllCookies(null)
                }
            },
            modifier = Modifier.background(titleBarBackgroundColor),
        )
    }
}
