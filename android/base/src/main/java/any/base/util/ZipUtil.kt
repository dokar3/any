package any.base.util

import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtil {
    private const val BUFFER_SIZE = 1024 * 8

    fun zip(inputs: List<File>, output: File) {
        if (output.exists()) {
            output.delete()
        }
        ZipOutputStream(output.outputStream()).use { zipOut ->
            for (input in inputs) {
                zipOut.addFile(dir = "", input)
            }
        }
    }

    private fun ZipOutputStream.addFile(dir: String, file: File) {
        val name = if (dir.isNotEmpty()) {
            dir + File.separator + file.name
        } else {
            file.name
        }
        if (file.isFile) {
            putNextEntry(ZipEntry(name))
            file.inputStream().use { inputStream ->
                val buffer = ByteArray(BUFFER_SIZE)
                do {
                    val len = inputStream.read(buffer)
                    if (len != -1) {
                        write(buffer, 0, len)
                    }
                } while (len != -1)
            }
        } else {
            val files = file.listFiles() ?: emptyArray()
            for (f in files) {
                addFile(dir = name, file = f)
            }
        }
    }

    fun unzip(zip: File, dir: File) {
        ZipInputStream(zip.inputStream()).use { zipIn ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val zipEntry = zipIn.nextEntry ?: break
                val file = File(dir, zipEntry.name)
                if (zipEntry.isDirectory) {
                    if (!file.exists() && !file.mkdirs()) {
                        throw IOException("Cannot create dir: $file")
                    }
                } else {
                    val entryDir = file.parentFile
                    if (entryDir != null && !entryDir.exists() && !entryDir.mkdirs()) {
                        throw IOException("Cannot create dir: $entryDir")
                    }
                    file.outputStream().use { output ->
                        do {
                            val len = zipIn.read(buffer)
                            if (len != -1) {
                                output.write(buffer, 0, len)
                            }
                        } while (len != -1)
                    }
                }
            }
        }
    }

    /**
     * For each zip entries in the [ZipInputStream].
     */
    fun eachEntry(
        zipIn: ZipInputStream,
        action: (ZipEntry) -> Unit,
    ) {
        do {
            val entry = zipIn.nextEntry
            if (entry != null) {
                action(entry)
            }
        } while (entry != null)
    }

    /**
     * Seek [ZipInputStream] to target zip entry, returns true if target entry is found,
     * false otherwise.
     */
    fun seekTo(
        zipIn: ZipInputStream,
        predicate: (ZipEntry) -> Boolean,
    ): Boolean {
        do {
            val entry = zipIn.nextEntry
            if (entry != null) {
                if (predicate(entry)) {
                    return true
                }
            }
        } while (entry != null)
        return false
    }
}