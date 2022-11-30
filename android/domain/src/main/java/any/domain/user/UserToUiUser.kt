package any.domain.user

import any.base.util.ColorUtil
import any.data.entity.ServiceManifest
import any.data.entity.User
import any.domain.entity.UiUser
import any.richtext.html.HtmlParser

fun User.toUiUser(
    service: ServiceManifest?,
    htmlParser: HtmlParser,
): UiUser {
    return UiUser.fromUser(
        user = this,
        service = service,
        htmlParser = htmlParser,
    )
}

fun UiUser.Companion.fromUser(
    user: User,
    service: ServiceManifest?,
    htmlParser: HtmlParser,
): UiUser {
    return UiUser(
        raw = user,
        serviceName = service?.name,
        serviceIcon = service?.icon,
        serviceThemeColor = ColorUtil.parseOrDefault(service?.themeColor),
        serviceDarkThemeColor = ColorUtil.parseOrDefault(service?.darkThemeColor),
        description = user.description?.let(htmlParser::parse),
    )
}
