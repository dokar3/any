package any.data.cleanable

import android.content.Context
import any.base.util.Dirs
import any.base.util.FileUtil
import any.data.cache.ExoVideoCache
import any.data.entity.SpaceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

class DiskCacheVideos(context: Context) : Cleanable {
    private val cacheDir = Dirs.videoCacheDir(context)

    override suspend fun clean(): Boolean = withContext(Dispatchers.IO) {
        FileUtil.clearDirectory(cacheDir)
        true
    }

    override suspend fun spaceInfo(): SpaceInfo {
        val size = FileUtil.length(cacheDir)
        val max = ExoVideoCache.MAX_CACHE_SIZE
        return SpaceInfo(
            size = size,
            maxSize = max,
            available = min(cacheDir.freeSpace, (max - size).coerceAtLeast(0L)),
        )
    }
}