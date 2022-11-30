package any.data.source.service

import any.data.entity.ServiceManifest

interface BuiltinServiceDataSource {
    suspend fun getAll(): List<ServiceManifest>
}