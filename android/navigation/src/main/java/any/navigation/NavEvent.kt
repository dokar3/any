package any.navigation

fun navPushEvent(route: String): NavEvent {
    return NavEvent.Push(route)
}

sealed interface NavEvent {
    object Back : NavEvent

    class Push(val route: String) : NavEvent

    class ReplaceWith(val route: String) : NavEvent

    class PushImagePager(val route: String, val images: List<String>) : NavEvent
}
