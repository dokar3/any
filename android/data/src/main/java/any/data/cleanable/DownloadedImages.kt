package any.data.cleanable

import android.content.Context
import any.base.util.Dirs
import any.data.entity.SpaceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

class DownloadedImages(
    context: Context
) : Cleanable {
    private val context = context.applicationContext

    override suspend fun clean(): Boolean = withContext(Dispatchers.IO) {
        val dir = Dirs.postImageDownloadDir(context)
        if (!dir.exists()) {
            return@withContext false
        }
        dir.deleteRecursively()
    }

    override suspend fun spaceInfo(): SpaceInfo = withContext(Dispatchers.IO) {
        val dir = Dirs.postImageDownloadDir(context)
        val occupied = dir.walkTopDown().fold(0L) { acc, file -> acc + file.length() }
        val max = dir.totalSpace
        val available = min(dir.freeSpace, (max - occupied).coerceAtLeast(0L))
        SpaceInfo(occupied, max, available)
    }
}