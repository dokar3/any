package any.data.source.service

import android.content.Context
import any.data.entity.ServiceManifest
import any.data.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssetsServiceDataSource(
    context: Context,
    private val json: Json = Json,
) : BuiltinServiceDataSource {
    private val context = context.applicationContext

    override suspend fun getAll(): List<ServiceManifest> = withContext(Dispatchers.IO) {
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