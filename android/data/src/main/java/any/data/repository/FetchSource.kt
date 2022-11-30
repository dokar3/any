package any.data.repository

@JvmInline
value class FetchControl private constructor(
    private val value: Int
) {
    fun includesSource(source: FetchSource): Boolean {
        return value and source.value != 0
    }

    fun isOneShot(): Boolean {
        return value shr 30 == 1
    }

    fun toOneShot(): FetchControl {
        val v = value or (1 shl 30)
        return FetchControl(value = v)
    }

    companion object {
        fun of(vararg sources: FetchSource): FetchControl {
            var i = 0
            for (source in sources) {
                i = i or source.value
            }
            return FetchControl(value = i)
        }

        fun ofOneShot(vararg sources: FetchSource): FetchControl {
            return of(*sources).toOneShot()
        }
    }
}

enum class FetchSource(internal val value: Int) {
    Cache(1),
    Remote(1 shl 1),
}
