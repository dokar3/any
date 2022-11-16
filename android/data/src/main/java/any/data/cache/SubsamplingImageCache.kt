package any.data.cache

import android.content.Context
import any.base.cache.TwoTypesCache
import any.base.util.Dirs
import com.jakewharton.disklrucache.DiskLruCache
import java.io.File
import java.io.InputStream
import java.lang.reflect.Method

class SubsamplingImageCache(
    private val cacheDir: File
) : TwoTypesCache<String, InputStream, File> {
    private val lruEntriesField by lazy {
        DiskLruCache::class.java.getDeclaredField("lruEntries").also {
            it.isAccessible = true
        }
    }

    private val inEditorEntryField by lazy {
        DiskLruCache.Editor::class.java.getDeclaredField("entry").also {
            it.isAccessible = true
        }
    }

    // DiskLruCache.Entry.getCleanFile(int index)
    private var _getCleanFileMethod: Method? = null

    // DiskLruCache.lruEntries
    private var _lruEntries: HashMap<String, Any>? = null

    private val cache: DiskLruCache by lazy {
        DiskLruCache.open(
            cacheDir, /* directory */
            DISK_CACHE_VER, /* appVersion */
            1, /* valueCount */
            MAX_DISK_CACHE_SIZE /* maxSize */
        )
    }

    override fun get(key: String): File? {
        val entry = lruEntries()[lruKey(key)]
        return if (entry != null) {
            getCleanFileFromLruEntry(entry)
        } else {
            null
        }
    }

    override fun put(key: String, value: InputStream): File {
        return cache.edit(lruKey(key)).let {
            it.newOutputStream(0).use { outputStream ->
                value.copyTo(outputStream)
            }
            it.commit()

            val entry = inEditorEntryField.get(it)!!
            getCleanFileFromLruEntry(entry)
        }
    }

    override fun remove(key: String) {
        cache.remove(lruKey(key))
    }

    override fun contains(key: String): Boolean {
        return cache.get(lruKey(key)) != null
    }

    override fun clear() {
        cache.directory.deleteRecursively()
        lruEntries().clear()
    }

    private fun lruKey(url: String): String {
        return url.hashCode().toString(16)
    }

    @Suppress("unchecked_cast")
    private fun lruEntries(): HashMap<String, Any> {
        return _lruEntries ?: lruEntriesField.get(cache)
            .let { it as HashMap<String, Any> }
            .also { _lruEntries = it }
    }

    private fun getCleanFileFromLruEntry(lruEntry: Any): File {
        val method = _getCleanFileMethod ?: lruEntry.javaClass
            .getDeclaredMethod("getCleanFile", Int::class.java).also {
                it.isAccessible = true
                _getCleanFileMethod = it
            }
        return method.invoke(lruEntry, 0) as File
    }

    companion object {
        private const val DISK_CACHE_VER = 1

        private const val MAX_DISK_CACHE_SIZE = 1024L * 1024 * 50 // 50 MB

        @Volatile
        private var instance: SubsamplingImageCache? = null

        fun get(context: Context): SubsamplingImageCache {
            return instance ?: synchronized(SubsamplingImageCache::class) {
                instance ?: SubsamplingImageCache(
                    cacheDir = Dirs.subsamplingImageTempDir(context)
                ).also { instance = it }
            }
        }
    }
}