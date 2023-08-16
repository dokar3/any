package any.data.cache

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import any.base.util.Dirs
import any.base.util.MB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object ExoVideoCache {
    val MAX_CACHE_SIZE = 512.MB

    private val currMaxSize = AtomicLong(MAX_CACHE_SIZE)

    @Volatile
    private var cache: Cache? = null

    suspend fun release() = withContext(Dispatchers.IO) {
        synchronized(this) {
            cache?.release()
            cache = null
        }
    }

    fun get(
        context: Context,
        maxCacheSize: Long = MAX_CACHE_SIZE,
    ): Cache {
        val validMaxCacheSize = maxCacheSize.takeIf { it > 0L } ?: MAX_CACHE_SIZE
        val cache = this.cache
        if (cache != null && currMaxSize.get() == validMaxCacheSize) {
            return cache
        }
        synchronized(this) {
            val currCache = this.cache
            if (currCache != null && currMaxSize.get() == validMaxCacheSize) {
                return currCache
            }
            val newCache = SimpleCache(
                Dirs.videoCacheDir(context),
                LeastRecentlyUsedCacheEvictor(validMaxCacheSize),
                StandaloneDatabaseProvider(context)
            )
            currMaxSize.set(validMaxCacheSize)
            this.cache = newCache
            return newCache
        }
    }
}