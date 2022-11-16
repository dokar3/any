package any.base.util

import any.base.R as BaseR
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.FileProvider
import any.base.image.DownloadedImageFetcher
import any.base.image.PostImageSaver
import any.base.log.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

object Intents {
    suspend fun shareImage(
        context: Context,
        imageFetcher: DownloadedImageFetcher,
        url: String,
        chooserTitle: String = context.getString(BaseR.string.share_image),
        packageName: String? = null,
    ) = withContext(Dispatchers.IO) {
        val ext = when (val ext = MimeTypeMap.getFileExtensionFromUrl(url)) {
            "", "jpeg" -> "jpg"
            else -> ext
        }
        val tempFile = File(Dirs.shareDir(context), "sharing_image.$ext")
        val saved = try {
            PostImageSaver.saveImageToFile(
                imageFetcher = imageFetcher,
                url = url,
                targetFile = tempFile,
            )
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
        if (saved && isActive) {
            val uri = FileProvider.getUriForFile(
                context,
                context.getString(BaseR.string.file_provider_authorities),
                tempFile
            )
            share(context = context, chooserTitle = chooserTitle) {
                setPackage(packageName)
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                clipData = ClipData.newRawUri(null, uri)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    fun shareText(
        context: Context,
        text: String,
        chooserTitle: String = context.getString(BaseR.string.share),
    ) {
        share(context = context, chooserTitle = chooserTitle) {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }

    fun share(
        context: Context,
        chooserTitle: String,
        config: Intent.() -> Unit,
    ) {
        val intent = Intent(Intent.ACTION_SEND).also(config)
        try {
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            Logger.e("Intents", "Sharing failed, intent: $intent")
        }
    }

    fun playVideo(
        context: Context,
        url: String,
    ) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.parse(url), "video/*")
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Logger.e("Intents", "Cannot find app to play this video, url: $url")
        }
    }

    fun openInBrowser(
        context: Context,
        url: String,
        browserType: BrowserType = BrowserType.Internal,
    ) {
        when {
            isAppUrl(url) -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    setPackage(context.packageName)
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            browserType == BrowserType.Internal -> {
                val intent = Intent("any.action.BROWSE", Uri.parse(url)).apply {
                    setPackage(context.packageName)
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            browserType == BrowserType.External -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            browserType == BrowserType.ChromeCustomTab -> {
                try {
                    CustomTabsIntent.Builder()
                        .build()
                        .launchUrl(context, Uri.parse(url))
                } catch (e: Exception) {
                    e.printStackTrace()
                    Logger.e("Intents", "Cannot open url: $url")
                }
            }
        }
    }

    private fun isAppUrl(url: String): Boolean = url.startsWith("anyapp://")
}

enum class BrowserType {
    Internal,
    External,
    ChromeCustomTab,
}