package any.data.js.plugin

import any.data.entity.ServiceManifest
import any.data.entity.ServiceViewType
import any.data.repository.ServiceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class LocalServiceManifestUpdater(
    service: ServiceManifest,
    private val serviceRepository: ServiceRepository,
    private val coroutineScope: CoroutineScope,
) : ServiceManifestUpdater {
    private val serviceId = service.id

    override fun updateName(newValue: String) {
        updateLatestService { it.copy(name = newValue) }
    }

    override fun updateDeveloper(newValue: String) {
        updateLatestService { it.copy(developer = newValue) }
    }

    override fun updateDeveloperUrl(newValue: String?) {
        updateLatestService { it.copy(developerUrl = newValue) }
    }

    override fun updateDeveloperAvatar(newValue: String?) {
        updateLatestService { it.copy(developerAvatar = newValue) }
    }

    override fun updateDescription(newValue: String) {
        updateLatestService { it.copy(description = newValue) }
    }

    override fun updateHomepage(newValue: String?) {
        updateLatestService { it.copy(homepage = newValue) }
    }

    override fun updateChangelog(newValue: String?) {
        updateLatestService { it.copy(changelog = newValue) }
    }

    override fun updateIsPageable(newValue: Boolean) {
        updateLatestService { it.copy(isPageable = newValue) }
    }

    override fun updateViewType(newValue: String?) {
        updateLatestService {
            val viewType = ServiceViewType.values().find { type -> type.value == newValue }
            it.copy(viewType = viewType)
        }
    }

    override fun updateMediaAspectRatio(newValue: String) {
        updateLatestService { it.copy(mediaAspectRatio = newValue) }
    }

    override fun updateIcon(newValue: String?) {
        updateLatestService { it.copy(icon = newValue) }
    }

    override fun updateHeaderImage(newValue: String?) {
        updateLatestService { it.copy(headerImage = newValue) }
    }

    override fun updateThemeColor(newValue: String?) {
        updateLatestService { it.copy(themeColor = newValue) }
    }

    override fun updateDarkThemeColor(newValue: String?) {
        updateLatestService { it.copy(darkThemeColor = newValue) }
    }

    override fun updateLanguages(newValue: Array<String>?) {
        updateLatestService { it.copy(languages = newValue?.toList()) }
    }

    override fun updateSupportedPostUrls(newValue: Array<String>?) {
        updateLatestService { it.copy(supportedPostUrls = newValue?.toList()) }
    }

    override fun updateSupportedUserUrls(newValue: Array<String>?) {
        updateLatestService { it.copy(supportedUserUrls = newValue?.toList()) }
    }

    private inline fun updateLatestService(
        crossinline update: (latestService: ServiceManifest) -> ServiceManifest
    ) {
        coroutineScope.launch {
            serviceRepository.updateWithLock(serviceId) {
                update(it)
            }
        }
    }
}