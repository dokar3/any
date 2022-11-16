package any.data.source.service

import any.data.entity.ServiceManifest

interface BuiltinServicesLoader {
    suspend fun loadAll(): List<ServiceManifest>
}