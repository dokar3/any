package any.data.testing.dao

import any.data.db.ServiceDao
import any.data.entity.ServiceManifest

class TestServiceDao(services: List<ServiceManifest>) : ServiceDao {
    private val allServices = services.toMutableList()

    override suspend fun get(id: String): ServiceManifest? {
        return allServices.find { it.id == id }
    }

    override suspend fun count(): Int {
        return allServices.size
    }

    override suspend fun getAll(): List<ServiceManifest> {
        return allServices.toList()
    }

    override suspend fun getAllIds(): List<String> {
        return allServices.map { it.id }
    }

    override suspend fun add(service: ServiceManifest) {
        allServices.add(service)
    }

    override suspend fun add(services: List<ServiceManifest>) {
        allServices.addAll(services)
    }

    override suspend fun update(service: ServiceManifest) {
        val idx = allServices.indexOfFirst { it.id == service.id }
        if (idx != -1) {
            allServices[idx] = service
        }
    }

    override suspend fun update(services: List<ServiceManifest>) {
        for (service in services) {
            update(service)
        }
    }

    override suspend fun remove(service: ServiceManifest) {
        allServices.remove(service)
    }

    override suspend fun remove(services: List<ServiceManifest>) {
        allServices.removeAll(services)
    }

    override suspend fun clear() {
        allServices.clear()
    }

}