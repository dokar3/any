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
                    .readAndVerifyCode(checksums)
            }

            url.startsWith("file://") -> {
                val file = File(url.removePrefix("file://"))
                require(file.exists()) { "Source file not exists: $file" }
                file.inputStream().readAndVerifyCode(checksums)
            }

            url.startsWith("/") -> {
                val file = File(url)
                require(file.exists()) { "Source file not exists: $file" }
                file.inputStream().readAndVerifyCode(checksums)
            }

            url.startsWith("http://") || url.startsWith("https://") -> {
                val call = Request.Builder().url(url).get().build()
                val res = httpClient.newCall(call).execute()
                val inputStream = res.body?.byteStream()
                    ?: throw IllegalStateException("Cannot read remote code")
                inputStream.readAndVerifyCode(checksums)
            }

            else -> {
                throw IllegalArgumentException("Unsupported url: $url")
            }
        }

        text
    }

    private fun InputStream.readAndVerifyCode(checksums: Checksums): String = use {
        it.checkMaxCodeBytes()
        val bytes = it.readBytes()
        bytes.verifyChecksums(checksums)
        bytes.decodeToString()
    }

    private fun InputStream.checkMaxCodeBytes() {
        val availableBytes = available()
        if (availableBytes > MAX_CODE_BYTES) {
            throw IllegalStateException("Too many bytes of code: $availableBytes")
        }
    }

    private fun ByteArray.verifyChecksums(checksums: Checksums) {
        val md5Digest = MessageDigest.getInstance("md5")
        val sha1Digest = MessageDigest.getInstance("sha1")
        val sha256Digest = MessageDigest.getInstance("sha256")
        val sha512Digest = MessageDigest.getInstance("sha512")
        val current = Checksums(
            md5 = md5Digest.digest(this).toHexString(),
            sha1 = sha1Digest.digest(this).toHexString(),
            sha256 = sha256Digest.digest(this).toHexString(),
            sha512 = sha512Digest.digest(this).toHexString()
        )
        require(current == checksums) { "Failed to verify the service code" }
    }

    companion object {
        private val MAX_CODE_CACHE_SIZE = 50.MB

        private val MAX_CODE_BYTES = 1.MB
    }
}