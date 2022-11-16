package any.data

import any.data.entity.ServiceManifest

object ServiceLookup {
    fun find(
        services: List<ServiceManifest>,
        targetServiceId: String?,
        postUrl: String,
    ): ServiceManifest? {
        return find(
            targetServiceId = targetServiceId,
            postUrl = postUrl,
            services = services.associateBy { it.id },
        )
    }

    fun find(
        services: Map<String, ServiceManifest>,
        targetServiceId: String?,
        postUrl: String,
    ): ServiceManifest? {
        if (services.isEmpty()) {
            return null
        }
        // Find by service id
        val target = services[targetServiceId]
        if (target != null) {
            return target
        }
        // Match supported post urls
        for (service in services.values) {
            val supportedPostUrls = service.supportedPostUrls
            if (!supportedPostUrls.isNullOrEmpty()) {
                for (supportedUrl in supportedPostUrls) {
                    val regex = Regex(supportedUrl.replace("*", "(.*)?"))
                    if (regex.matches(postUrl)) {
                        return service
                    }
                }
            }
        }
        return null
    }

    fun find(
        services: List<ServiceManifest>,
        userUrl: String,
    ): ServiceManifest? {
        for (service in services) {
            val supportedUserUrls = service.supportedUserUrls
            if (!supportedUserUrls.isNullOrEmpty()) {
                for (supportedUrl in supportedUserUrls) {
                    val regex = Regex(supportedUrl.replace("*", "(.*)?"))
                    if (regex.matches(userUrl)) {
                        return service
                    }
                }
            }
        }
        return null
    }
}