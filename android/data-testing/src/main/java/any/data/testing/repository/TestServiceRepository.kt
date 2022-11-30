package any.data.testing.repository

import any.data.entity.ServiceManifest
import any.data.json.Json
import any.data.repository.ServiceRepository
import any.data.testing.dao.TestServiceDao
import any.data.testing.source.service.TestBuiltinServiceDataSource

fun createTestServiceRepository(
    builtinService: List<ServiceManifest> = emptyList(),
    localServices: List<ServiceManifest> = emptyList(),
) = ServiceRepository(
    builtinServiceDataSource = TestBuiltinServiceDataSource(builtinService),
    serviceDao = TestServiceDao(localServices),
    json = Json,
)