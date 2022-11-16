package any.domain.service

import any.base.file.FileReader
import any.base.util.ColorUtil
import any.base.util.FileUtil
import any.base.util.isHttpUrl
import any.data.ThumbAspectRatio
import any.data.entity.ServiceManifest
import any.data.entity.ServiceResource
import any.domain.entity.UiServiceManifest
import any.richtext.html.HtmlParser
import java.io.File
import java.util.Locale

fun ServiceManifest.toUiManifest(
    fileReader: FileReader,
    htmlParser: HtmlParser
): UiServiceManifest {
    return UiServiceManifest.fromManifest(this, fileReader, htmlParser)
}

fun UiServiceManifest.Companion.fromManifest(
    service: ServiceManifest,
    fileReader: FileReader,
    htmlParser: HtmlParser,
): UiServiceManifest {
    val cl = service.localFirstResourcePath(
        type = ServiceResource.Type.Changelog,
        fallback = { service.changelog }
    )
    val changelog = when {
        cl.isNullOrEmpty() -> null

        cl.isHttpUrl() -> UiServiceManifest.Changelog.Url(cl)

        FileUtil.isAssetsFile(cl) || File(cl).exists() -> {
            val text = runCatching { fileReader.read(cl) }
                .getOrNull()
                ?.bufferedReader()
                ?.use { it.readText() } ?: ""
            UiServiceManifest.Changelog.Text(text)
        }

        else -> UiServiceManifest.Changelog.Text(cl)
    }
    val description = service.description.let(htmlParser::parse)
    val themeColor = ColorUtil.parseOrNull(service.themeColor)
    val darkThemeColor = ColorUtil.parseOrNull(service.darkThemeColor)
    val mediaAspectRatio = ThumbAspectRatio.parse(service.mediaAspectRatio)
    val languages = service.languages?.map {
        UiServiceManifest.Language(
            languageTag = it,
            displayName = Locale.forLanguageTag(it).displayName,
        )
    }
    return UiServiceManifest(
        raw = service,
        changelog = changelog,
        description = description,
        themeColor = themeColor,
        darkThemeColor = darkThemeColor,
        mediaAspectRatio = mediaAspectRatio,
        languages = languages,
    )
}
