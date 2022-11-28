package any.data

import any.data.entity.PostsViewType

object ThumbAspectRatio {
    private const val DEFAULT_LIST_COVER_RATIO = 5f / 4
    private const val DEFAULT_GRID_COVER_RATIO = 4f / 5
    private const val DEFAULT_FULL_WIDTH_COVER_RATIO = 5f / 4
    private const val DEFAULT_CARD_COVER_RATIO = 5f / 4

    private val SEPARATORS = charArrayOf(':', '/', 'x', 'X')

    private val cache: MutableMap<String, Float?> = mutableMapOf()

    const val MIN_THUMB_ASPECT_RATIO = 1f / 8
    const val MAX_THUMB_ASPECT_RATIO = 8f / 1

    const val MIN_VIDEO_THUMB_ASPECT_RATIO = 1f
    const val MAX_VIDEO_THUMB_ASPECT_RATIO = 8f / 1f

    fun parse(
        aspectRatio: String?,
        min: Float = MIN_THUMB_ASPECT_RATIO,
        max: Float = MAX_THUMB_ASPECT_RATIO,
        fallback: Float = 1f,
    ): Float {
        return parseOrNull(aspectRatio, min, max) ?: fallback
    }

    fun parseOrNull(
        aspectRatio: String?,
        min: Float = MIN_THUMB_ASPECT_RATIO,
        max: Float = MAX_THUMB_ASPECT_RATIO,
    ): Float ? {
        if (aspectRatio.isNullOrEmpty()) {
            return null
        }
        val cached = cache[aspectRatio]
        if (cached != null) {
            return cached
        }
        val value = parseToFloat(
            aspectRatio = aspectRatio,
            min = min,
            max = max,
        )
        return if (value != null) {
            cache[aspectRatio] = value
            value
        } else {
            null
        }
    }

    private fun parseToFloat(
        aspectRatio: String,
        min: Float,
        max: Float,
    ): Float? {
        var separatorIdx = -1
        for (separator in SEPARATORS) {
            separatorIdx = aspectRatio.indexOf(separator)
            if (separatorIdx != -1) {
                break
            }
        }
        val value = if (separatorIdx != -1) {
            val width = aspectRatio.substring(0, separatorIdx).toIntOrNull()
            val height = aspectRatio.substring(separatorIdx + 1).toIntOrNull()
            if (width != null && height != null) {
                width.toFloat() / height
            } else {
                null
            }
        } else {
            aspectRatio.toFloatOrNull()
        }
        return value?.coerceIn(min, max)
    }

    fun defaultAspectRatio(
        viewType: PostsViewType?,
    ): Float {
        return when (viewType) {
            PostsViewType.FullWidth -> DEFAULT_FULL_WIDTH_COVER_RATIO
            PostsViewType.Grid -> DEFAULT_GRID_COVER_RATIO
            PostsViewType.Card -> DEFAULT_CARD_COVER_RATIO
            else -> DEFAULT_LIST_COVER_RATIO
        }
    }
}
