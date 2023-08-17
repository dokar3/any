package any.data.entity

import androidx.compose.runtime.Immutable
import any.base.util.PathJoiner
import any.base.util.joinToPath
import java.io.File

@Immutable
data class Folder(
    val path: String,
    val posts: List<Post>? = null,
) {
    val pathSegments: List<String> by lazy {
        path.trim(File.separatorChar)
            .split(File.separatorChar)
            .filter { it.isNotEmpty() }
            .takeIf { it.isNotEmpty() } ?: listOf("")
    }

    val name: String by lazy { pathSegments.last() }

    val validPath: String by lazy { pathSegments.joinToPath() }

    val parentPath: String by lazy {
        if (pathSegments.size > 1) {
            PathJoiner(pathSegments.subList(0, pathSegments.lastIndex)).join()
        } else {
            ROOT.path
        }
    }

    fun isRoot(): Boolean = path == ROOT.path

    fun isTheSame(sub: Folder): Boolean {
        return path.trim(File.separatorChar) == sub.path.trim(File.separatorChar)
    }

    fun isTheSameOrSubFolderOf(sub: Folder): Boolean {
        if (isRoot()) {
            return true
        }
        val subSegments = sub.pathSegments
        val currentSegments = pathSegments
        if (currentSegments.size > subSegments.size) {
            return false
        }
        for (i in currentSegments.indices) {
            if (subSegments[i] != currentSegments[i]) {
                return false
            }
        }
        return true
    }

    companion object {
        val ROOT = Folder("")
    }
}