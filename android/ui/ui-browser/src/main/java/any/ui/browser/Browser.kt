package any.ui.browser

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun Browser(
    url: String,
    title: String? = null,
    userAgent: String? = null,
    cookiesTargetUrl: String? = null,
    onGetCookies: ((String?) -> Unit)? = null,
) {
    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            val cookies = it.data?.getStringExtra(BrowserActivity.EXTRA_RESULT_COOKIES)
            onGetCookies?.invoke(cookies)
        }

    val context = LocalContext.current

    LaunchedEffect(url) {
        val intent = Intent(context, BrowserActivity::class.java).apply {
            putExtra(BrowserActivity.EXTRA_URL, url)
            putExtra(BrowserActivity.EXTRA_TITLE, title)
            putExtra(BrowserActivity.EXTRA_USER_AGENT, userAgent)
            putExtra(BrowserActivity.EXTRA_COOKIES_TARGET_URL, cookiesTargetUrl)
        }
        launcher.launch(intent)
    }
}