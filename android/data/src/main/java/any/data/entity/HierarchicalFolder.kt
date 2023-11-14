package any.data.entity

import androidx.compose.runtime.Stable
import any.base.util.PathJoiner
import any.base.util.joinToPath
import java.io.File

@Stable
data class HierarchicalFolder(
    val path: String,
    val subFolders: MutableList<HierarchicalFolder> = mutableListOf(),
    val expanded: Boolean = false,
    val depth: Int = 0,
    val updatedAt: Long = 0,
) {
    val pathSegments: List<String> = path.trim(Folder.pathSeparator)
        .split(Folder.pathSeparator)
        .filter { it.isNotEmpty() }
        .takeIf { it.isNotEmpty() } ?: listOf("")

    val name: String = pathSegments.last()

    val validPath: String = pathSegments.joinToPath()

    val parentPath: String by lazy {
        if (pathSegments.size > 1) {
            PathJoiner(pathSegments.subList(0, pathSegments.lastIndex)).join()
        } else {
            Folder.ROOT.path
        }
    }

    fun toFolder(): Folder {
        return Folder(path = this.path)
    }

    override fun toString(): String {
        return "Folder: $path"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HierarchicalFolder

        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }

    companion object {
        val ROOT = HierarchicalFolder(path = "")
    }
}
