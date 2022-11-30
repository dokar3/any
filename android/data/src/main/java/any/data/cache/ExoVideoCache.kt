package any.data.cache

import android.content.Context
import any.base.util.Dirs
import any.base.util.MB
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    fun get(context: Context): Cache {
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