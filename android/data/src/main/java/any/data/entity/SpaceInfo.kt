package any.data.entity

import androidx.compose.runtime.Immutable
import any.base.util.FileUtil

@Immutable
data class SpaceInfo(
    val size: Long,
    val maxSize: Long,
    val available: Long,
) {
    val occupiedPercent = if (maxSize > 0L) {
        (size.toFloat() / maxSize).coerceIn(0f, 1f)
    } else {
        0f
    }

    val availablePercent = if (maxSize > 0L) {
        (available.toFloat() / maxSize).coerceIn(0f, 1f)
    } else {
        0f
    }

    val readableOccupiedSize = FileUtil.byteCountToString(size)

    val readableMaxSize = FileUtil.byteCountToString(maxSize)

    val readableAvailableSize = FileUtil.byteCountToString(available)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SpaceInfo

        if (size != other.size) return false
        if (maxSize != other.maxSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = size.hashCode()
        result = 31 * result + maxSize.hashCode()
        return result
    }

    companion object {
        val None = SpaceInfo(0, 0, 0)
    }
}
