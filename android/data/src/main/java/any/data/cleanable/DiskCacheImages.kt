package any.data.cleanable

import any.base.util.FileUtil
import any.data.entity.SpaceInfo
import com.facebook.drawee.backends.pipeline.Fresco
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

class DiskCacheImages : Cleanable {
    override suspend fun clean(): Boolean = withContext(Dispatchers.IO) {
        Fresco.getImagePipeline().clearDiskCaches()
        true
    }

    override suspend fun spaceInfo(): SpaceInfo = withContext(Dispatchers.IO) {
        val mainDiskCacheConfig = Fresco.getImagePipeline().config.mainDiskCacheConfig
        val maxSize = mainDiskCacheConfig.defaultSizeLimit
        val dir = mainDiskCacheConfig.baseDirectoryPathSupplier.get()
        val size = FileUtil.folderSize(dir)
        val available = min(maxSize - size, dir.freeSpace)
        SpaceInfo(size, maxSize, available)
    }
}