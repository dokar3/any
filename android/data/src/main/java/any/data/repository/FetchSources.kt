package any.data.repository

import kotlin.math.abs

@JvmInline
value class FetchSources private constructor(
    private val value: Int
) {
    fun contains(source: FetchSources): Boolean {
        return abs(value) and source.value != 0
    }

    fun isOneShot(): Boolean {
        return value < 0
    }

    fun toOneShot(): FetchSources {
        return FetchSources(-abs(value))
    }

    operator fun plus(other: FetchSources): FetchSources {
        return FetchSources(value or other.value)
    }

    operator fun minus(other: FetchSources): FetchSources {
        return FetchSources(value and other.value.inv())
    }

    companion object {
        private const val SOURCE_NONE = 0
        private const val SOURCE_CACHE = 1 shl 1
        private const val SOURCE_NETWORK = 1 shl 2

        fun none(): FetchSources {
            return FetchSources(SOURCE_NONE)
        }

        fun cache(): FetchSources {
            return FetchSources(SOURCE_CACHE)
        }

        fun remote(): FetchSources {
            return FetchSources(SOURCE_NETWORK)
        }

        fun all(): FetchSources {
            return cache() + remote()
        }
    }
}
