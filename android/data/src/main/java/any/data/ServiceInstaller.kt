package any.data

import android.content.Context
import androidx.compose.runtime.Immutable
import any.base.file.AndroidFileReader
import any.base.file.FileReader
import any.base.log.Logger
import any.base.util.Dirs
import any.base.util.FileUtil
import any.base.util.ZipUtil
import any.base.util.isHttpUrl
import any.data.entity.ServiceManifest
import any.data.entity.ServiceResource
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Local js service installer.
 */
class ServiceInstaller(
    private val fileReader: FileReader,
    private val json: Json,
    private val servicesDir: File,
) {
    /**
     * Read all json manifests in the zip stream.
     */
    fun readManifests(file: File): List<Manifest> {
        val zipInputStream = ZipInputStream(file.inputStream())
        return try {
            val manifests = mutableListOf<Manifest>()
            ZipUtil.eachEntry(zipInputStream) { entry ->
                if (!entry.isDirectory && isManifestFile(entry.name)) {
                    val name = entry.name
                    val service = parseManifest(zipInputStream)
                    if (service != null) {
                        manifests.add(Manifest(name, service))
                    }
                }
            }
            manifests
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        } finally {
            zipInputStream.close()
        }
    }

    private fun isManifestFile(filename: String): Boolean {
        return filename.startsWith("manifest.") && filename.endsWith(".json")
    }

    private fun parseManifest(inputStream: InputStream): ServiceManifest? {
        return try {
            val bytes = inputStream.readBytes()
            val jsonText = String(bytes, Charsets.UTF_8)
            json.fromJson(jsonText, ServiceManifest::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun serviceDir(service: ServiceManifest): File {
        return File(servicesDir, FileUtil.buildValidFatFilename(service.id))
    }

    /**
     * Extract and install service files from a zip file.
     *
     * @param zip The zip file.
     * @param manifestName The manifest json file path in the zip.
     * @return Extracted service resources.
     */
    fun installFromZip(zip: File, manifestName: String): List<ServiceResource>? {
        try {
            var zipIn = ZipInputStream(zip.inputStream())
            if (!ZipUtil.seekTo(zipIn) { !it.isDirectory && it.name == manifestName }) {
                zipIn.close()
                return null
            }

            val service = parseManifest(zipIn)
            zipIn.close()
            if (service == null) {
                return null
            }

            val destDir = serviceDir(service)
            if (!destDir.exists() && !destDir.mkdirs()) {
                throw IOException("Cannot unpack service zip")
            }

            val updatedResources = mutableMapOf<ServiceResource.Type, String>()
            for (res in service.resources()) {
                val type = res.type
                val name = res.path.trimStart('.', '/')
                if (name.isEmpty() || name.isHttpUrl()) {
                    continue
                }
                // Extract service icon
                zipIn = ZipInputStream(zip.inputStream())
                if (ZipUtil.seekTo(zipIn) { !it.isDirectory && it.name == name }) {
                    val dst = File(destDir, name)
                    if (!dst.exists() && !dst.createNewFile()) {
                        Logger.e(TAG, "Cannot create service resource file: $dst")
                        return null
                    }
                    dst.outputStream().buffered().use { output ->
                        zipIn.copyTo(output)
                    }
                    updatedResources[type] = dst.absolutePath
                } else {
                    // Resource file not found
                    if (type == ServiceResource.Type.Main) {
                        Logger.e(TAG, "No main js file found in zip")
                        return null
                    }
                }
                zipIn.close()
            }

            return updatedResources.map { ServiceResource(it.key, it.value) }
        } catch (e: IOException) {
            Logger.e(TAG, e.message ?: "Cannot install service from zip: unknown error")
            return null
        }
    }

    /**
     * Install service files from a local manifest json file.
     *
     * @param manifestFile The manifest json file.
     * @return The service manifest to add to local.
     */
    fun installFromManifest(manifestFile: File): ServiceManifest? {
        val service = manifestFile.inputStream().use { parseManifest(it) } ?: return null

        val serviceDir = serviceDir(service)
        if (!serviceDir.exists() && !serviceDir.mkdirs()) {
            Logger.e(TAG, "Cannot create service dir: ${serviceDir.absolutePath}")
            return null
        }
        val updatedResources = mutableMapOf<ServiceResource.Type, String>()

        val manifestDir = manifestFile.parentFile

        val resources = service.resources()
            .toMutableList()
            .apply { addAll(service.localResources ?: emptyList()) }

        for (res in resources) {
            val type = res.type
            val path = res.path
            if (path.isEmpty() || path.isHttpUrl()) {
                continue
            }
            var src = File(path)
            if (!src.exists() && manifestDir != null) {
                src = File(manifestDir, path)
                if (!src.exists()) {
                    continue
                }
            }
            val dst = File(serviceDir, src.name)
            try {
                src.copyTo(dst, overwrite = true)
            } catch (e: IOException) {
                Logger.e(TAG, "Cannot copy resource $path to $dst")
            }
            updatedResources[type] = dst.absolutePath
        }

        val localResources = updatedResources.map { ServiceResource(it.key, it.value) }
        return service.copy(localResources = localResources)
    }

    /**
     * Install service from a assets manifest.
     *
     * @param manifest The manifest loaded from the assets.
     * @return Extracted service resources.
     */
    fun installFromAssets(manifest: ServiceManifest): List<ServiceResource>? {
        val serviceDir = serviceDir(manifest)
        if (!serviceDir.exists() && !serviceDir.mkdirs()) {
            Logger.e(TAG, "Cannot create service dir: ${serviceDir.absolutePath}")
            return null
        }

        val updatedResources = mutableMapOf<ServiceResource.Type, String>()

        for (res in manifest.resources()) {
            val type = res.type
            val path = res.path
            if (path.isEmpty() || path.isHttpUrl()) {
                continue
            }
            val dst = File(serviceDir, FileUtil.name(path))
            val inputStream = runCatching { fileReader.read(path) }.getOrNull() ?: continue
            inputStream.buffered().use { input ->
                dst.outputStream().buffered().use { output ->
                    try {
                        input.copyTo(output)
                    } catch (e: IOException) {
                        Logger.e(TAG, "Cannot copy builtin resource $path to $dst")
                        return null
                    }
                }
            }
            updatedResources[type] = dst.absolutePath
        }

        return updatedResources.map { ServiceResource(it.key, it.value) }
    }

    /**
     * Remove installed service files
     */
    fun removeServiceFiles(service: ServiceManifest) {
        val dir = serviceDir(service)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }

    @Immutable
    data class Manifest(
        val name: String,
        val service: ServiceManifest,
    )

    companion object {
        private const val TAG = "ServiceInstaller"

        @Volatile
        private var instance: ServiceInstaller? = null

        fun getDefault(context: Context): ServiceInstaller {
            return instance ?: synchronized(ServiceInstaller::class) {
                instance ?: ServiceInstaller(
                    fileReader = AndroidFileReader(context),
                    json = Json,
                    servicesDir = Dirs.servicesDir(context),
                ).also { instance = it }
            }
        }
    }
}