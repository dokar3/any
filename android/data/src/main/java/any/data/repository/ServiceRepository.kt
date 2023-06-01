package any.data.repository

import android.content.Context
import any.base.util.Http
import any.data.db.AppDatabase
import any.data.db.ServiceDao
import any.data.entity.ServiceManifest
import any.data.json.Json
import any.data.json.fromJson
import any.data.source.service.AssetsServiceDataSource
import any.data.source.service.BuiltinServiceDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ServiceRepository(
    private val builtinServiceDataSource: BuiltinServiceDataSource,
    private val serviceDao: ServiceDao,
    private val json: Json,
) : ReactiveRepository<String, ServiceManifest>() {
    private val updateMutex = Mutex()

    suspend fun getBuiltinServices(): List<ServiceManifest> {
        return runCatching { builtinServiceDataSource.getAll() }.getOrElse { emptyList() }
    }

    suspend fun getDbServices(): List<ServiceManifest> {
        return serviceDao.getAll()
    }

    suspend fun fetchServiceFromManifestUrl(
        url: String,
    ): ServiceManifest? = withContext(Dispatchers.IO) {
        val manifestJson = Http.get(url) ?: return@withContext null
        try {
            json.fromJson<ServiceManifest>(manifestJson)
                ?.let(ServiceManifest::markAsRemote)
                ?.copy(upgradeUrl = url)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun findDbService(id: String): ServiceManifest? {
        return serviceDao.get(id)
    }

    suspend fun upsertDbService(service: ServiceManifest) {
        if (serviceDao.get(service.id) != null) {
            serviceDao.update(service)
            notifyUpdated(service)
        } else {
            serviceDao.add(service.copy(addedAt = System.currentTimeMillis()))
            notifyInserted(service)
        }
    }

    suspend fun updateDbService(service: ServiceManifest) {
        serviceDao.update(service)
        notifyUpdated(service)
    }

    suspend fun updateDbService(services: List<ServiceManifest>) {
        serviceDao.update(services)
        notifyUpdated(services)
    }

    suspend fun updateWithLock(serviceId: String, update: (ServiceManifest) -> ServiceManifest) {
        updateMutex.withLock {
            serviceDao.get(serviceId)?.let { latest ->
                val updated = update(latest)
                if (updated != latest) {
                    serviceDao.update(updated)
                    notifyUpdated(updated)
                }
            }
        }
    }

    suspend fun removeDbService(service: ServiceManifest) {
        serviceDao.remove(service)
        notifyDeletedByItem(service)
    }

    suspend fun removeDbServices(services: List<ServiceManifest>) {
        serviceDao.remove(services)
        notifyDeletedByItem(services)
    }

    suspend fun clearDbServices() {
        val allIds = serviceDao.getAllIds()
        serviceDao.clear()
        notifyDeletedByKey(allIds)
    }

    companion object {
        @Volatile
        private var instance: ServiceRepository? = null

        fun getDefault(context: Context): ServiceRepository {
            return instance ?: synchronized(ServiceRepository::class) {
                instance ?: ServiceRepository(
                    builtinServiceDataSource = AssetsServiceDataSource(context),
                    serviceDao = AppDatabase.get(context).serviceDao(),
                    json = Json,
                ).also {
                    instance = it
                }
            }
        }
    }
}