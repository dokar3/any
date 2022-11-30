package any.base.image

@JvmInline
value class PostImageSources private constructor(private val value: Int) {
    fun contains(source: PostImageSources): Boolean {
        return value and source.value != 0
    }

    operator fun plus(other: PostImageSources): PostImageSources {
        return PostImageSources(value or other.value)
    }

    operator fun minus(other: PostImageSources): PostImageSources {
        return PostImageSources(value and other.value.inv())
    }

    companion object {
        private const val SOURCE_NONE = 0
        private const val SOURCE_MEMORY = 1 shl 1
        private const val SOURCE_DISK_CACHE = 1 shl 2
        private const val SOURCE_DOWNLOAD_DIR = 1 shl 3
        private const val SOURCE_SUBSAMPLING_CACHE_DIR = 1 shl 4
        private const val SOURCE_NETWORK = 1 shl 5

        fun none(): PostImageSources {
            return PostImageSources(SOURCE_NONE)
        }

        fun memory(): PostImageSources {
            return PostImageSources(SOURCE_MEMORY)
        }

        fun diskCache(): PostImageSources {
            return PostImageSources(SOURCE_DISK_CACHE)
        }

        fun downloadDir(): PostImageSources {
            return PostImageSources(SOURCE_DOWNLOAD_DIR)
        }

        fun subsamplingCacheDir(): PostImageSources {
            return PostImageSources(SOURCE_SUBSAMPLING_CACHE_DIR)
        }

        fun network(): PostImageSources {
            return PostImageSources(SOURCE_NETWORK)
        }

        fun all(): PostImageSources {
            return memory() + diskCache() + downloadDir() + subsamplingCacheDir() + network()
        }
    }
}