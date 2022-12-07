package any.navigation

class NavigationBlocker {
    private val blockList = mutableSetOf<String>()
    private val allowList = mutableSetOf<String>()

    val blockedRoutes: Set<String> = blockList.toSet()
    val allowedRoutes: Set<String> = allowList.toSet()

    private var isBlocked = false

    fun allow(route: String) {
        allowList.add(route)
        blockList.remove(route)
    }

    fun allowAll() {
        isBlocked = false
        blockList.clear()
    }

    fun blockAll() {
        isBlocked = true
        allowList.clear()
    }

    fun block(route: String) {
        blockList.add(route)
        allowList.remove(route)
        isBlocked = true
    }

    fun isAllowed(route: String): Boolean {
        if (!isBlocked) return true
        if (blockList.contains(route)) return false
        return allowList.contains(route)
    }

    fun isBlocked(route: String): Boolean {
        if (!isBlocked) return false
        if (blockList.contains(route)) return true
        return !allowList.contains(route)
    }
}