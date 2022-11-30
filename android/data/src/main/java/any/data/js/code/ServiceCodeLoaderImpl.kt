package any.data.js.code

import android.content.Context
import any.base.util.Dirs
import any.base.util.Http
import any.base.util.MB
import any.base.util.toHexString
import any.data.entity.Checksums
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

class ServiceCodeLoaderImpl(context: Context) : ServiceCodeLoader {
    private val context = context.applicationContext

    private val httpClient by lazy {
        val cache = Cache(Dirs.servicesCodeCacheDir(context), MAX_CODE_CACHE_SIZE)
        Http.DEFAULT_CLIENT_BUILDER.cache(cache).build()
    }

    override suspend fun load(
        checksums: Checksums,
        url: String
    ): String = withContext(Dispatchers.IO) {
        val text = when {
            url.startsWith("file:///android_asset/") -> {
                context.assets.open(url.removePrefix("file:///android_asset/"))
                    .also { it.verifyChecksums(checksums) }
                    .also { it.reset() }
                    .bufferedReader()
                    .use { it.readText() }
            }

            url.startsWith("file://") -> {
                val file = File(url.removePrefix("file://"))
                require(file.exists()) { "Source file not exists: $file" }
                file.inputStream().use { it.verifyChecksums(checksums) }
                file.readText()
            }

            url.startsWith("/") -> {
                val file = File(url)
                require(file.exists()) { "Source file not exists: $file" }
                file.inputStream().use { it.verifyChecksums(checksums) }
                file.readText()
            }

            url.startsWith("http://") || url.startsWith("https://") -> {
                val call = Request.Builder().url(url).get().build()
                val res = httpClient.newCall(call).execute()
                res.body?.byteStream()?.verifyChecksums(checksums)
                res.body?.byteStream()?.bufferedReader()
                    ?.use { it.readText() }
                    ?: throw IllegalStateException("Cannot read remote code")
            }

            else -> {
                throw IllegalArgumentException("Unsupported url: $url")
            }
        }

        text
    }

    private fun InputStream.verifyChecksums(checksums: Checksums) {
        val md5Digest = MessageDigest.getInstance("md5")
        val sha1Digest = MessageDigest.getInstance("sha1")
        val sha256Digest = MessageDigest.getInstance("sha256")
        val sha512Digest = MessageDigest.getInstance("sha512")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var len: Int
        while (read(buffer).also { len = it } != -1) {
            md5Digest.update(buffer, 0, len)
            sha1Digest.update(buffer, 0, len)
            sha256Digest.update(buffer, 0, len)
            sha512Digest.update(buffer, 0, len)
        }
        val current = Checksums(
            md5 = md5Digest.digest().toHexString(),
            sha1 = sha1Digest.digest().toHexString(),
            sha256 = sha256Digest.digest().toHexString(),
            sha512 = sha512Digest.digest().toHexString()
        )
        require(current == checksums) { "Failed to verify the service code" }
    }

    companion object {
        private val MAX_CODE_CACHE_SIZE = 50.MB
    }
}