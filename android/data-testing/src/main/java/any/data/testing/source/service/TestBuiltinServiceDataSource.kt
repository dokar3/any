package any.data.testing.source.service

import any.data.entity.ServiceManifest
import any.data.source.service.BuiltinServiceDataSource

class TestBuiltinServiceDataSource(
    private var services: List<ServiceManifest>
) : BuiltinServiceDataSource {
    override suspend fun getAll(): List<ServiceManifest> =
        services.map(ServiceManifest::markAsBuiltin)
}