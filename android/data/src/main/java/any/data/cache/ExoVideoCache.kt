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

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object ExoVideoCache {
    val MAX_CACHE_SIZE = 500.MB

    @Volatile
    private var cache: Cache? = null

    suspend fun release() = withContext(Dispatchers.IO) {
        synchronized(this) {
            cache?.release()
            cache = null
        }
    }

    fun  get(context: Context): Cache {
        return cache ?: synchronized(this) {
            cache ?: SimpleCache(
                Dirs.videoCacheDir(context),
                LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE),
                StandaloneDatabaseProvider(context)
            ).also {
                cache = it
            }
        }
    }
}