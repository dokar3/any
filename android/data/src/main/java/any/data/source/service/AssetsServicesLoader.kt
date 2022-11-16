package any.data.source.service

import android.content.Context
import any.data.Json
import any.data.entity.ServiceManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssetsServicesLoader(
    context: Context,
    private val json: Json = Json,
) : BuiltinServicesLoader {
    private val context = context.applicationContext

    override suspend fun loadAll(): List<ServiceManifest> = withContext(Dispatchers.IO) {
        val text = context.assets.open(BUILTIN_SERVICES)
            .bufferedReader()
            .use { it.readText() }
        return@withContext json.fromJson(text, Array<ServiceManifest>::class.java)
            ?.map(ServiceManifest::markAsBuiltin)
            ?: emptyList()
    }

    companion object {
        private const val BUILTIN_SERVICES = "js/services.json"
    }
}