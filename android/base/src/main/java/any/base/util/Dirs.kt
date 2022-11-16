package any.base.util

import android.content.Context
import android.os.Environment
import java.io.File

object Dirs {
    fun servicesDir(context: Context): File {
        return File(context.filesDir, "services").also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
    }

    fun servicesCodeCacheDir(context: Context): File {
        return File(context.filesDir, "services_code_cache").also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
    }

    fun servicesTempDir(context: Context): File {
        return File(context.cacheDir, "service").also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
    }

    fun shareDir(context: Context): File {
        return File(context.cacheDir, "share").also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
    }

    fun postImageDownloadDir(context: Context): File {
        return File(context.getExternalFilesDir(null), "Download/images")
    }

    fun picturesPostImageDownloadDir(): File {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File(dir, "Any")
    }

    fun subsamplingImageTempDir(context: Context): File {
        return File(context.cacheDir, "subsampling").also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
    }

    fun backupTempDir(context: Context): File {
        return File(context.cacheDir, "backup")
    }
}