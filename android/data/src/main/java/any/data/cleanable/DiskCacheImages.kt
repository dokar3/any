package any.data.cleanable

import any.base.util.FileUtil
import any.data.entity.SpaceInfo
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

class DiskCacheImages : Cleanable {
    override suspend fun clean(): Boolean = withContext(Dispatchers.IO) {
        ImagePipelineFactory.getInstance().also {
            it.mainBufferedDiskCache.clearAll().waitForCompletion()
            it.smallImageFileCache.clearAll()
        }
        true
    }

    override suspend fun spaceInfo(): SpaceInfo = withContext(Dispatchers.IO) {
        val mainDiskCacheConfig = Fresco.getImagePipeline().config.mainDiskCacheConfig
        val maxSize = mainDiskCacheConfig.defaultSizeLimit
        val dir = File(
            mainDiskCacheConfig.baseDirectoryPathSupplier.get(),
            mainDiskCacheConfig.baseDirectoryName,
        )
        val size = FileUtil.length(dir)
        val available = min(dir.freeSpace, (maxSize - size).coerceAtLeast(0L))
        SpaceInfo(size, maxSize, available)
    }
}