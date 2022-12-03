package any.base.util

fun List<String>.joinToPath(): String {
    return PathJoiner(this).join()
}

class PathJoiner {
    private val segments: MutableList<String>

    private var separator: String = "/"

    constructor(vararg segments: String) {
        this.segments = segments.toMutableList()
    }

    constructor(segments: List<String>) {
        this.segments = segments.toMutableList()
    }

    fun add(segment: String): PathJoiner {
        this.segments.add(segment)
        return this
    }

    fun separator(separator: String): PathJoiner {
        this.separator = separator
        return this
    }

    fun join(): String {
        val builder = StringBuilder()
        for (segment in segments) {
            if (segment.isEmpty()) {
                continue
            }
            builder.append(separator)
            builder.append(segment.removeSurrounding(separator))
        }
        return if (builder.isNotEmpty()) {
            builder.substring(1)
        } else {
            ""
        }
    }

    override fun toString(): String {
        return join()
    }
}