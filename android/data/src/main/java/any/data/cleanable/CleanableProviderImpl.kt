package any.data.cleanable

import android.content.Context

class CleanableProviderImpl(context: Context) : CleanableProvider {
    private val context = context.applicationContext

    override fun get(type: Cleanable.Type): Cleanable {
        return when (type) {
            Cleanable.Type.DiskCacheImages -> DiskCacheImages()
            Cleanable.Type.DownloadedImage -> DownloadedImages(context)
        }
    }
}