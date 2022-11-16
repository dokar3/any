package any.data.testing.repository

import any.data.Json
import any.data.entity.ServiceManifest
import any.data.repository.ServiceRepository
import any.data.testing.dao.TestServiceDao
import any.data.testing.loader.TestBuiltinServiceLoader

fun createTestServiceRepository(
    builtinService: List<ServiceManifest> = emptyList(),
    localServices: List<ServiceManifest> = emptyList(),
) = ServiceRepository(
    builtinServicesLoader = TestBuiltinServiceLoader(builtinService),
    serviceDao = TestServiceDao(localServices),
    json = Json,
)