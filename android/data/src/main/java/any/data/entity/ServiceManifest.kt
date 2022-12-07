package any.data.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import any.data.js.ServiceApiVersion
import com.github.snksoft.crc.CRC
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.vdurmont.semver4j.Semver
import okhttp3.internal.toHexString

@Immutable
@JsonClass(generateAdapter = true)
@Entity(tableName = "Service")
data class ServiceManifest(
    /**
     * If the service was read from db, the [id] is a stored id, if the service was read from json,
     * it's the source id.
     * [id] and [originalId] always are the same if there is no variables in the original id.
     */
    @PrimaryKey
    val id: String,
    /**
     * The original id.
     */
    val originalId: String = id,
    val name: String,
    val description: String,
    val developer: String,
    val developerUrl: String?,
    val developerAvatar: String?,
    val homepage: String?,
    val changelog: String?,
    val version: String,
    val minApiVersion: String,
    val maxApiVersion: String?,
    val isPageable: Boolean,
    val postsViewType: PostsViewType?,
    val mediaAspectRatio: String,
    val icon: String?,
    val headerImage: String?,
    val themeColor: String?,
    val darkThemeColor: String?,
    val main: String,
    val mainChecksums: Checksums,
    val languages: List<String>?,
    val supportedPostUrls: List<String>?,
    val supportedUserUrls: List<String>?,
    val configs: List<ServiceConfig>?,
    val forceConfigsValidation: Boolean?,
    val isEnabled: Boolean = true,
    val pageKeyOfPage2: JsPageKey? = null,
    val upgradeUrl: String? = null,
    val buildTime: Long = -1,
    val addedAt: Long = -1,
    val updatedAt: Long = -1,
    val source: Source = Source.Unspecified,
    val localResources: List<ServiceResource>? = null,
) {
    @Ignore
    @Json(ignore = true)
    val localResourceMap = localResources?.associateBy { it.type }

    @Ignore
    @Json(ignore = true)
    val areApiVersionsCompatible: Boolean = checkApiVersionsCompatibility(
        minApiVersion,
        maxApiVersion
    )

    inline fun <T : String?> localFirstResourcePath(
        type: ServiceResource.Type,
        fallback: () -> T,
    ): T {
        @Suppress("UNCHECKED_CAST")
        return (localResourceMap?.get(type)?.path ?: fallback()) as T
    }

    fun markAsBuiltin(): ServiceManifest {
        return copy(source = Source.Builtin)
    }

    fun markAsRemote(): ServiceManifest {
        return copy(source = Source.Remote)
    }

    fun markAsLocal(): ServiceManifest {
        return copy(source = Source.Local)
    }

    fun resources(): List<ServiceResource> {
        return ServiceResource.Type.values()
            .mapNotNull { type ->
                val path = when (type) {
                    ServiceResource.Type.Main -> main
                    ServiceResource.Type.Icon -> icon
                    ServiceResource.Type.HeaderImage -> headerImage
                    ServiceResource.Type.Changelog -> changelog
                }
                path?.let { ServiceResource(type, it) }
            }
    }

    /**
     * Convert the service [id] to a stored id: All dynamic fields in the id will be replaced with
     * actual values.
     */
    fun toStored(): ServiceManifest = copy(id = generatedStoredId())

    private fun generatedStoredId(): String {
        val list = mutableListOf<Pair<String, String?>>("serviceName" to name)

        this.configs?.forEach { list.add(it.key to it.value?.text) }

        val mayHaveHashFn = id.indexOf("hash(") != -1

        return list.fold(id) { currId, config ->
            val varName = "{${config.first}}"
            val varValue = config.second
                ?.lowercase()
                ?.replace(' ', '_')

            if (mayHaveHashFn) {
                val hashVarName = "{hash(${config.first})}"
                val crc32 = CRC.calculateCRC(
                    CRC.Parameters.CRC32C,
                    config.second?.toByteArray() ?: byteArrayOf()
                )
                val hashVarValue = crc32.toHexString()
                currId.replace(varName, varValue ?: "null")
                    .replace(hashVarName, hashVarValue)
            } else {
                currId.replace(varName, varValue ?: "null")
            }
        }
    }

    @JsonClass(generateAdapter = false)
    enum class Source {
        Unspecified,
        Builtin,
        Remote,
        Local,
    }

    companion object {
        fun checkApiVersionsCompatibility(
            minApiVersion: String,
            maxApiVersion: String?,
        ): Boolean {
            try {
                val apiVersion = Semver(ServiceApiVersion.get())
                val minSemanticVer = Semver(minApiVersion)
                // Check min api version
                if (apiVersion < minSemanticVer) {
                    return false
                }
                if (!maxApiVersion.isNullOrEmpty()) {
                    val maxSemanticVer = Semver(maxApiVersion)
                    // Check max api version
                    if (apiVersion > maxSemanticVer) {
                        return false
                    }
                }
            } catch (e: Exception) {
                // Failed to check versions
                e.printStackTrace()
            }
            return true
        }
    }
}