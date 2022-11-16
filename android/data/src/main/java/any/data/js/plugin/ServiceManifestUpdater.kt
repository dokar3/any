package any.data.js.plugin

interface ServiceManifestUpdater {
    fun updateName(newValue: String)

    fun updateDeveloper(newValue: String)

    fun updateDeveloperUrl(newValue: String?)

    fun updateDeveloperAvatar(newValue: String?)

    fun updateDescription(newValue: String)

    fun updateHomepage(newValue: String?)

    fun updateChangelog(newValue: String?)

    fun updateIsPageable(newValue: Boolean)

    fun updateViewType(newValue: String?)

    fun updateMediaAspectRatio(newValue: String)

    fun updateIcon(newValue: String?)

    fun updateHeaderImage(newValue: String?)

    fun updateThemeColor(newValue: String?)

    fun updateDarkThemeColor(newValue: String?)

    fun updateLanguages(newValue: Array<String>?)

    fun updateSupportedPostUrls(newValue: Array<String>?)

    fun updateSupportedUserUrls(newValue: Array<String>?)
}