package any.navigation

object Routes {
    const val PASSWORD = "password"

    const val HOME = "home"

    const val RUN_SQL = "run_sql"

    const val POST = "post/{postUrl}?serviceId={serviceId}&" +
            "initialElementIndex={initialElementIndex}&" +
            "initialElementScrollOffset={initialElementScrollOffset}"

    const val PROFILE = "profile?serviceId={serviceId}&userId={userId}&userUrl={userUrl}"

    const val SEARCH = "search?serviceId={serviceId}&query={query}"

    const val SETTINGS = "settings?subSettings={subSettings}"

    const val IMAGE_PAGER = "image_pager?title={title}&currPage={currPage}"

    object Home {
        const val FRESH = "home_fresh"

        const val FOLLOWING = "home_following"

        const val COLLECTIONS = "home_collections"

        const val DOWNLOADS = "home_downloads"
    }

    object Settings {
        const val MAIN = "main_settings"

        const val UI = "ui_settings"

        const val DEV = "dev_settings"

        const val FILES = "files"

        const val SERVICE_MGT = "service_management"

        const val PRIVACY = "privacy_settings"

        const val ABOUT = "about"

        const val LIBS = "libs"
    }

    fun parameterizedRouteOf(route: String): Builder {
        return Builder(route)
    }

    class Builder(private var route: String) {
        fun set(name: String, value: Any?): Builder {
            route = route.replace("{${name}}", value?.toString() ?: "")
            return this
        }

        fun get(): String = route
    }
}