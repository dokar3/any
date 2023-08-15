package any.data.entity

import androidx.compose.runtime.Immutable
import any.base.util.PathJoiner

@Immutable
data class Folder(
    val path: String,
    val posts: List<Post>? = null,
) {
    val name: String by lazy {
        path.trim('/').split("/").last()
    }

    val pathSegments: List<String> by lazy {
        path.trim('/').split("/")
    }

    val parentPath: String by lazy {
        if (pathSegments.size > 1) {
            PathJoiner(pathSegments.subList(0, pathSegments.lastIndex)).join()
        } else {
            ROOT.path
        }
    }

    fun isRoot(): Boolean = path == ROOT.path

    fun isTheSame(sub: Folder): Boolean {
        return path.trim('/') == sub.path.trim('/')
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