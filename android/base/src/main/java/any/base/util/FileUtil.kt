package any.base.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.text.TextUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.util.LinkedList

object FileUtil {
    private const val KB = 1024
    private const val MB = KB * 1024
    private const val GB = MB * 1024

    private const val ANDROID_ASSET_PREFIX = "file:///android_asset/"

    fun isAssetsFile(filepath: String): Boolean {
        return filepath.startsWith(ANDROID_ASSET_PREFIX)
    }

    fun readAssetsFile(context: Context, filepath: String): InputStream {
        val path = filepath.removePrefix(ANDROID_ASSET_PREFIX)
        return context.assets.open(path)
    }

    fun byteCountToString(byteCount: Long): String {
        val format = DecimalFormat("0.0")
        val count = byteCount.toDouble()
        return when {
            byteCount < KB -> "O KB"
            byteCount in KB until MB -> format.format(count / KB) + " KB"
            byteCount in MB until GB -> format.format(count / MB) + " MB"
            else -> format.format(count / GB) + " GB"
        }
    }

    fun clearDirectory(dir: File) {
        if (dir.isFile) return
        dir.listFiles()?.forEach {
            if (it.isDirectory) {
                it.deleteRecursively()
            } else {
                it.delete()
            }
        }
    }

    fun directorySize(dir: File): Long {
        val queue = LinkedList<File>()
        queue.add(dir)
        var len = 0L
        while (queue.isNotEmpty()) {
            val file = queue.remove()
            if (file.isDirectory) {
                queue.addAll(file.listFiles() ?: emptyArray())
            } else {
                len += file.length()
            }
        }
        return len
    }

    fun writeBitmap(output: File, bitmap: Bitmap) {
        val buffer = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, buffer)

        val out = FileOutputStream(output)
        buffer.writeTo(out)

        out.close()
        buffer.close()
    }

    fun writeText(
        file: File,
        text: String,
        createDirs: Boolean = true,
        charset: Charset = Charsets.UTF_8,
    ) {
        if (createDirs && file.parentFile?.exists() == false) {
            file.parentFile?.mkdirs()
        }
        file.writeText(text, charset)
    }

    fun copyToUri(
        context: Context,
        input: File,
        output: Uri,
    ) {
        input.inputStream().use { inStream ->
            // Override existing file
            // https://developer.android.com/reference/android/os/ParcelFileDescriptor#parseMode(java.lang.String)
            context.contentResolver.openOutputStream(output, "wt")?.use { outStream ->
                inStream.copyTo(outStream)
            }
        }
    }

    fun copyToFile(
        context: Context,
        input: Uri,
        output: File,
    ) {
        if (!output.exists() && !output.createNewFile()) {
            throw IOException("Cannot create file: $output")
        }
        context.contentResolver.openInputStream(input)?.use { inStream ->
            output.outputStream().use { outStream ->
                inStream.copyTo(outStream)
            }
        }
        if (!output.exists()) {
            throw IOException("Failed to copy $input to $output")
        }
    }

    fun name(filepath: String): String {
        val idx = filepath.lastIndexOf(File.separatorChar)
        return if (idx != -1) {
            filepath.substring(idx)
        } else {
            filepath
        }
    }

    fun extensions(filename: String): String {
        val dotAt = filename.indexOfLast { it == '.' }
        if (dotAt == -1) {
            return ""
        }
        return filename.substring(dotAt + 1)
    }

    //// Functions below were copied from android.os.FileUtils.java /////
    private fun isValidFatFilenameChar(c: Char): Boolean {
        return if (c.code in 0x00..0x1f) {
            false
        } else when (c) {
            '"', '*', '/', ':', '<', '>', '?', '\\', '|', (0x7F).toChar() -> false
            else -> true
        }
    }

    /**
     * Check if given filename is valid for a FAT filesystem.
     */
    fun isValidFatFilename(name: String?): Boolean {
        return name != null && name == buildValidFatFilename(name)
    }

    /**
     * Mutate the given filename to make it valid for a FAT filesystem,
     * replacing any invalid characters with "_".
     */
    fun buildValidFatFilename(name: String): String {
        if (TextUtils.isEmpty(name) || "." == name || ".." == name) {
            return "(invalid)"
        }
        val res = StringBuilder(name.length)
        for (element in name) {
            if (isValidFatFilenameChar(element)) {
                res.append(element)
            } else {
                res.append('_')
            }
        }
        // Even though vfat allows 255 UCS-2 chars, we might eventually write to
        // ext4 through a FUSE layer, so use that limit.
        trimFilename(res, 255)
        return res.toString()
    }

    private fun trimFilename(res: StringBuilder, maxBytes: Int) {
        var mutMaxBytes = maxBytes
        var raw: ByteArray = res.toString().toByteArray(StandardCharsets.UTF_8)
        if (raw.size > mutMaxBytes) {
            mutMaxBytes -= 3
            while (raw.size > mutMaxBytes) {
                res.deleteCharAt(res.length / 2)
                raw = res.toString().toByteArray(StandardCharsets.UTF_8)
            }
            res.insert(res.length / 2, "...")
        }
    }
    //// Functions above were copied from android.os.FileUtils.java /////
}