package any.data.cleanable

import any.data.entity.SpaceInfo

interface Cleanable {
    suspend fun clean(): Boolean

    suspend fun spaceInfo(): SpaceInfo

    enum class Type {
        DiskCacheImages,
        DownloadedImage,
    }
}