package any.domain.entity

import androidx.compose.runtime.Immutable
import any.data.entity.Checksums
import any.data.entity.JsPageKey
import any.data.entity.PostsViewType
import any.data.entity.ServiceConfig
import any.data.entity.ServiceManifest
import any.data.entity.ServiceResource
import any.richtext.RichContent

@Immutable
data class UiServiceManifest(
    val raw: ServiceManifest,
    val description: RichContent?,
    val changelog: Changelog?,
    val themeColor: Int?,
    val darkThemeColor: Int?,
    val mediaAspectRatio: Float,
    val languages: List<Language>?,
) {
    val id: String get() = raw.id
    val originalId: String get() = raw.originalId
    val name: String get() = raw.name
    val developer: String get() = raw.developer
    val developerUrl: String? get() = raw.developerUrl
    val developerAvatar: String? get() = raw.developerAvatar
    val homepage: String? get() = raw.homepage
    val version: String get() = raw.version
    val minApiVersion: String get() = raw.minApiVersion
    val maxApiVersion: String? get() = raw.maxApiVersion
    val isPageable: Boolean get() = raw.isPageable
    val postsViewType: PostsViewType? get() = raw.postsViewType
    val icon: String? get() = raw.icon
    val headerImage: String? get() = raw.headerImage
    val main: String get() = raw.main
    val mainChecksums: Checksums get() = raw.mainChecksums
    val supportedPostUrls: List<String>? get() = raw.supportedPostUrls
    val supportedUserUrls: List<String>? get() = raw.supportedUserUrls
    val configs: List<ServiceConfig>? get() = raw.configs
    val forceConfigsValidation: Boolean? get() = raw.forceConfigsValidation
    val isEnabled: Boolean get() = raw.isEnabled
    val pageKeyOfPage2: JsPageKey? get() = raw.pageKeyOfPage2
    val upgradeUrl: String? get() = raw.upgradeUrl
    val areApiVersionsCompatible: Boolean get() = raw.areApiVersionsCompatible
    val buildTime: Long get() = raw.buildTime
    val addedAt: Long get() = raw.addedAt
    val updatedAt: Long get() = raw.updatedAt
    val source: ServiceManifest.Source get() = raw.source
    val localResources: List<ServiceResource>? get() = raw.localResources

    fun copy(
        id: String = this.id,
        originalId: String = this.originalId,
        name: String = this.name,
        developer: String = this.developer,
        developerUrl: String? = this.developerUrl,
        developerAvatar: String? = this.developerAvatar,
        homepage: String? = this.homepage,
        version: String = this.version,
        minApiVersion: String = this.minApiVersion,
        maxApiVersion: String? = this.maxApiVersion,
        isPageable: Boolean = this.isPageable,
        postsViewType: PostsViewType? = this.postsViewType,
        icon: String? = this.icon,
        headerImage: String? = this.headerImage,
        main: String = this.main,
        mainChecksum: Checksums = this.mainChecksums,
        supportedPostUrls: List<String>? = this.supportedPostUrls,
        supportedUserUrls: List<String>? = this.supportedUserUrls,
        configs: List<ServiceConfig>? = this.configs,
        forceConfigsValidation: Boolean? = this.forceConfigsValidation,
        isEnabled: Boolean = this.isEnabled,
        pageKeyOfPage2: JsPageKey? = this.pageKeyOfPage2,
        upgradeUrl: String? = this.upgradeUrl,
        buildTime: Long = this.buildTime,
        addedAt: Long = this.addedAt,
        updatedAt: Long = this.updatedAt,
        source: ServiceManifest.Source = this.source,
        localResources: List<ServiceResource>? = this.localResources,
    ): UiServiceManifest {
        val newRaw = raw.copy(
            id = id,
            originalId = originalId,
            name = name,
            developer = developer,
            developerUrl = developerUrl,
            developerAvatar = developerAvatar,
            homepage = homepage,
            version = version,
            minApiVersion = minApiVersion,
            maxApiVersion = maxApiVersion,
            isPageable = isPageable,
            postsViewType = postsViewType,
            icon = icon,
            headerImage = headerImage,
            main = main,
            mainChecksums = mainChecksum,
            supportedPostUrls = supportedPostUrls,
            supportedUserUrls = supportedUserUrls,
            configs = configs,
            forceConfigsValidation = forceConfigsValidation,
            isEnabled = isEnabled,
            pageKeyOfPage2 = pageKeyOfPage2,
            upgradeUrl = upgradeUrl,
            buildTime = buildTime,
            addedAt = addedAt,
            updatedAt = updatedAt,
            source = source,
            localResources = localResources,
        )
        return copy(raw = newRaw)
    }

    inline fun <T : String?> localFirstResourcePath(
        type: ServiceResource.Type,
        fallback: () -> T,
    ): T = raw.localFirstResourcePath(type, fallback)

    fun toStored(): UiServiceManifest {
        return copy(raw = raw.toStored())
    }

    sealed class Changelog {
        class Text(val text: String) : Changelog()

        class Url(val url: String) : Changelog()
    }

    data class Language(
        val languageTag: String,
        val displayName: String,
    )

    companion object
}