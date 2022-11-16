package any.data.js.plugin

import any.data.entity.ServiceManifest
import any.data.entity.ServiceViewType

class MemoryServiceManifestUpdater(
    private val latest: () -> ServiceManifest,
    private val update: (ServiceManifest) -> Unit,
) : ServiceManifestUpdater {
    override fun updateName(newValue: String) {
        update(latest().copy(name = newValue))
    }

    override fun updateDeveloper(newValue: String) {
        update(latest().copy(developer = newValue))
    }

    override fun updateDeveloperUrl(newValue: String?) {
        update(latest().copy(developerUrl = newValue))
    }

    override fun updateDeveloperAvatar(newValue: String?) {
        update(latest().copy(developerAvatar = newValue))
    }

    override fun updateDescription(newValue: String) {
        update(latest().copy(description = newValue))
    }

    override fun updateHomepage(newValue: String?) {
        update(latest().copy(homepage = newValue))
    }

    override fun updateChangelog(newValue: String?) {
        update(latest().copy(changelog = newValue))
    }

    override fun updateIsPageable(newValue: Boolean) {
        update(latest().copy(isPageable = newValue))
    }

    override fun updateViewType(newValue: String?) {
        val viewType = ServiceViewType.values().find { type -> type.value == newValue }
        update(latest().copy(viewType = viewType))
    }

    override fun updateMediaAspectRatio(newValue: String) {
        update(latest().copy(mediaAspectRatio = newValue))
    }

    override fun updateIcon(newValue: String?) {
        update(latest().copy(icon = newValue))
    }

    override fun updateHeaderImage(newValue: String?) {
        update(latest().copy(headerImage = newValue))
    }

    override fun updateThemeColor(newValue: String?) {
        update(latest().copy(themeColor = newValue))
    }

    override fun updateDarkThemeColor(newValue: String?) {
        update(latest().copy(darkThemeColor = newValue))
    }

    override fun updateLanguages(newValue: Array<String>?) {
        update(latest().copy(languages = newValue?.toList()))
    }

    override fun updateSupportedPostUrls(newValue: Array<String>?) {
        update(latest().copy(supportedPostUrls = newValue?.toList()))
    }

    override fun updateSupportedUserUrls(newValue: Array<String>?) {
        update(latest().copy(supportedUserUrls = newValue?.toList()))
    }
}