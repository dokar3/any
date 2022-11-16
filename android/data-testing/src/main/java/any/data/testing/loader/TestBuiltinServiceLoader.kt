package any.data.testing.loader

import any.data.entity.ServiceManifest
import any.data.source.service.BuiltinServicesLoader

class TestBuiltinServiceLoader(
    private var services: List<ServiceManifest>
) : BuiltinServicesLoader {
    override suspend fun loadAll(): List<ServiceManifest> =
        services.map(ServiceManifest::markAsBuiltin)
}